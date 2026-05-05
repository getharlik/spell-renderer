package dev.harlik.api.font;

import java.util.Map;
import lombok.Getter;

public class Font {

    @Getter
    private final Map<Integer, Glyph> glyphs;
    private final Map<String, Integer> names;
    @Getter
    private final float lineHeight, ascender, descender, distanceRange, atlasGlyphSize;
    @Getter
    private final int atlasWidth, atlasHeight, atlasId;

    Font(Map<Integer, Glyph> glyphs,
         Map<String, Integer> names,
         float lineHeight, float ascender, float descender,
         float distanceRange, float atlasGlyphSize,
         int atlasWidth, int atlasHeight, int atlasId) {
        this.glyphs = glyphs;
        this.names = names;
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

    public Glyph getGlyphByName(String name) {
        return glyphs.get(names.get(name));
    }

}
