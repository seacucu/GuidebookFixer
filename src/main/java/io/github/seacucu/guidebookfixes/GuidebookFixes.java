package io.github.seacucu.guidebookfixes;

import net.minecraftforge.fml.common.Mod;

/**
 * Client-side fixes for in-game guidebooks that break inside modpacks.
 *
 * <p>The mod itself does nothing at runtime; every fix is a Mixin that only
 * applies when the book's owning mod is actually present. See {@link
 * io.github.seacucu.guidebookfixes.mixin.MixinPlugin}.
 */
@Mod(GuidebookFixes.MODID)
public final class GuidebookFixes {
    public static final String MODID = "guidebookfixes";

    public GuidebookFixes() {
        // Everything happens in Mixins; no registration needed.
    }
}
