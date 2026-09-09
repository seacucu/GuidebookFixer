package io.github.seacucu.guidebookfixes.guideme;

import guideme.document.block.LytBlock;
import guideme.document.block.LytImage;
import guideme.document.block.LytParagraph;
import guideme.document.flow.LytFlowInlineBlock;
import guideme.document.flow.LytTooltipSpan;
import guideme.document.interaction.TextTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The correction-tape mark that goes in the title bar of a recipe box this mod
 * filled in, following what Almost Unified does on JEI recipes it changed: a
 * small icon, and on hover a tooltip that names the mod responsible and warns
 * the reader off blaming the wrong author.
 *
 * <p>Saying who did this matters more than the icon. A reader who finds a
 * recipe box that disagrees with the paragraph above it should be able to work
 * out why in one hover, and should not open an issue against the mod whose
 * guidebook it is.
 */
public final class FixMark {
    private FixMark() {
    }

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("guidebookfixes", "textures/gui/correction_tape.png");

    private static byte[] texture;
    private static boolean textureLoaded;

    /**
     * @param packAuthored whether the recipe being drawn was added by the
     *                     modpack rather than shipped by the mod that owns the
     *                     book, which is when the surrounding text is likely to
     *                     be describing something else entirely
     */
    public static LytBlock icon(boolean packAuthored) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.m_237115_("guidebookfixes.mark.title"));        // translatable
        lines.add(Component.m_237115_("guidebookfixes.mark.simplified"));
        if (packAuthored) {
            lines.add(Component.m_237115_("guidebookfixes.mark.pack_changed"));
        }

        LytTooltipSpan span = new LytTooltipSpan();
        span.setTooltip(new TextTooltip(lines));

        byte[] png = texture();
        if (png != null) {
            LytImage image = new LytImage();
            image.setImage(TEXTURE, png);
            LytFlowInlineBlock inline = new LytFlowInlineBlock();
            inline.setBlock(image);
            span.append(inline);
        } else {
            // No icon is better than no mark: the tooltip is the part that
            // carries the information.
            span.appendText("*");
        }

        LytParagraph holder = new LytParagraph();
        holder.append(span);
        return holder;
    }

    private static byte[] texture() {
        if (textureLoaded) {
            return texture;
        }
        textureLoaded = true;
        try {
            Minecraft mc = Minecraft.m_91087_();                            // getInstance
            Optional<Resource> res = mc.m_91098_().m_213713_(TEXTURE);      // getResourceManager().getResource
            if (res.isPresent()) {
                try (InputStream in = res.get().m_215507_()) {              // open
                    texture = in.readAllBytes();
                }
            }
        } catch (Throwable ignored) {
            texture = null;
        }
        return texture;
    }
}
