package io.github.seacucu.guidebookfixer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a recipe the modpack deleted was supposed to make.
 *
 * <p>A guidebook that names a recipe by id and nothing else looks unfixable:
 * the id is gone, and an id is not an item. But the recipe was shipped by a
 * mod, and removing it from the game does not remove it from that mod's jar.
 * The original json is still sitting there saying what it produced, which is
 * exactly the missing piece. Read it, and the by-item search that fixes every
 * other case becomes available here too.
 *
 * <p>Ids like {@code ae2:upgrade/item_storage_cell_1k_to_4k}, which no version
 * of the mod ever shipped, resolve to nothing. That is the right answer: the
 * page is referring to a recipe that does not exist and never did here.
 */
public final class IntendedResult {
    private IntendedResult() {
    }

    private static final Map<ResourceLocation, Item> CACHE = new ConcurrentHashMap<>();
    private static final Item NONE = null;

    public static Item of(ResourceLocation recipeId) {
        if (recipeId == null) {
            return NONE;
        }
        return CACHE.computeIfAbsent(recipeId, IntendedResult::read);
    }

    private static Item read(ResourceLocation id) {
        try {
            var mod = ModList.get().getModFileById(id.m_135827_());          // getNamespace
            if (mod == null) {
                return NONE;
            }
            Path path = mod.getFile().findResource(
                    "data", id.m_135827_(), "recipes", id.m_135815_() + ".json");  // getPath
            if (!Files.exists(path)) {
                return NONE;
            }
            JsonObject json;
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
            return item(resultId(json));
        } catch (Throwable e) {
            // A recipe file we cannot read tells us nothing, which is the same
            // as not finding one.
            return NONE;
        }
    }

    /**
     * Recipe json has no single shape for its output: vanilla writes
     * {@code result} as an object or, for stonecutting, a bare id; many mods
     * write {@code results} as a list. Take the first thing that looks like an
     * item id.
     */
    private static String resultId(JsonObject json) {
        for (String field : new String[] {"result", "results", "output", "outputs"}) {
            JsonElement element = json.get(field);
            String found = fromElement(element);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String fromElement(JsonElement element) {
        if (element == null) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            String s = element.getAsString();
            return s.contains(":") ? s : null;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : new String[] {"item", "id"}) {
                if (object.has(key) && object.get(key).isJsonPrimitive()) {
                    return object.get(key).getAsString();
                }
            }
            return null;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                String found = fromElement(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Item item(String id) {
        if (id == null) {
            return NONE;
        }
        try {
            ResourceLocation key = new ResourceLocation(id);
            if (!BuiltInRegistries.f_257033_.m_7804_(key)) {                 // ITEM.containsKey
                return NONE;
            }
            return BuiltInRegistries.f_257033_.m_7745_(key);                 // ITEM.get
        } catch (Exception e) {
            return NONE;
        }
    }
}
