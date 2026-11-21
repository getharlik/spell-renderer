package dev.harlik.impl.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.harlik.api.RenderContext;
import dev.harlik.api.scale.Scale;
import dev.harlik.api.scissors.Region;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;
import lombok.Getter;
import net.minecraft.client.Minecraft;

/**
 * One render of a {@link RenderContext} into Minecraft's main render target.
 * Owns the command encoder and the open render pass, which has to be closed before any upload or texture copy.
 * Vanilla submits the encoder after it blits the main target to the window, so this never submits.
 */
public final class SpellFrame implements AutoCloseable {

    private record Buffers(DynamicBuffer vertices, DynamicBuffer uniforms) {}

    /** Separate buffers per context, so the HUD and GUI renders of the same frame never wait on each other. */
    private static final Map<RenderContext, Buffers> BUFFERS = new EnumMap<>(RenderContext.class);

    private final RenderContext context;
    private final Buffers buffers;
    @Getter
    private final CommandEncoder encoder;
    @Getter
    private final RenderTarget target;
    @Getter
    private final int width, height;
    private RenderPass pass;

    private SpellFrame(RenderContext context) {
        this.context = context;
        this.buffers = BUFFERS.computeIfAbsent(context, c -> new Buffers(
                new DynamicBuffer("spell-renderer " + c + " vertices", GpuBuffer.USAGE_VERTEX),
                new DynamicBuffer("spell-renderer " + c + " uniforms", GpuBuffer.USAGE_UNIFORM)));
        this.encoder = RenderSystem.getDevice().createCommandEncoder();
        this.target = Minecraft.getInstance().getMainRenderTarget();
        this.width = target.getColorTexture().getWidth(0);
        this.height = target.getColorTexture().getHeight(0);
    }

    public static SpellFrame begin(RenderContext context) {
        return new SpellFrame(context);
    }

    public GpuBuffer uploadVertices(ByteBuffer data) {
        assertNoPass();
        return buffers.vertices().write(data);
    }

    public GpuBuffer uploadUniforms(ByteBuffer data) {
        assertNoPass();
        return buffers.uniforms().write(data);
    }

    /** Opens a pass on the main target. Keeps what is already drawn there and starts with the scissor disabled. */
    public RenderPass beginPass() {
        endPass();
        pass = encoder.createRenderPass(() -> "spell-renderer " + context, target.getColorTextureView(), OptionalInt.empty());
        return pass;
    }

    public void endPass() {
        if (pass != null) {
            pass.close();
            pass = null;
        }
    }

    public RenderPass pass() {
        if (pass == null) throw new IllegalStateException("No render pass is open");
        return pass;
    }

    /**
     * Clips the next draws to the region, given in scaled units like the shapes.
     * Returns false when nothing of the region is on screen, so the draws can be skipped.
     */
    public boolean scissors(Region region) {
        RenderPass pass = pass();
        if (region == null) {
            pass.disableScissor();
            return true;
        }

        float scale = width / Scale.INSTANCE.getViewportWidth();
        int sx       = clamp((int) Math.floor(region.x() * scale), width);
        int sxMax    = clamp((int) Math.ceil((region.x() + region.w()) * scale), width);
        int syTopMin = clamp((int) Math.floor(region.y() * scale), height);
        int syTopMax = clamp((int) Math.ceil((region.y() + region.h()) * scale), height);
        int sw = sxMax - sx;
        int sh = syTopMax - syTopMin;
        if (sw <= 0 || sh <= 0) return false;

        // Scissor origin is bottom-left, shapes are top-left
        pass.enableScissor(sx, height - syTopMax, sw, sh);
        return true;
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    private void assertNoPass() {
        if (pass != null) throw new IllegalStateException("Close the render pass before uploading");
    }

    @Override
    public void close() {
        endPass();
        buffers.vertices().rotate();
        buffers.uniforms().rotate();
    }
}
