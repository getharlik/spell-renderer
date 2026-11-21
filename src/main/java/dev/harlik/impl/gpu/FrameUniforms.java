package dev.harlik.impl.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.harlik.api.scale.Scale;
import dev.harlik.impl.batch.BlurPyramid;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

/**
 * The std140 uniform blocks of one context render, uploaded together in one buffer.
 * Each block sits in its own slot, aligned to what the device requires for uniform buffer offsets.
 *
 * @param glass  {@code SpellGlass}, null when no glass is drawn
 * @param kawase {@code SpellKawase} per pyramid level, index i is the downsample into level i; empty when no glass is drawn
 */
public record FrameUniforms(GpuBufferSlice projection, GpuBufferSlice glass, GpuBufferSlice[] kawase) {

    /** Largest block, the projection matrix. */
    private static final int SLOT_SIZE = 64;
    private static final int SMALL_BLOCK_SIZE = 16;

    private static ByteBuffer scratch;

    /** Writes and uploads the blocks. Must be called while no render pass is open. */
    public static FrameUniforms upload(SpellFrame frame, float[] projection, boolean withGlass) {
        int align = RenderSystem.getDevice().getUniformOffsetAlignment();
        int stride = Mth.roundToward(SLOT_SIZE, align);
        int levels = withGlass ? BlurPyramid.INSTANCE.getLevels() : 0;
        int slots = withGlass ? 2 + levels : 1;

        ByteBuffer data = scratch(stride * slots);

        data.position(0);
        Std140Builder.intoBuffer(data).putMat4f(new Matrix4f().set(projection));

        if (withGlass) {
            int w = frame.getWidth(), h = frame.getHeight();
            data.position(stride);
            Std140Builder.intoBuffer(data)
                    .putVec2(w, h)
                    .putFloat(w / Scale.INSTANCE.getViewportWidth())
                    .putFloat(levels);

            for (int i = 1; i <= levels; i++) {
                data.position(stride * (1 + i));
                Std140Builder.intoBuffer(data)
                        .putVec2(1f / BlurPyramid.levelSize(w, i - 1), 1f / BlurPyramid.levelSize(h, i - 1));
            }
        }

        data.position(0).limit(stride * slots);
        GpuBuffer buffer = frame.uploadUniforms(data);

        GpuBufferSlice glass = null;
        GpuBufferSlice[] kawase = new GpuBufferSlice[levels + 1];
        if (withGlass) {
            glass = buffer.slice(stride, SMALL_BLOCK_SIZE);
            for (int i = 1; i <= levels; i++) {
                kawase[i] = buffer.slice((long) stride * (1 + i), SMALL_BLOCK_SIZE);
            }
        }
        return new FrameUniforms(buffer.slice(0, SLOT_SIZE), glass, kawase);
    }

    private static ByteBuffer scratch(int size) {
        if (scratch == null || scratch.capacity() < size) {
            if (scratch != null) MemoryUtil.memFree(scratch);
            scratch = MemoryUtil.memCalloc(Math.max(size, 4096));
        }
        scratch.clear();
        return scratch;
    }
}
