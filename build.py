#!/usr/bin/env python3
"""Build guidebookfixes-<version>.jar with plain javac — no Gradle, no downloads.

Why not ForgeGradle: this mod is a handful of Mixins that patch other mods.
Compiling against the SRG-named Minecraft jar that the launcher already has on
disk gives us exactly the shape Forge runs in production, which means no refmap
and no remapping step — what we write is what runs. The cost is that vanilla
members appear under their SRG names (`m_92895_` rather than `Font.width`);
every such call carries a comment naming the real member.

Inputs are located automatically from a PrismLauncher install and the LIA
instance; override with GBF_LIBRARIES / GBF_MODS if yours live elsewhere.

    python build.py

Output: build/libs/guidebookfixes-<version>.jar
"""

import glob
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
VERSION = "0.2.0"
MC = "1.20.1"

APPDATA = os.environ.get("APPDATA", "")
PRISM = os.path.join(APPDATA, "PrismLauncher")
LIBRARIES = os.environ.get("GBF_LIBRARIES") or os.path.join(PRISM, "libraries")
MODS = os.environ.get("GBF_MODS") or os.path.join(
    PRISM, "instances", "Liminal Industries Acension", "minecraft", "mods")
JDK = os.environ.get("GBF_JDK") or os.path.join(PRISM, "java", "java-runtime-gamma")

SRC = os.path.join(ROOT, "src", "main", "java")
RES = os.path.join(ROOT, "src", "main", "resources")
OUT = os.path.join(ROOT, "build")
CLASSES = os.path.join(OUT, "classes")
LIBS = os.path.join(OUT, "libs")

# Compile-time only. Mixin targets are resolved at runtime, so these jars are
# never redistributed and nothing from them ends up in our jar.
NEEDED = [
    "net/minecraft/client/{mc}-*/client-*-srg.jar",
    "net/minecraftforge/forge/{mc}-*/forge-*-universal.jar",
    "net/minecraftforge/fmlcore/{mc}-*/fmlcore-*.jar",
    "net/minecraftforge/fmlloader/{mc}-*/fmlloader-*.jar",
    "net/minecraftforge/javafmllanguage/{mc}-*/javafmllanguage-*.jar",
    "org/spongepowered/mixin/*/mixin-*.jar",
    "org/ow2/asm/asm-tree/*/asm-tree-*.jar",
    "org/ow2/asm/asm/*/asm-*.jar",
    # Registry implements com.mojang.serialization.Keyable, so javac needs
    # DataFixerUpper on the classpath to resolve the supertype. 6.x is the line
    # Minecraft 1.20.1 ships; the launcher keeps several versions side by side.
    "com/mojang/datafixerupper/6.*/datafixerupper-*.jar",
]
# Mods whose classes our Mixins reference. Optional: a missing one only means
# the Mixins for that mod cannot be compiled, which we report rather than hide.
MOD_JARS = {"eidolon": "eidolon_repraised-*.jar",
            "guideme": "guideme-*.jar",
            "patchouli": "Patchouli-*.jar"}


def die(msg):
    print(f"錯誤：{msg}", file=sys.stderr)
    sys.exit(1)


def find(pattern, root):
    hits = sorted(glob.glob(os.path.join(root, pattern.format(mc=MC)).replace("\\", "/")))
    return hits[-1] if hits else None


def classpath():
    cp, missing = [], []
    for pat in NEEDED:
        hit = find(pat, LIBRARIES)
        (cp if hit else missing).append(hit or pat)
    if missing:
        die("找不到編譯用的 jar（設 GBF_LIBRARIES 指向 launcher 的 libraries 目錄）：\n  "
            + "\n  ".join(missing))
    for modid, pat in MOD_JARS.items():
        hit = find(pat, MODS)
        if not hit:
            die(f"找不到 {modid} 的 jar（設 GBF_MODS 指向含模組 jar 的目錄）：{pat}")
        cp.append(hit)
    return cp


# Forge rejects anything else outright: mod construction throws
# "Invalid displayTest value supplied in mods.toml", which takes the whole
# client down at load with a stack trace pointing at whichever mod happens to
# report the failure. Not something to discover by launching the game.
DISPLAY_TEST = {"MATCH_VERSION", "IGNORE_SERVER_VERSION", "IGNORE_ALL_VERSION", "NONE"}


