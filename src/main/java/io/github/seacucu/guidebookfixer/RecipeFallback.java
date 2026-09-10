package io.github.seacucu.guidebookfixer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the recipe a guidebook page meant to show, when the id it was written
 * against no longer exists.
 *
 * <p>Guidebooks hardcode recipe ids. A modpack that removes a recipe and adds
 * its own replacement gives that replacement a fresh id, so the lookup fails
 * and the page has nothing to draw. The item the page is about is still right,
 * though, so we ask the far more durable question, what currently produces
 * this item, and hand back that recipe. The page then shows what the player
 * can actually craft.
 *
 * <p>Nothing here touches the recipe registry. It is a read-only lookup.
 */
public final class RecipeFallback {
    private RecipeFallback() {
    }

    /** Cache of page class -> the recipe class that page is able to render. */
    private static final Map<Class<?>, Class<?>> EXPECTED = new ConcurrentHashMap<>();

    /**
     * A page renders one specific kind of recipe and will class-cast if handed
     * another, so a crafting page must never be given a smelting recipe. Rather
     * than hardcode the page classes, we read the return type of the page's own
     * {@code getRecipe} override: that is precisely the type it can render, and
     * it keeps working for page types that do not exist yet.
     */
    private static Class<?> expectedRecipeClass(Class<?> pageClass) {
        return EXPECTED.computeIfAbsent(pageClass, c -> {
            for (Class<?> k = c; k != null; k = k.getSuperclass()) {
                for (Method m : k.getDeclaredMethods()) {
                    // Erasure leaves a bridge method returning Recipe next to the
                    // real override; the bridge is the one we must ignore.
                    if (m.isBridge() || m.isSynthetic()) {
                        continue;
                    }
                    if (m.getName().equals("getRecipe") && m.getParameterCount() == 1
                            && Recipe.class.isAssignableFrom(m.getReturnType())) {
                        return m.getReturnType();
                    }
                }
            }
            return Recipe.class;
        });
    }

    /**
     * The recipe set currently loaded, as an identity a caller can compare
     * against to tell whether a previous search is still valid. Changes when
     * the player joins another world or a datapack reload happens.
     */
    public static Object currentRecipeSet() {
        Minecraft mc = Minecraft.m_91087_();                          // getInstance
        Level level = mc == null ? null : mc.f_91073_;                // Minecraft.level
        return level == null ? null : level.m_7465_();                // getRecipeManager
    }

    /**
     * @param page   the guidebook page asking for the recipe
     * @param result the item the page is documenting
     * @return a recipe of the kind {@code page} can render that produces
     *         {@code result}, or null if the modpack left none
     */
    public static Recipe<?> find(Object page, ItemStack result) {
        if (page == null || result == null || result.m_41619_()) {   // ItemStack.isEmpty
            return null;
        }
        return producing(result.m_41720_(), expectedRecipeClass(page.getClass()), null);
    }

    /**
     * Guidebooks that only carry a recipe id, with no item alongside it, would
     * otherwise be unfixable. Most mods name a recipe after what it makes
     * ({@code eidolon:worktable}, {@code botania:diluted_pool}), so when the id
     * also names an item we can recover the intent. Ids that are not item ids
     * ({@code ae2:transform/fluix_crystals}) get no fallback, which is the
     * honest answer rather than a guess.
     */
    public static Recipe<?> byRecipeId(ResourceLocation id) {
        return byRecipeId(id, null, null);
    }

    /**
     * Same idea as {@link #byRecipeId}, but the caller knows exactly which
     * recipe type the page renders, which is a tighter filter than any guess we
     * could make from the page class.
     */
    public static Recipe<?> byRecipeId(ResourceLocation id, RecipeType<?> type) {
        return byRecipeId(id, null, type);
    }

    private static Recipe<?> byRecipeId(ResourceLocation id, Class<?> expected, RecipeType<?> type) {
        Item item = intended(id);
        return item == null ? null : producing(item, expected, type);
    }

    /**
     * The item a recipe id was about. Most mods name a recipe after what it
     * makes, so the id itself usually says. When it does not, the mod that
     * shipped the recipe still has the original json in its jar, and that says.
     */
    private static Item intended(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        if (BuiltInRegistries.f_257033_.m_7804_(id)) {                 // ITEM.containsKey
            return BuiltInRegistries.f_257033_.m_7745_(id);            // ITEM.get
        }
        return IntendedResult.of(id);
    }

    /**
     * Which kinds of recipe currently produce this item, as registry ids.
     *
     * <p>A guidebook that reports "no recipe" may simply have no renderer for
     * the kind of recipe involved: GuideME draws crafting, smelting, smithing
     * and cooking, so an item a modpack moved to a Create crusher or a Thermal
     * pulverizer looks missing to it while being perfectly craftable. Telling
     * those two situations apart is the difference between a true statement and
     * a false one, so the caller needs to know.
     *
     * @return the distinct recipe type ids, empty if nothing produces the item
     */
    public static List<String> recipeTypesProducing(ResourceLocation itemId) {
        if (itemId == null || !BuiltInRegistries.f_257033_.m_7804_(itemId)) {   // ITEM.containsKey
            return List.of();
        }
        Item item = BuiltInRegistries.f_257033_.m_7745_(itemId);               // ITEM.get
        Minecraft mc = Minecraft.m_91087_();                                    // getInstance
        Level level = mc == null ? null : mc.f_91073_;                          // Minecraft.level
        if (level == null) {
            return List.of();
        }
        SortedSet<String> types = new TreeSet<>();
        for (Recipe<?> recipe : level.m_7465_().m_44051_()) {                   // getRecipes
            try {
                ItemStack out = recipe.m_8043_(level.m_9598_());                // getResultItem
                if (out != null && out.m_41720_() == item) {                    // getItem
                    ResourceLocation key = BuiltInRegistries.f_256990_.m_7981_(recipe.m_6671_());
                    types.add(key == null ? recipe.m_6671_().toString() : key.toString());
                }
            } catch (Throwable ignored) {
                // see producing()
            }
        }
        return new ArrayList<>(types);
    }

    /**
     * @param expected restrict to recipes of this class, or null for any kind
     * @param type     restrict to this recipe type, or null for any
     */
    private static Recipe<?> producing(Item item, Class<?> expected, RecipeType<?> type) {
        Minecraft mc = Minecraft.m_91087_();                          // getInstance
        Level level = mc == null ? null : mc.f_91073_;                // Minecraft.level
        if (level == null || item == null) {
            return null;
        }
        List<Recipe<?>> candidates = new ArrayList<>();
        for (Recipe<?> recipe : level.m_7465_().m_44051_()) {         // getRecipeManager().getRecipes()
            if (expected != null && !expected.isInstance(recipe)) {
                continue;
            }
            if (type != null && recipe.m_6671_() != type) {            // Recipe.getType
                continue;
            }
            try {
                ItemStack out = recipe.m_8043_(level.m_9598_());       // getResultItem(registryAccess)
                if (out != null && out.m_41720_() == item) {           // ItemStack.getItem
                    candidates.add(recipe);
                }
            } catch (Throwable ignored) {
                // A recipe that cannot describe its own result is no use to us,
                // and a broken third-party recipe must not take the book down.
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        // getRecipes() has no defined order. Pick by id so the page shows the
        // same recipe every time rather than changing between sessions.
        candidates.sort(Comparator.comparing(r -> r.m_6423_().toString()));   // getId
        return candidates.get(0);
    }
}
