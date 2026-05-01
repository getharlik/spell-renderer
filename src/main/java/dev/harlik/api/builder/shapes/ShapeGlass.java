package dev.harlik.api.builder.shapes;

import dev.harlik.impl.batch.RenderBatch;
import dev.harlik.impl.buffer.VertexFormat;
import java.awt.Color;

public class ShapeGlass extends Shape<ShapeGlass> {

    private float width, height;
    private Color color = Color.WHITE;
    private float radiusTL, radiusTR, radiusBR, radiusBL;
    private float smoothness;

    public ShapeGlass(RenderBatch batch) {
        super(batch);
    }

    @Override
    public void submit() {
        int stride = VertexFormat.RECTANGLE.getFloats();
        float[] data = new float[6 * stride];

        float absX = absoluteX(), absY = absoluteY();
        float absOpacity = absoluteOpacity();

        putVertex(data, stride * 0, absX, absY, color, 0, 0, absOpacity);
        putVertex(data, stride * 1, absX + width, absY, color, width, 0, absOpacity);
        putVertex(data, stride * 2, absX + width, absY + height, color, width, height, absOpacity);

        putVertex(data, stride * 3, absX, absY, color, 0, 0, absOpacity);
        putVertex(data, stride * 4, absX + width, absY + height, color, width, height, absOpacity);
        putVertex(data, stride * 5, absX, absY + height, color, 0, height, absOpacity);

        batch.addShape(absoluteZ(), data, 0);
    }

    private void putVertex(float[] data, int offset, float vx, float vy, Color c, float u, float v, float absOpacity) {
        data[offset] = vx;
        data[offset + 1] = vy;
        data[offset + 2] = width;
        data[offset + 3] = height;
        data[offset + 4] = c.getRed() / 255f;
        data[offset + 5] = c.getGreen() / 255f;
        data[offset + 6] = c.getBlue() / 255f;
        data[offset + 7] = c.getAlpha() / 255f * absOpacity;
        data[offset + 8] = radiusTL;
        data[offset + 9] = radiusTR;
        data[offset + 10] = radiusBR;
        data[offset + 11] = radiusBL;
        data[offset + 12] = smoothness;
        data[offset + 13] = u;
        data[offset + 14] = v;
    }

    /** Sets the top-left position in pixels */
    public ShapeGlass pos(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /** Sets the top-left position in pixels with z-order. */
    public ShapeGlass pos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /** Sets the size in pixels */
    public ShapeGlass size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /** Sets the color of the tint */
    public ShapeGlass color(Color color) {
        this.color = color;
        return this;
    }

    /** Sets an overall radius for corners */
    public ShapeGlass radius(float radius) {
        this.radiusTL = radius;
        this.radiusTR = radius;
        this.radiusBR = radius;
        this.radiusBL = radius;
        return this;
    }

    /** Sets a radius with a smoothness.
     * @param smoothness a 0-1 float, that defines how much corners will be smoothed
     * */
    public ShapeGlass radius(float radius, float smoothness) {
        this.radiusTL = radius;
        this.radiusTR = radius;
        this.radiusBR = radius;
        this.radiusBL = radius;
        this.smoothness = smoothness;
        return this;
    }

    /** Sets a per corner radius */
    public ShapeGlass radius(float radiusTL, float radiusTR, float radiusBR, float radiusBL) {
        this.radiusTL = radiusTL;
        this.radiusTR = radiusTR;
        this.radiusBR = radiusBR;
        this.radiusBL = radiusBL;
        return this;
    }
}
