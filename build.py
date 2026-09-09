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
import os
import shutil
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
VERSION = "0.1.0"
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
]
# Mods whose classes our Mixins reference. Optional: a missing one only means
# the Mixins for that mod cannot be compiled, which we report rather than hide.
MOD_JARS = {"eidolon": "eidolon_repraised-*.jar"}


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


def main():
    javac = os.path.join(JDK, "bin", "javac.exe" if os.name == "nt" else "javac")
    jar = os.path.join(JDK, "bin", "jar.exe" if os.name == "nt" else "jar")
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

    manifest = os.path.join(OUT, "MANIFEST.MF")
    with open(manifest, "w", encoding="utf-8", newline="\r\n") as fh:
        fh.write(
            "Manifest-Version: 1.0\n"
            "Specification-Title: guidebookfixes\n"
            "Specification-Vendor: seacucu\n"
            "Specification-Version: 1\n"
            "Implementation-Title: Guidebook Fixes\n"
            f"Implementation-Version: {VERSION}\n"
            "Implementation-Vendor: seacucu\n"
            "MixinConfigs: guidebookfixes.mixins.json\n"
        )

    out = os.path.join(LIBS, f"guidebookfixes-{MC}-{VERSION}.jar")
    r = subprocess.run([jar, "--create", "--file", out, "--manifest", manifest,
                        "-C", CLASSES, "."],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode:
        print(r.stdout + r.stderr, file=sys.stderr)
        die("打包失敗")

    # ${file.jarVersion} is substituted by Forge from the manifest at load time.
    with zipfile.ZipFile(out) as z:
        names = z.namelist()
    print(f"完成：{os.path.relpath(out, ROOT)}（{len(names)} 個項目，"
          f"{os.path.getsize(out)} bytes）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
