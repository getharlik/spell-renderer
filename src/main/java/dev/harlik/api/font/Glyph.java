package dev.harlik.api.font;

public record Glyph(
        int codepoint,
        float advance,
        float planeLeft, float planeBottom, float planeRight, float planeTop,
        float atlasLeft, float atlasBottom, float atlasRight, float atlasTop,
        boolean visible
) {}
