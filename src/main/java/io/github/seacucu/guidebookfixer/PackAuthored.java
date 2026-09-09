package io.github.seacucu.guidebookfixer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Did the modpack write this recipe, or did the mod ship it?
 *
 * <p>The question decides whether a guidebook page is merely being drawn a
 * different way or is actually describing something that no longer happens, so
 * it is worth answering properly rather than guessing. A recipe id names the
 * file it came from, so the answer is simply whether that file exists inside
 * the jar of the mod the id belongs to. Anything else, a datapack in the world
 * folder or a recipe a script produced, is the pack's own work.
 *
 * <p>This does not depend on how the pack was built. KubeJS ids happen to be
 * recognisable, but a hand-written datapack is not, and both are the pack's.
 */
public final class PackAuthored {
    private PackAuthored() {
    }

    private static final Map<ResourceLocation, Boolean> CACHE = new ConcurrentHashMap<>();

    public static boolean test(ResourceLocation recipeId) {
        if (recipeId == null) {
            return false;
        }
        return CACHE.computeIfAbsent(recipeId, id -> {
            try {
                var mod = ModList.get().getModFileById(id.m_135827_());     // getNamespace
                if (mod == null) {
                    return true;        // no mod owns this namespace
                }
                Path path = mod.getFile().findResource(
                        "data", id.m_135827_(), "recipes", id.m_135815_() + ".json");  // getPath
                return !Files.exists(path);
            } catch (Throwable e) {
                // Never let a filesystem quirk turn into a wrong claim about
                // who changed what; say nothing instead.
                return false;
            }
        });
    }
}
