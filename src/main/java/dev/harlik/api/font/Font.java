package dev.harlik.api.font;

import java.util.Map;
import lombok.Getter;

public class Font {

    private final Map<Integer, Glyph> glyphs;
    @Getter
    private final float lineHeight, ascender, descender, distanceRange, atlasGlyphSize;
    @Getter
    private final int atlasWidth, atlasHeight, atlasId;

    Font(Map<Integer, Glyph> glyphs,
         float lineHeight, float ascender, float descender,
         float distanceRange, float atlasGlyphSize,
         int atlasWidth, int atlasHeight, int atlasId) {
        this.glyphs = glyphs;
        this.lineHeight = lineHeight;
        this.ascender = ascender;
        this.descender = descender;
        this.distanceRange = distanceRange;
        this.atlasGlyphSize = atlasGlyphSize;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.atlasId = atlasId;
    }

    public Glyph getGlyph(int codepoint) {
        return glyphs.get(codepoint);
    }

}
