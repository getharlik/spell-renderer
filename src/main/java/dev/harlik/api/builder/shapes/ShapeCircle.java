package dev.harlik.api.builder.shapes;

import dev.harlik.impl.batch.RenderBatch;
import dev.harlik.impl.buffer.VertexFormat;
import java.awt.Color;

public class ShapeCircle extends Shape<ShapeCircle> {

    private float radius;
    private Color color = Color.WHITE;

    public ShapeCircle(RenderBatch batch) {
        super(batch);
    }

    @Override
    public void submit() {
        int stride = VertexFormat.RECTANGLE.getFloats();
        float size = radius * 2;
        float left = absoluteX() - radius, top = absoluteY() - radius;
        float absOpacity = absoluteOpacity();
        float[] data = new float[6 * stride];

        putVertex(data, stride * 0, left, top, 0, 0, absOpacity);
        putVertex(data, stride * 1, left + size, top, size, 0, absOpacity);
        putVertex(data, stride * 2, left + size, top + size, size, size, absOpacity);

        putVertex(data, stride * 3, left, top, 0, 0, absOpacity);
        putVertex(data, stride * 4, left + size, top + size, size, size, absOpacity);
        putVertex(data, stride * 5, left, top + size, 0, size, absOpacity);

        batch.addShape(absoluteZ(), data, 0);
    }

    private void putVertex(float[] data, int offset, float vx, float vy, float u, float v, float absOpacity) {
        float size = radius * 2;
        data[offset] = vx;
        data[offset + 1] = vy;
        data[offset + 2] = size;
        data[offset + 3] = size;
        data[offset + 4] = color.getRed() / 255f;
        data[offset + 5] = color.getGreen() / 255f;
        data[offset + 6] = color.getBlue() / 255f;
        data[offset + 7] = color.getAlpha() / 255f * absOpacity;
        data[offset + 8] = radius;
        data[offset + 9] = radius;
        data[offset + 10] = radius;
        data[offset + 11] = radius;
        data[offset + 12] = 0;
        data[offset + 13] = u;
        data[offset + 14] = v;
    }

    /** Sets the center position in pixels. */
    public ShapeCircle pos(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /** Sets the center position in pixels with z-order. */
    public ShapeCircle pos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /** Sets the radius in pixels. */
    public ShapeCircle radius(float radius) {
        this.radius = radius;
        return this;
    }

    /** Sets the color. */
    public ShapeCircle color(Color color) {
        this.color = color;
        return this;
    }
}
