package io.github.seacucu.guidebookfixer.mixin.guideme;

import guideme.document.LytRect;
import guideme.document.block.recipes.LytStandardRecipeBox;
import guideme.document.interaction.GuideTooltip;
import guideme.document.interaction.InteractiveElement;
import guideme.document.interaction.TextTooltip;
import guideme.render.RenderContext;
import io.github.seacucu.guidebookfixer.Mark;
import io.github.seacucu.guidebookfixer.Marked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Puts the correction tape on a GuideME recipe box, whether this mod drew the
 * box or merely fed GuideME a different recipe than the page asked for.
 *
 * <p>Unlike the other books, this one goes through GuideME's own layout rather
 * than painting over the page. The guide scrolls and clips; a badge drawn on
 * top would have to track both, while a badge that is part of the box gets
 * that for free, and the hover comes from GuideME's own hit testing.
 */
@Mixin(value = LytStandardRecipeBox.class, remap = false)
public abstract class LytStandardRecipeBoxMixin implements Marked, InteractiveElement {

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

    @Unique
    private LytRect guidebookfixer$badge() {
        LytRect bounds = ((LytStandardRecipeBox<?>) (Object) this).getBounds();
        return new LytRect(bounds.right() - Mark.SIZE, bounds.y(), Mark.SIZE, Mark.SIZE);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void guidebookfixer$drawMark(RenderContext context, CallbackInfo ci) {
        if (!this.guidebookfixer$substituted) {
            return;
        }
        LytRect badge = guidebookfixer$badge();
        // GuiGraphics.blit(texture, x, y, u, v, w, h, texWidth, texHeight)
        context.guiGraphics().m_280543_(Mark.TEXTURE, badge.x(), badge.y(),
                0, 0, Mark.SIZE, Mark.SIZE, Mark.SIZE, Mark.SIZE);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (!this.guidebookfixer$substituted) {
            return Optional.empty();
        }
        LytRect badge = guidebookfixer$badge();
        if (x < badge.x() || x >= badge.right() || y < badge.y() || y >= badge.bottom()) {
            return Optional.empty();
        }
        return Optional.of(new TextTooltip(Mark.tooltip(this.guidebookfixer$packAuthored)));
    }
}
