package io.github.seacucu.guidebookfixes.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies each Mixin only when the mod it patches is actually installed.
 *
 * <p>Every Mixin lives in a sub-package named after the mod it targets, so the
 * guard is a package-name lookup rather than a list that can drift out of date.
 * If the owning mod is absent the Mixin is skipped silently — this mod must
 * never turn a missing optional dependency into a crash or an error log.
 */
public class MixinPlugin implements IMixinConfigPlugin {
    private static final String PACKAGE = "io.github.seacucu.guidebookfixes.mixin.";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(PACKAGE)) {
            return true;
        }
        String rest = mixinClassName.substring(PACKAGE.length());
        int dot = rest.indexOf('.');
        if (dot < 0) {
            return true;            // not in a per-mod sub-package
        }
        return isLoaded(rest.substring(0, dot));
    }

    /**
     * Mixins are applied before the mod list is available through the normal
     * API, so we ask the loading mod list directly. Anything unexpected means
     * we cannot prove the mod is absent, and skipping a fix is cheaper than
     * failing to load, so we let the Mixin through and let its own target
     * lookup decide.
     */
    private static boolean isLoaded(String modid) {
        try {
            return net.minecraftforge.fml.loading.LoadingModList.get()
                    .getModFileById(modid) != null;
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
