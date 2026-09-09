# Guidebook Fixes

[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

Client-side Minecraft mod that repairs in-game guidebooks which break once a
modpack gets involved. Minecraft 1.20.1, Forge.

Mod guidebooks tend to hardcode two assumptions that stop being true inside a
modpack: that words are separated by spaces, and that the recipe ids they were
written against still exist. Neither is fixable from a resource pack, because
both live in the book's own rendering code.

## What it fixes

**CJK line breaking.** Books that wrap text with `text.split(" ")` treat a
whole Chinese, Japanese or Korean paragraph as one unbreakable word. The
paragraph is drawn as a single line several times wider than the page and runs
off the edge. This mod hands the layout to Minecraft's own line breaker, which
already handles CJK — that is why vanilla books and tooltips are fine.

**Recipe pages pointing at recipes the pack changed.** A page that asks for
recipe id `examplemod:widget` fails when the pack removed that recipe and added
its own replacement under a different id, and the book prints an untranslated
error string over the page. This mod falls back to whatever recipe currently
produces the item, so the page shows the recipe the player can actually craft.
When the pack removed the recipe outright and there is no replacement, the page
says so in the player's language instead of printing an id.

Nothing is added to or removed from the recipe registry. The mod only changes
what the book looks up and what it draws.

## Supported books

| Mod | Line breaking | Recipe fallback |
|---|---|---|
| Eidolon: Repraised | ✅ | ✅ |

Each fix is a Mixin in a package named after the mod it patches, and is skipped
entirely when that mod is not installed. Adding a book means adding a package.

## Building

No Gradle and no downloads. The build compiles against the SRG-named Minecraft
jar the launcher already has on disk, which is the exact shape Forge runs in
production — so there is no refmap and no remapping step, and what is written
is what runs. The trade-off is that vanilla members appear under SRG names
(`m_92895_` rather than `Font.width`); every such call names the real member in
a comment.

```
python build.py
```

Inputs are located from a PrismLauncher install by default. Override with
`GBF_LIBRARIES` (launcher `libraries/`), `GBF_MODS` (a folder holding the mod
jars to compile against) and `GBF_JDK` (a JDK 17).

Output: `build/libs/guidebookfixes-1.20.1-<version>.jar`

## License

[MIT](LICENSE). Contains no code from any of the mods it patches.
