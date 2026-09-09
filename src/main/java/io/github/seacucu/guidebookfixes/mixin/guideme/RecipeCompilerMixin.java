package io.github.seacucu.guidebookfixes.mixin.guideme;

import guideme.compiler.PageCompiler;
import guideme.color.SymbolicColor;
import guideme.compiler.tags.RecipeCompiler;
import guideme.document.block.LytBlockContainer;
import guideme.document.block.LytParagraph;
import guideme.document.flow.LytFlowSpan;
import guideme.libs.unist.UnistNode;
import io.github.seacucu.guidebookfixes.RecipeFallback;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Optional;

/**
 * GuideME (the AE2 guide) draws compile errors straight onto the page, so a
 * recipe the modpack changed shows the reader an English
 * {@code Couldn't find recipe ae2:transform/fluix_crystals}.
 *
 * <p>Three changes, all confined to what the page displays:
 * <ol>
 *   <li>{@code <Recipe id="...">} looks recipes up by id. When that fails and
 *       the id also names an item, fall back to whatever currently produces
 *       it.</li>
 *   <li>{@code <RecipeFor>} searches by item, but only among the recipe kinds
 *       GuideME can draw: crafting, smelting, smithing and cooking. An item a
 *       modpack moved to a Create crusher or a Thermal pulverizer is perfectly
 *       craftable and still reported missing, so we ask the recipe manager
 *       ourselves and say which of the two situations it is. Claiming a recipe
 *       was removed when it was merely moved is worse than saying nothing.</li>
 *   <li>the messages are said in the player's language, and without the source
 *       position and MDX excerpt {@code appendError} attaches for authors.</li>
 * </ol>
 */
@Mixin(value = RecipeCompiler.class, remap = false)
public abstract class RecipeCompilerMixin {

    @SuppressWarnings("rawtypes")
    @Redirect(method = "compile",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager;m_44043_"
                            + "(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"))
    private Optional guidebookfixes$orByResult(RecipeManager manager, ResourceLocation id) {
        Optional<? extends Recipe<?>> found = manager.m_44043_(id);   // byKey
        if (found.isPresent()) {
            return found;
        }
        Recipe<?> alternative = RecipeFallback.byRecipeId(id);
        return alternative == null ? found : Optional.of(alternative);
    }

    @Redirect(method = "compile",
            at = @At(value = "INVOKE",
                    target = "Lguideme/document/block/LytBlockContainer;appendError"
                            + "(Lguideme/compiler/PageCompiler;Ljava/lang/String;"
                            + "Lguideme/libs/unist/UnistNode;)V"))
    private void guidebookfixes$sayItInTheirLanguage(LytBlockContainer parent, PageCompiler compiler,
                                                     String message, UnistNode node) {
        String translated = guidebookfixes$translate(message);
        if (translated.equals(message)) {
            // Not one of ours: a genuine authoring error, where GuideME's line
            // number and source excerpt are exactly what the author needs.
            parent.appendError(compiler, message, node);
            return;
        }
        // Our messages are addressed to the player, so drop the diagnostics
        // appendError attaches: the node type, "(22:1)", the raw MDX line and a
        // caret under it belong in a log, not on a page someone is reading.
        LytFlowSpan span = new LytFlowSpan();
        span.modifyStyle(style -> style.color(SymbolicColor.ERROR_TEXT));
        span.appendText(translated);
        LytParagraph paragraph = new LytParagraph();
        paragraph.append(span);
        parent.append(paragraph);
    }

    /**
     * GuideME builds these messages by string concatenation, so matching on the
     * English prefix is the only handle we have. If a GuideME update rewords
     * one, the message simply passes through untranslated rather than breaking.
     */
    private static String guidebookfixes$translate(String message) {
        if (message.equals("Cannot show recipe while not in-game")) {
            return I18n.m_118938_("guidebookfixes.guide.not_in_game");
        }
        if (message.startsWith("Couldn't find a handler for recipe ")) {
            return I18n.m_118938_("guidebookfixes.guide.no_renderer",
                    message.substring("Couldn't find a handler for recipe ".length()));
        }
        if (message.startsWith("Couldn't find recipe for ")) {
            return guidebookfixes$aboutItem(message.substring("Couldn't find recipe for ".length()));
        }
        if (message.startsWith("Couldn't find recipe ")) {
            return I18n.m_118938_("guidebookfixes.guide.recipe_missing",
                    message.substring("Couldn't find recipe ".length()));
        }
        return message;
    }

    /**
     * {@code <RecipeFor>} says "no recipe" for two very different situations,
     * and saying the wrong one is worse than saying nothing. GuideME only draws
     * crafting, smelting, smithing and cooking, so an item a modpack moved to,
     * say, a Create crusher is perfectly craftable and still reported missing.
     * Ask the recipe manager directly and tell the reader which case this is.
     */
    private static String guidebookfixes$aboutItem(String itemId) {
        List<String> types;
        try {
            types = RecipeFallback.recipeTypesProducing(new ResourceLocation(itemId));
        } catch (Exception e) {
            types = List.of();
        }
        if (types.isEmpty()) {
            return I18n.m_118938_("guidebookfixes.recipe_removed.id", itemId);
        }
        return I18n.m_118938_("guidebookfixes.guide.not_drawable", String.join(", ", types));
    }
}
