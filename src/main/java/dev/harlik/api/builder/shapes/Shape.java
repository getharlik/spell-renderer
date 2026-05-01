package dev.harlik.api.builder.shapes;

import dev.harlik.impl.batch.RenderBatch;

public abstract class Shape<T extends Shape<T>> {

    protected final RenderBatch batch;
    protected Shape<?> parent;
    protected float x, y, z;
    protected float opacity = 1.0f;

    public Shape(RenderBatch batch) {
        this.batch = batch;
    }

    /** Sets the opacity of the shape and children. */
    @SuppressWarnings("unchecked")
    public T opacity(float opacity) {
        this.opacity = opacity;
        return (T) this;
    }

    /** Returns the x position relative to the screen, accounting for all parent offsets. */
    public float absoluteX() { return x + (parent != null ? parent.absoluteX() : 0); }

    /** Returns the y position relative to the screen, accounting for all parent offsets. */
    public float absoluteY() { return y + (parent != null ? parent.absoluteY() : 0); }

    /** Returns the z-order, summed through the parent chain. */
    public float absoluteZ() { return z + (parent != null ? parent.absoluteZ() : 0); }

    /** Returns the opacity, multiplied through the parent chain. */
    public float absoluteOpacity() { return opacity * (parent != null ? parent.absoluteOpacity() : 1.0f); }

    /** Sets the parent shape. Position, z-order, and opacity become relative to it. */
    @SuppressWarnings("unchecked")
    public T parent(Shape<?> parent) {
        this.parent = parent;
        return (T) this;
    }

    /** Submits the shape to the batch */
    public abstract void submit();
}
