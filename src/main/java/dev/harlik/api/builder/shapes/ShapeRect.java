package dev.harlik.api.builder.shapes;

import dev.harlik.impl.batch.RenderBatch;
import dev.harlik.impl.buffer.VertexFormat;
import java.awt.Color;

public class ShapeRect extends Shape<ShapeRect> {

    private float width, height;
    private Color colorTL = Color.WHITE, colorTR = Color.WHITE,
            colorBR = Color.WHITE, colorBL = Color.WHITE;
    private float radiusTL, radiusTR, radiusBR, radiusBL;
    private float smoothness;

    public ShapeRect(RenderBatch batch) {
        super(batch);
    }

    @Override
    public void submit() {
        int stride = VertexFormat.RECTANGLE.getFloats();
        float[] data = new float[6 * stride];

        float absX = absoluteX(), absY = absoluteY();
        float absOpacity = absoluteOpacity();

        putVertex(data, stride * 0, absX, absY, colorTL, 0, 0, absOpacity);
        putVertex(data, stride * 1, absX + width, absY, colorTR, width, 0, absOpacity);
        putVertex(data, stride * 2, absX + width, absY + height, colorBR, width, height, absOpacity);

        putVertex(data, stride * 3, absX, absY, colorTL, 0, 0, absOpacity);
        putVertex(data, stride * 4, absX + width, absY + height, colorBR, width, height, absOpacity);
        putVertex(data, stride * 5, absX, absY + height, colorBL, 0, height, absOpacity);

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
    public ShapeRect pos(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /** Sets the top-left position in pixels with z-order. */
    public ShapeRect pos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /** Sets the size in pixels */
    public ShapeRect size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /** Sets the overall color */
    public ShapeRect color(Color color) {
        this.colorTL = color;
        this.colorTR = color;
        this.colorBR = color;
        this.colorBL = color;
        return this;
    }

    /** Sets a per corner color */
    public ShapeRect color(Color colorTL, Color colorTR, Color colorBR, Color colorBL) {
        this.colorTL = colorTL;
        this.colorTR = colorTR;
        this.colorBR = colorBR;
        this.colorBL = colorBL;
        return this;
    }

    /** Sets an overall radius for corners */
    public ShapeRect radius(float radius) {
        this.radiusTL = radius;
        this.radiusTR = radius;
        this.radiusBR = radius;
        this.radiusBL = radius;
        return this;
    }

    /** Sets a radius with a smoothness.
     * @param smoothness a 0-1 float, that defines how much corners will be smoothed
     * */
    public ShapeRect radius(float radius, float smoothness) {
        this.radiusTL = radius;
        this.radiusTR = radius;
        this.radiusBR = radius;
        this.radiusBL = radius;
        this.smoothness = smoothness;
        return this;
    }

    /** Sets a per corner radius */
    public ShapeRect radius(float radiusTL, float radiusTR, float radiusBR, float radiusBL) {
        this.radiusTL = radiusTL;
        this.radiusTR = radiusTR;
        this.radiusBR = radiusBR;
        this.radiusBL = radiusBL;
        return this;
    }
}
