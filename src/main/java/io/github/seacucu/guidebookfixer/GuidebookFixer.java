package io.github.seacucu.guidebookfixer;

import net.minecraftforge.fml.common.Mod;

/**
 * Client-side fixes for in-game guidebooks that break inside modpacks.
 *
 * <p>The mod itself does nothing at runtime; every fix is a Mixin that only
 * applies when the book's owning mod is actually present. See {@link
 * io.github.seacucu.guidebookfixer.mixin.MixinPlugin}.
 */
@Mod(GuidebookFixer.MODID)
public final class GuidebookFixer {
    public static final String MODID = "guidebookfixer";

    public GuidebookFixer() {
        // Everything happens in Mixins; no registration needed.
    }
}
