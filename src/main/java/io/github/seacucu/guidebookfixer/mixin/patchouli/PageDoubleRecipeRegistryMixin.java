package io.github.seacucu.guidebookfixer.mixin.patchouli;

import io.github.seacucu.guidebookfixer.RecipeFallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

/**
 * Patchouli recipe pages name a recipe by id. When a modpack removes that
 * recipe and adds its own replacement, the id no longer resolves, the page
 * silently renders nothing, and the only trace is a
 * {@code Recipe x (of type y) not found} line in the log.
 *
 * <p>When the lookup fails and the id also names an item, we look for whatever
 * currently produces that item. The page's own {@code recipeType} field is the
 * filter, so a crafting page can only ever be handed a crafting recipe.
 *
 * <p>The fallback deliberately does not register the result with
 * {@code addRelevantStack}: that index drives "which page documents this item"
 * lookups, and an entry the book author never wrote is not something to invent.
 */
@Mixin(value = PageDoubleRecipeRegistry.class, remap = false)
public abstract class PageDoubleRecipeRegistryMixin {

    @SuppressWarnings("rawtypes")
    @Shadow
    @Final
    private RecipeType recipeType;

    @SuppressWarnings("rawtypes")
    @Inject(method = "loadRecipe(Lnet/minecraft/world/level/Level;"
                   + "Lvazkii/patchouli/client/book/BookContentsBuilder;"
                   + "Lvazkii/patchouli/client/book/BookEntry;"
                   + "Lnet/minecraft/resources/ResourceLocation;)"
                   + "Lnet/minecraft/world/item/crafting/Recipe;",
            at = @At("RETURN"), cancellable = true)
    private void guidebookfixer$orByResult(Level level, BookContentsBuilder builder, BookEntry entry,
                                           ResourceLocation res, CallbackInfoReturnable<Recipe> cir) {
        if (cir.getReturnValue() != null || res == null || level == null) {
            return;
        }
        Recipe<?> alternative = RecipeFallback.byRecipeId(res, this.recipeType);
        if (alternative != null) {
            cir.setReturnValue(alternative);
        }
    }
}
