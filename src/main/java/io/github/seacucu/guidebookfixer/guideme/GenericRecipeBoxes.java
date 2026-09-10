package io.github.seacucu.guidebookfixer.guideme;

import guideme.document.block.LytBlock;
import io.github.seacucu.guidebookfixer.Marked;
import io.github.seacucu.guidebookfixer.PackAuthored;
import guideme.document.block.LytSlotGrid;
import guideme.document.block.recipes.LytStandardRecipeBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a recipe of a kind GuideME has no renderer for.
 *
 * <p>GuideME ships renderers for crafting, smelting, smithing and cooking, and
 * mods can contribute more. Everything else it reports as "couldn't find
 * recipe", which is how an item a modpack moved to a Create crusher ends up
 * looking uncraftable in the AE2 guide. Telling the reader to go and look in
 * JEI is a poor answer when the recipe is right there and GuideME already has
 * everything needed to draw it: {@link LytStandardRecipeBox} takes a list of
 * ingredients and a result, which is exactly what {@link Recipe} exposes.
 *
 * <p>The result is a plain inputs-arrow-output box. It cannot show what is
 * special about a given machine (Create's processing time, a pulverizer's
 * secondary chances), so a mod that contributes a proper renderer still wins;
 * this only ever runs where the alternative was nothing at all.
 */
public final class GenericRecipeBoxes {
    private GenericRecipeBoxes() {
    }

    /** More than this on one page stops being useful and starts being a wall. */
    private static final int MAX_BOXES = 6;

    /** Every recipe currently producing {@code itemId}, drawn. */
    public static List<LytBlock> forItem(String itemId) {
        List<LytBlock> boxes = new ArrayList<>();
        ResourceLocation id;
        try {
            id = new ResourceLocation(itemId);
        } catch (Exception e) {
            return boxes;
        }
        if (!BuiltInRegistries.f_257033_.m_7804_(id)) {          // ITEM.containsKey
            return boxes;
        }
        Item item = BuiltInRegistries.f_257033_.m_7745_(id);     // ITEM.get
        Level level = level();
        if (level == null) {
            return boxes;
        }
        for (Recipe<?> recipe : level.m_7465_().m_44051_()) {    // getRecipeManager().getRecipes()
            try {
                ItemStack out = recipe.m_8043_(level.m_9598_()); // getResultItem(registryAccess)
                if (out == null || out.m_41720_() != item) {     // getItem
                    continue;
                }
            } catch (Throwable ignored) {
                continue;
            }
            LytBlock box = forRecipe(recipe);
            if (box != null) {
                boxes.add(box);
                if (boxes.size() >= MAX_BOXES) {
                    break;
                }
            }
        }
        return boxes;
    }

    /**
     * @return a box for this recipe, or null when the recipe cannot describe
     *         itself well enough to draw (no ingredients, or no result)
     */
    public static LytBlock forRecipe(Recipe<?> recipe) {
        Level level = level();
        if (recipe == null || level == null) {
            return null;
        }
        List<Ingredient> inputs = new ArrayList<>();
        try {
            for (Ingredient ingredient : recipe.m_7527_()) {     // getIngredients
                if (!ingredient.m_43947_()) {                    // isEmpty
                    inputs.add(ingredient);
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
        ItemStack result;
        try {
            result = recipe.m_8043_(level.m_9598_());            // getResultItem
        } catch (Throwable ignored) {
            return null;
        }
        if (inputs.isEmpty() || result == null || result.m_41619_()) {   // isEmpty
            return null;
        }
        LytStandardRecipeBox<?> box = LytStandardRecipeBox.builder()
                .title(title(recipe.m_6671_()))                  // getType
                .input(LytSlotGrid.row(inputs, false))
                .output(result)
                .build(recipe);
        // Drawn by us, so it always carries the tape.
        ((Marked) box).guidebookfixer$markSubstituted(PackAuthored.test(recipe.m_6423_()));  // getId
        return box;
    }

    private static Level level() {
        Minecraft mc = Minecraft.m_91087_();                     // getInstance
        return mc == null ? null : mc.f_91073_;                  // Minecraft.level
    }

    /**
     * A readable name for a recipe kind. There is no standard key for this, but
     * mods that support a recipe viewer nearly always ship a category name for
     * one, so try the common conventions before falling back to the raw id.
     */
    private static String title(RecipeType<?> type) {
        ResourceLocation id = BuiltInRegistries.f_256990_.m_7981_(type);   // RECIPE_TYPE.getKey
        if (id == null) {
            return String.valueOf(type);
        }
        String ns = id.m_135827_();                              // getNamespace
        String path = id.m_135815_();                            // getPath
        for (String key : new String[] {
                "emi.category." + ns + "." + path,
                ns + ".recipe." + path,
                "gui.jei.category." + path,
                "jei." + ns + "." + path,
        }) {
            if (I18n.m_118936_(key)) {                           // exists
                return I18n.m_118938_(key);                      // get
            }
        }
        return id.toString();
    }
}
