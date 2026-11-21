package dev.harlik.impl.batch;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.harlik.impl.gpu.FrameUniforms;
import dev.harlik.impl.gpu.SpellFrame;
import dev.harlik.impl.gpu.SpellPipelines;
import java.util.OptionalInt;
import lombok.Getter;

/**
 * Copy of the framebuffer behind the glass, with a dual Kawase downsample chain stored in its mip levels.
 * Level 0 is the sharp background, every next level is half the size and roughly twice as blurred.
 */
public class BlurPyramid {

    public static final BlurPyramid INSTANCE = new BlurPyramid();

    private static final int MAX_LEVELS = 6;

    /** Highest mip level in the pyramid. */
    @Getter
    private int levels = 0;
    private int width = 0;
    private int height = 0;
    private GpuTexture texture;
    /** All levels, sampled by the glass shader with textureLod. */
    @Getter
    private GpuTextureView fullView;
    /** One view per level, each is the render target of its downsample and the source of the next one. */
    private GpuTextureView[] levelViews = new GpuTextureView[0];

    /** (Re)allocates the pyramid for a w x h framebuffer. Must be called while no render pass is open. */
    public void ensure(int w, int h) {
        if (texture != null && w == width && h == height) return;
        release();

        width = w;
        height = h;
        levels = Math.min(MAX_LEVELS, 31 - Integer.numberOfLeadingZeros(Math.max(1, Math.min(w, h))));

        GpuDevice device = RenderSystem.getDevice();
        texture = device.createTexture(() -> "spell-renderer blur pyramid",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8, w, h, 1, levels + 1);
        fullView = device.createTextureView(texture, 0, levels + 1);
        levelViews = new GpuTextureView[levels + 1];
        for (int i = 0; i <= levels; i++) {
            levelViews[i] = device.createTextureView(texture, i, 1);
        }
    }

    /** Captures the frame's target into level 0 and builds the pyramid. Must be called while no render pass is open. */
    public void build(SpellFrame frame, FrameUniforms uniforms) {
        CommandEncoder encoder = frame.getEncoder();
        encoder.copyTextureToTexture(frame.getTarget().getColorTexture(), texture, 0, 0, 0, 0, 0, width, height);

        for (int i = 1; i <= levels; i++) {
            int level = i;
            // Only the source level is bound for reading, so writing to level i is not a feedback loop
            try (RenderPass pass = encoder.createRenderPass(() -> "spell-renderer kawase " + level, levelViews[i], OptionalInt.empty())) {
                pass.setPipeline(SpellPipelines.KAWASE_DOWN);
                pass.bindTexture(SpellPipelines.SOURCE_SAMPLER, levelViews[i - 1], RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setUniform(SpellPipelines.KAWASE_BLOCK, uniforms.kawase()[i]);
                pass.draw(0, 3);
            }
        }
    }

    public static int levelSize(int size, int level) {
        return Math.max(1, size >> level);
    }

    private void release() {
        for (GpuTextureView view : levelViews) view.close();
        levelViews = new GpuTextureView[0];
        if (fullView != null) {
            fullView.close();
            fullView = null;
        }
        if (texture != null) {
            texture.close();
            texture = null;
        }
        levels = 0;
        width = 0;
        height = 0;
    }

    public void destroy() {
        release();
    }
}
