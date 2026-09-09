package io.github.seacucu.guidebookfixes.mixin.eidolon;

import elucent.eidolon.codex.Page;
import io.github.seacucu.guidebookfixes.TextWrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Eidolon's codex wraps text with {@code text.split(" ")}, so a Chinese,
 * Japanese or Korean paragraph is one unbreakable "word": the page emits an
 * empty line and then draws the whole paragraph as one line several times wider
 * than the 120px column, spilling across the facing page.
 *
 * <p>We replace the layout pass with Minecraft's own line breaker and then draw
 * through Eidolon's {@code drawText}, so the shadowed, tinted look of the codex
 * is untouched. Latin text lays out exactly as before.
 */
@Mixin(value = Page.class, remap = false)
public class PageMixin {

    @Inject(method = "drawWrappingText", at = @At("HEAD"), cancellable = true)
    private static void guidebookfixes$wrapForCjk(GuiGraphics graphics, String text,
                                                  int x, int y, int width, CallbackInfo ci) {
        Font font = Minecraft.m_91087_().f_91062_;          // Minecraft.getInstance().font
        List<String> lines = TextWrap.lines(font, text, width);
        int lineHeight = font.f_92710_ + 1;                  // font.lineHeight, matching Eidolon's spacing
        for (int i = 0; i < lines.size(); i++) {
            Page.drawText(graphics, lines.get(i), x, y + i * lineHeight);
        }
        ci.cancel();
    }
}
