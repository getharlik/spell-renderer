package dev.harlik.impl.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.MappableRingBuffer;

/**
 * Per-frame GPU data (vertices, uniform blocks). Backed by Mojang's {@link MappableRingBuffer}: three buffers used
 * in turn, each fenced so we never overwrite data the GPU is still reading. Grows when a frame needs more space.
 */
public final class DynamicBuffer implements AutoCloseable {

    private static final int MIN_CAPACITY = 16 * 1024;

    private final String label;
    private final int usage;
    private MappableRingBuffer ring;
    private boolean written;

    public DynamicBuffer(String label, int usage) {
        this.label = label;
        this.usage = usage | GpuBuffer.USAGE_MAP_WRITE;
    }

    /** Copies the remaining bytes of data into the current buffer. Must be called while no render pass is open. */
    public GpuBuffer write(ByteBuffer data) {
        int size = data.remaining();
        if (ring == null || ring.size() < size) {
            int capacity = Math.max(MIN_CAPACITY, ring == null ? 0 : ring.size());
            while (capacity < size) capacity *= 2;
            if (ring != null) ring.close();
            ring = new MappableRingBuffer(() -> label, usage, capacity);
        }

        GpuBuffer buffer = ring.currentBuffer();
        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(buffer.slice(0, size), false, true)) {
            view.data().put(data);
        }
        written = true;
        return buffer;
    }

    /** Moves on to the next buffer. Call once every draw reading the current one has been recorded. */
    public void rotate() {
        if (!written) return;
        ring.rotate();
        written = false;
    }

    @Override
    public void close() {
        if (ring != null) {
            ring.close();
            ring = null;
        }
    }
}
