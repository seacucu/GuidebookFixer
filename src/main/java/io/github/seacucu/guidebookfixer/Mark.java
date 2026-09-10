package io.github.seacucu.guidebookfixer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * The correction tape: a badge in the corner of a recipe this mod filled in or
 * swapped out, and on hover a tooltip saying who did it and what to distrust.
 *
 * <p>Following what Almost Unified does to JEI recipes it changed. Saying who
 * is responsible matters more than the badge: a reader looking at a recipe that
 * disagrees with the paragraph above it should be able to find out why in one
 * hover, and should not open an issue against the mod whose guidebook it is.
 *
 * <p>Guidebooks share no rendering model, so there is no single way to attach
 * this. What they do share is the mark itself, which is why the texture, the
 * wording and the drawing live here rather than inside any one book's package.
 * A book with a layout engine can place the badge through it; a book that
 * paints a fixed rectangle gets {@link #render} on top of that rectangle.
 */
public final class Mark {
    private Mark() {
    }

    public static final ResourceLocation TEXTURE =
            new ResourceLocation("guidebookfixer", "textures/gui/correction_tape.png");

    public static final int SIZE = 16;

    /**
     * @param packAuthored whether the recipe on display was added by the
     *                     modpack rather than shipped by the mod that owns the
     *                     book, which is when the surrounding text is likely to
     *                     be describing something else entirely
     */
    public static List<Component> tooltip(boolean packAuthored) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.m_237115_("guidebookfixer.mark.title"));         // translatable
        lines.add(Component.m_237115_("guidebookfixer.mark.simplified"));
        if (packAuthored) {
            lines.add(Component.m_237115_("guidebookfixer.mark.pack_changed"));
        }
        return lines;
    }

    /**
     * Draws the badge at {@code (x, y)} and, when the pointer is on it, the
     * tooltip. For books that paint a recipe into a rectangle they own: pass
     * the corner you want the badge in.
     */
    public static void render(GuiGraphics graphics, int x, int y,
                              int mouseX, int mouseY, boolean packAuthored) {
        // GuiGraphics.blit(texture, x, y, u, v, width, height, texWidth, texHeight).
        // The short overload assumes a 256x256 sheet; this one is a 16x16 file.
        graphics.m_280543_(TEXTURE, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
        if (mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE) {
            Minecraft mc = Minecraft.m_91087_();                              // getInstance
            // GuiGraphics.renderComponentTooltip(font, lines, x, y)
            graphics.m_280666_(mc.f_91062_, tooltip(packAuthored), mouseX, mouseY);
        }
    }
}
