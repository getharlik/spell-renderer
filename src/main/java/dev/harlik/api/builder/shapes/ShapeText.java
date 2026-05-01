package dev.harlik.api.builder.shapes;

import dev.harlik.api.font.Font;
import dev.harlik.api.font.FontManager;
import dev.harlik.api.font.Glyph;
import dev.harlik.impl.batch.RenderBatch;
import dev.harlik.impl.buffer.VertexFormat;
import java.awt.Color;

public class ShapeText extends Shape<ShapeText> {

    private final String text;
    private Color color = Color.WHITE;
    private String fontKey = "spell_fallback";
    private float size = 16;

    public ShapeText(RenderBatch batch, String text) {
        super(batch);
        this.text = text;
    }

    @Override
    public void submit() {
        if (text.isEmpty()) return;
        Font font = FontManager.INSTANCE.get(fontKey);
        if (font == null) return;

        int[] codepoints = text.codePoints().toArray();
        int visible = 0;
        for (int cp : codepoints) {
            if (cp == '\n') continue;
            Glyph g = font.getGlyph(cp);
            if (g != null && g.visible()) visible++;
        }
        if (visible == 0) return;

        int stride = VertexFormat.TEXT.getFloats();
        float[] data = new float[visible * 6 * stride];

        float absX = absoluteX(), absY = absoluteY();
        float absOpacity = absoluteOpacity();
        float pxRange = font.getDistanceRange() * (size / font.getAtlasGlyphSize());

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f * absOpacity;

        float invW = 1f / font.getAtlasWidth();
        float invH = 1f / font.getAtlasHeight();
        float ascender = font.getAscender();

        float pen = 0;
        float line = 0;
        int offset = 0;

        for (int cp : codepoints) {
            if (cp == '\n') {
                pen = 0;
                line += font.getLineHeight() * size;
                continue;
            }
            Glyph gl = font.getGlyph(cp);
            if (gl == null) continue;

            if (gl.visible()) {
                float xL = absX + pen + gl.planeLeft() * size;
                float xR = absX + pen + gl.planeRight() * size;
                float yT = absY + line + (ascender - gl.planeTop()) * size;
                float yB = absY + line + (ascender - gl.planeBottom()) * size;

                float uL = gl.atlasLeft() * invW;
                float uR = gl.atlasRight() * invW;
                float vT = 1f - gl.atlasTop() * invH;
                float vB = 1f - gl.atlasBottom() * invH;

                offset = putVertex(data, offset, xL, yT, uL, vT, r, g, b, a, pxRange);
                offset = putVertex(data, offset, xR, yT, uR, vT, r, g, b, a, pxRange);
                offset = putVertex(data, offset, xR, yB, uR, vB, r, g, b, a, pxRange);
                offset = putVertex(data, offset, xL, yT, uL, vT, r, g, b, a, pxRange);
                offset = putVertex(data, offset, xR, yB, uR, vB, r, g, b, a, pxRange);
                offset = putVertex(data, offset, xL, yB, uL, vB, r, g, b, a, pxRange);
            }
            pen += gl.advance() * size;
        }

        batch.addShape(absoluteZ(), data, font.getAtlasId());
    }

    private int putVertex(float[] data, int offset, float x, float y, float u, float v,
                          float r, float g, float b, float a, float pxRange) {
        data[offset] = x;
        data[offset + 1] = y;
        data[offset + 2] = u;
        data[offset + 3] = v;
        data[offset + 4] = r;
        data[offset + 5] = g;
        data[offset + 6] = b;
        data[offset + 7] = a;
        data[offset + 8] = pxRange;
        return offset + 9;
    }

    /** Sets the top-left position in pixels */
    public ShapeText pos(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /** Sets the top-left position in pixels with z-order. */
    public ShapeText pos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /** Sets the font size for the text. */
    public ShapeText size(float size) {
        this.size = size;
        return this;
    }

    /** Sets the color of the text. */
    public ShapeText color(Color color) {
        this.color = color;
        return this;
    }

    /** Sets the font. If font is not found, fallbacks to the built-in font. */
    public ShapeText font(String fontKey) {
        this.fontKey = fontKey;
        return this;
    }

}
