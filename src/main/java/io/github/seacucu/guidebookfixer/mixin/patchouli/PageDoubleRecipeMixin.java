package io.github.seacucu.guidebookfixer.mixin.patchouli;

import io.github.seacucu.guidebookfixer.Mark;
import io.github.seacucu.guidebookfixer.Marked;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipe;

/**
 * Patchouli paints its own recipe box, so the correction tape goes on top of
 * the page rather than into a layout slot.
 *
 * <p>This also covers the quieter failure. When a modpack removes a recipe and
 * puts nothing in its place, Patchouli logs a line and draws nothing at all:
 * the page keeps its heading and its prose and simply has a hole where the
 * recipe was. A reader cannot tell that from a page that never had a recipe,
 * so say it.
 */
@Mixin(value = PageDoubleRecipe.class, remap = false)
public abstract class PageDoubleRecipeMixin implements Marked {

    @Shadow
    protected Object recipe1;

    @Shadow
    ResourceLocation recipeId;

    @Shadow
    protected abstract int getX();

    @Shadow
    protected abstract int getY();

    @Shadow
    protected abstract int getRecipeHeight();

    @Unique
    private boolean guidebookfixer$substituted;

    @Unique
    private boolean guidebookfixer$packAuthored;

    @Override
    public void guidebookfixer$markSubstituted(boolean packAuthored) {
        this.guidebookfixer$substituted = true;
        this.guidebookfixer$packAuthored = packAuthored;
    }

    @Override
    public boolean guidebookfixer$isSubstituted() {
        return this.guidebookfixer$substituted;
    }

    @Override
    public boolean guidebookfixer$isPackAuthored() {
        return this.guidebookfixer$packAuthored;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void guidebookfixer$mark(GuiGraphics graphics, int mouseX, int mouseY, float pticks,
                                     CallbackInfo ci) {
        if (this.recipe1 == null) {
            if (this.recipeId != null) {
                guidebookfixer$sayItIsGone(graphics);
            }
            return;
        }
        if (this.guidebookfixer$substituted) {
            Mark.render(graphics, getX() + PAGE_WIDTH - Mark.SIZE, getY(),
                    mouseX, mouseY, this.guidebookfixer$packAuthored);
        }
    }

    /** Patchouli's usable page width, the same figure its own pages lay out to. */
    @Unique
    private static final int PAGE_WIDTH = 100;

    @Unique
    private void guidebookfixer$sayItIsGone(GuiGraphics graphics) {
        Minecraft mc = Minecraft.m_91087_();                                  // getInstance
        // GuiGraphics.drawWordWrap(font, text, x, y, lineWidth, colour)
        graphics.m_280554_(mc.f_91062_, Component.m_237115_("guidebookfixer.recipe_removed"),
                getX(), getY(), PAGE_WIDTH, 0x8C2F26);
    }
}
