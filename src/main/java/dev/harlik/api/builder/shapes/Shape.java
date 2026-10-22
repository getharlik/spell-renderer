package dev.harlik.api.builder.shapes;

import dev.harlik.api.scissors.Region;
import dev.harlik.impl.batch.RenderBatch;
import dev.harlik.impl.drag.DragManager;

public abstract class Shape<T extends Shape<T>> {

    protected final RenderBatch batch;
    protected Shape<?> parent;
    protected String dragId;
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

    /** Returns the x position relative to the screen, accounting for all parent offsets and drag offset. */
    public float absoluteX() {
        float base = x + (parent != null ? parent.absoluteX() : 0);
        return dragId != null ? base + DragManager.INSTANCE.offsetX(dragId) : base;
    }

    /** Returns the y position relative to the screen, accounting for all parent offsets and drag offset. */
    public float absoluteY() {
        float base = y + (parent != null ? parent.absoluteY() : 0);
        return dragId != null ? base + DragManager.INSTANCE.offsetY(dragId) : base;
    }

    /** Returns the z-order, summed through the parent chain. */
    public float absoluteZ() { return z + (parent != null ? parent.absoluteZ() : 0); }

    /** Returns the opacity, multiplied through the parent chain. */
    public float absoluteOpacity() { return opacity * (parent != null ? parent.absoluteOpacity() : 1.0f); }

    /** Returns the width of the shape's bounding box. */
    public abstract float width();

    /** Returns the height of the shape's bounding box. */
    public abstract float height();

    /** Returns the bounding box in absolute screen space. */
    public Region bounds() { return new Region(absoluteX(), absoluteY(), width(), height()); }

    /** Sets the parent shape. Position, z-order, and opacity become relative to it. */
    @SuppressWarnings("unchecked")
    public T parent(Shape<?> parent) {
        this.parent = parent;
        return (T) this;
    }

    /** Makes the shape (and everything parented to it) draggable with the left mouse button.
     *  The key identifies the drag state across frames, see {@link DragManager}. */
    @SuppressWarnings("unchecked")
    public T draggable(String key) {
        dragId = key;
        return (T) this;
    }

    /** Submits the shape to the batch */
    public final void submit() {
        registerDrag();
        draw();
    }

    /** Draws the shape into the batch */
    protected abstract void draw();

    /** Returns the drag id of this shape or its nearest draggable ancestor, or null. */
    protected String resolveDragId() {
        for (Shape<?> s = this; s != null; s = s.parent) {
            if (s.dragId != null) return s.dragId;
        }
        return null;
    }

    private void registerDrag() {
        String id = resolveDragId();
        if (id == null || width() <= 0 || height() <= 0) return;
        DragManager.INSTANCE.register(id, bounds(), absoluteZ(), dragId != null);
    }
}
