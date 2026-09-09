package io.github.seacucu.guidebookfixes.mixin.eidolon;

import elucent.eidolon.codex.RecipePage;
import io.github.seacucu.guidebookfixes.RecipeFallback;
import io.github.seacucu.guidebookfixes.TextWrap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Eidolon's codex looks recipes up by a hardcoded id. When a modpack removes
 * the recipe and adds its own under a different id, the page prints
 * {@code "No matching recipe found for eidolon:worktable"} over the page — an
 * untranslated string, and an id that means nothing to a player.
 *
 * <p>Two changes, both confined to what the page displays:
 * <ol>
 *   <li>when the id lookup fails, look the recipe up by the item the page is
 *       about, so the page shows the recipe the pack actually uses;</li>
 *   <li>when the pack removed it outright and there is no replacement, say so
 *       in the player's language instead of printing the id.</li>
 * </ol>
 */
@Mixin(value = RecipePage.class, remap = false)
public abstract class RecipePageMixin {

    @Shadow
    @Final
    ItemStack result;

    /**
     * The recipe set the fallback search was last run against, or null if it
     * has not run. Eidolon only caches a recipe it found, so a page with no
     * recipe at all re-runs {@code getRecipe} every frame; without this the
     * fallback would rescan every loaded recipe sixty times a second. Keyed on
     * the recipe set rather than a plain flag so joining another world or
     * reloading datapacks searches again.
     */
    @Unique
    private Object guidebookfixes$searchedIn;

    /**
     * The redirected instruction is the {@code getRecipe(recipeId)} call inside
     * {@code fullRender}; calling it again here is a different call site, so
     * there is no recursion.
     */
    @SuppressWarnings("rawtypes")
    @Redirect(method = "fullRender",
            at = @At(value = "INVOKE",
                    target = "Lelucent/eidolon/codex/RecipePage;getRecipe"
                            + "(Lnet/minecraft/resources/ResourceLocation;)"
                            + "Lnet/minecraft/world/item/crafting/Recipe;"))
    private Recipe guidebookfixes$orByResult(RecipePage self, ResourceLocation id) {
        Recipe<?> found = self.getRecipe(id);
        if (found != null) {
            return found;
        }
        Object recipes = RecipeFallback.currentRecipeSet();
        if (recipes != null && recipes == this.guidebookfixes$searchedIn) {
            return null;        // already searched this recipe set; nothing produces it
        }
        this.guidebookfixes$searchedIn = recipes;
        return RecipeFallback.find(self, this.result);
    }

    /**
     * A muted brick red, against the dark brown Eidolon draws its own body text
     * in. This line is not part of the book: it is this mod talking about the
     * modpack, and a reader should be able to tell that at a glance.
     */
    @Unique
    private static final int guidebookfixes$NOTE_COLOUR = 0xFF8C2F26;

    /**
     * Only reached when the fallback found nothing either, i.e. the modpack
     * removed the recipe and put nothing in its place. Drawn where Eidolon put
     * its error, wrapped to the page.
     */
    @Redirect(method = "fullRender",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;m_280488_"
                            + "(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
    private int guidebookfixes$sayRemoved(GuiGraphics graphics, Font font, String ignored,
                                          int x, int y, int colour) {
        String message = I18n.m_118938_("guidebookfixes.recipe_removed");   // I18n.get
        List<String> lines = TextWrap.lines(font, message, 108);
        int width = 0;
        for (int i = 0; i < lines.size(); i++) {
            // GuiGraphics.drawString
            width = Math.max(width, graphics.m_280488_(font, lines.get(i), x,
                    y + i * (font.f_92710_ + 1),                          // font.lineHeight
                    guidebookfixes$NOTE_COLOUR));
        }
        return width;
    }
}