def check_metadata():
    """Everything the game would only tell us about by crashing."""
    problems = []

    toml = open(os.path.join(RES, "META-INF", "mods.toml"), encoding="utf-8").read()
    m = re.search(r'^\s*displayTest\s*=\s*"([^"]*)"', toml, re.M)
    if m and m.group(1) not in DISPLAY_TEST:
        problems.append(f'mods.toml 的 displayTest="{m.group(1)}" 不是合法值，'
                        f'只能是 {"／".join(sorted(DISPLAY_TEST))}')

    cfg_name = "guidebookfixes.mixins.json"
    cfg = json.load(open(os.path.join(RES, cfg_name), encoding="utf-8"))
    pkg = cfg["package"]
    for section in ("mixins", "client", "server"):
        for name in cfg.get(section, []):
            path = os.path.join(CLASSES, *(pkg + "." + name).split(".")) + ".class"
            if not os.path.exists(path):
                problems.append(f"{cfg_name} 列了 {section}.{name}，但編譯結果裡沒有這個類別")

    plugin = cfg.get("plugin")
    if plugin:
        path = os.path.join(CLASSES, *plugin.split(".")) + ".class"
        if not os.path.exists(path):
            problems.append(f"{cfg_name} 的 plugin {plugin} 不存在")

    return problems


# The jar tool stamps every entry with the current time, so two builds of the
# same source produce different bytes. This mod ships inside the LIA-zhTW patch,
# which promises a byte-identical zip for a given source tree, so we write the
# archive ourselves with a fixed timestamp and a sorted entry order.
JAR_TIME = (2026, 1, 1, 0, 0, 0)


def write_jar(out_path, manifest):
    def entry(name):
        zi = zipfile.ZipInfo(name, date_time=JAR_TIME)
        zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = 0o644 << 16
        return zi

    files, dirs = [], set()
    for dp, _, fs in os.walk(CLASSES):
        for f in fs:
            full = os.path.join(dp, f)
            name = os.path.relpath(full, CLASSES).replace(os.sep, "/")
            files.append((name, full))
            parts = name.split("/")[:-1]
            for i in range(1, len(parts) + 1):
                dirs.add("/".join(parts[:i]) + "/")
    files.sort()

    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr(entry("META-INF/MANIFEST.MF"), manifest)
        # Directory entries are optional for the JVM, but the jar tool writes
        # them and some pack/resource scanners walk them, so keep the archive
        # shaped the way a normal jar is.
        for d in sorted(dirs):
            zi = zipfile.ZipInfo(d, date_time=JAR_TIME)
            zi.external_attr = (0o755 << 16) | 0x10
            z.writestr(zi, b"")
        for name, full in files:
            if name == "META-INF/MANIFEST.MF":
                continue
            with open(full, "rb") as fh:
                z.writestr(entry(name), fh.read())


def main():
    javac = os.path.join(JDK, "bin", "javac.exe" if os.name == "nt" else "javac")
    if not os.path.exists(javac):
        die(f"找不到 javac：{javac}（設 GBF_JDK）")

    cp = classpath()
    sources = [os.path.join(dp, f) for dp, _, fs in os.walk(SRC)
               for f in fs if f.endswith(".java")]
    if not sources:
        die("沒有原始碼")

    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(CLASSES)
    os.makedirs(LIBS)

    print(f"編譯 {len(sources)} 個檔案…")
    # -proc:none: the Mixin jar ships an annotation processor that generates
    # refmaps. We compile against SRG names on purpose, so there is nothing to
    # remap, and running it only drags in dependencies we do not have.
    r = subprocess.run([javac, "--release", "17", "-encoding", "UTF-8",
                        "-nowarn", "-proc:none", "-classpath", os.pathsep.join(cp),
                        "-d", CLASSES] + sources,
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode:
        print(r.stdout + r.stderr, file=sys.stderr)
        die("編譯失敗")
    if r.stderr.strip():
        print(r.stderr.strip())

    shutil.copytree(RES, CLASSES, dirs_exist_ok=True)

    problems = check_metadata()
    if problems:
        die("中繼資料檢查未過：\n  " + "\n  ".join(problems))

    # ${file.jarVersion} is substituted by Forge from the manifest at load time.
    manifest = (
        "Manifest-Version: 1.0\r\n"
        "Specification-Title: guidebookfixes\r\n"
        "Specification-Vendor: seacucu\r\n"
        "Specification-Version: 1\r\n"
        "Implementation-Title: Guidebook Fixes\r\n"
        f"Implementation-Version: {VERSION}\r\n"
        "Implementation-Vendor: seacucu\r\n"
        "MixinConfigs: guidebookfixes.mixins.json\r\n"
        "\r\n"
    )

    out = os.path.join(LIBS, f"guidebookfixes-{MC}-{VERSION}.jar")
    write_jar(out, manifest)

    with zipfile.ZipFile(out) as z:
        names = z.namelist()
    print(f"完成：{os.path.relpath(out, ROOT)}（{len(names)} 個項目，"
          f"{os.path.getsize(out)} bytes）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
