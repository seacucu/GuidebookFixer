package io.github.seacucu.guidebookfixes;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Line breaking that works for scripts without inter-word spaces.
 *
 * <p>Several mods roll their own word wrapping as {@code text.split(" ")}, which
 * treats a whole Chinese/Japanese/Korean paragraph as one unbreakable word: the
 * paragraph is drawn as a single line that runs off the page. Minecraft's own
 * {@code StringSplitter} already breaks CJK correctly — that is why vanilla
 * books and tooltips are fine — so we simply hand the work to it.
 *
 * <p>NOTE ON NAMES: this mod is compiled against the SRG-named Minecraft jar,
 * which is the shape Forge actually runs in production. Vanilla members
 * therefore appear as {@code m_92865_} rather than {@code getSplitter}. The
 * mapping is spelled out on each call so the code stays readable.
 */
public final class TextWrap {
    private TextWrap() {
    }

    /** Splits {@code text} into lines no wider than {@code maxWidth} pixels. */
    public static List<String> lines(Font font, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        // font.getSplitter().splitLines(text, maxWidth, Style.EMPTY)
        for (FormattedText line : font.m_92865_().m_92432_(text, maxWidth, Style.f_131099_)) {
            out.add(line.getString());
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
    }
}
