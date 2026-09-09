package io.github.seacucu.guidebookfixes.mixin.guideme;

import guideme.compiler.PageCompiler;
import guideme.compiler.tags.RecipeCompiler;
import guideme.document.block.LytBlockContainer;
import guideme.libs.unist.UnistNode;
import io.github.seacucu.guidebookfixes.RecipeFallback;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * GuideME (the AE2 guide) draws compile errors straight onto the page, so a
 * recipe the modpack changed shows the reader an English
 * {@code Couldn't find recipe ae2:transform/fluix_crystals}.
 *
 * <p>Two changes, both confined to what the page displays:
 * <ol>
 *   <li>{@code <Recipe id="...">} looks recipes up by id. When that fails and
 *       the id also names an item, fall back to whatever currently produces
 *       it. ({@code <RecipeFor>} already searches by item, so it needs no
 *       help — when it fails, the recipe really is gone.)</li>
 *   <li>the remaining errors are said in the player's language.</li>
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
        parent.appendError(compiler, guidebookfixes$translate(message), node);
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
            return I18n.m_118938_("guidebookfixes.recipe_removed.id",
                    message.substring("Couldn't find recipe for ".length()));
        }
        if (message.startsWith("Couldn't find recipe ")) {
            return I18n.m_118938_("guidebookfixes.recipe_removed.id",
                    message.substring("Couldn't find recipe ".length()));
        }
        return message;
    }
}
