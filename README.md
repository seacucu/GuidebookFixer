# Guidebook Fixer

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
already handles CJK. That is why vanilla books and tooltips are fine.

**Recipe pages pointing at recipes the pack changed.** A page that asks for
recipe id `examplemod:widget` fails when the pack removed that recipe and added
its own replacement under a different id. Depending on the book that shows up as
an untranslated error string over the page, or as a silently blank recipe slot.
This mod falls back to whatever recipe currently produces the item, so the page
shows the recipe the player can actually craft.

Books that only carry a recipe id, with no item beside it, would otherwise be
unfixable. Mods overwhelmingly name a recipe after what it makes
(`eidolon:worktable`, `botania:diluted_pool`), so when the id also names an item
the intent is recoverable. Ids that are not item ids
(`ae2:transform/fluix_crystals`) get no fallback, which is the honest answer
rather than a guess.

**Recipes of a kind the book has no renderer for.** GuideME draws crafting,
smelting, smithing and cooking. An item a modpack moved to a Create crusher or
a Thermal pulverizer is perfectly craftable and still reported missing, because
nothing registered a way to draw that kind of recipe. A recipe knows its own
ingredients and result, and GuideME's own recipe box takes exactly those, so
this mod draws it rather than explaining why it cannot. Where a mod contributes
a proper renderer that one still wins; this only runs where the alternative was
nothing at all.

When the pack really did remove the recipe and left no replacement, the page
says so in the player's language instead of printing an id.

Nothing is added to or removed from the recipe registry. The mod only changes
what the book looks up and what it draws.

## Supported books

| Mod | Line breaking | Recipe fallback | Draws unsupported recipe kinds | Message in the player's language |
|---|---|---|---|---|
| Eidolon: Repraised | yes | by result item | n/a | yes |
| Patchouli | n/a | by recipe id | n/a | n/a, it renders blank with nothing to translate |
| GuideME (the AE2 guide) | n/a | by recipe id | yes | yes |

Only GuideME gets the generic recipe drawing: it has a recipe box that takes
any ingredients and result. Patchouli and Eidolon pages are written as one
specific kind of recipe each and would break if handed another, so there the
fallback stays within the kind the page was written for.

Line breaking is only listed for books that roll their own; Patchouli and
GuideME already use Minecraft's line breaker and are fine as they are.

Each fix is a Mixin in a package named after the mod it patches, and is skipped
entirely when that mod is not installed. Adding a book means adding a package.

## Building

No Gradle and no downloads. The build compiles against the SRG-named Minecraft
jar the launcher already has on disk, which is the exact shape Forge runs in
production, so there is no refmap and no remapping step, and what is written
is what runs. The trade-off is that vanilla members appear under SRG names
(`m_92895_` rather than `Font.width`); every such call names the real member in
a comment.

```
python build.py
```

Inputs are located from a PrismLauncher install by default. Override with
`GBF_LIBRARIES` (launcher `libraries/`), `GBF_MODS` (a folder holding the mod
jars to compile against) and `GBF_JDK` (a JDK 17).

Output: `build/libs/guidebookfixer-1.20.1-<version>.jar`

## License

[MIT](LICENSE). Contains no code from any of the mods it patches.
