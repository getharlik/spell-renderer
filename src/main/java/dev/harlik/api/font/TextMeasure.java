package dev.harlik.api.font;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TextMeasure {

    public float getWidth(String fontKey, String text, float size) {
        Font font = FontManager.INSTANCE.get(fontKey);
        if (font == null) return 0;

        float maxWidth = 0;
        float lineWidth = 0;
        for (int cp : text.codePoints().toArray()) {
            if (cp == '\n') {
                if (lineWidth > maxWidth) maxWidth = lineWidth;
                lineWidth = 0;
                continue;
            }
            Glyph g = font.getGlyph(cp);
            if (g == null) continue;
            lineWidth += g.advance() * size;
        }
        return Math.max(maxWidth, lineWidth);
    }

    public float getHeight(String fontKey, float size) {
        Font font = FontManager.INSTANCE.get(fontKey);
        if (font == null) return 0;
        return font.getLineHeight() * size;
    }
}