package dev.harlik.impl.batch;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.harlik.api.scissors.Region;
import dev.harlik.api.scissors.Scissors;
import dev.harlik.impl.buffer.VertexFormat;
import dev.harlik.impl.gpu.FrameUniforms;
import dev.harlik.impl.gpu.SpellPipelines;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.Getter;

public class RenderBatch {

    record Entry(float z, float[] data, int textureId, Region scissors) {}

    private final RenderPipeline pipeline;
    /** Sampler the entry textures are bound to, or null when the batch is untextured. */
    private final String sampler;

    @Getter
    private final List<Entry> entries = new ArrayList<>();
    private final int floatsPerVertex;
    private BiConsumer<RenderPass, FrameUniforms> bindings;

    public RenderBatch(RenderPipeline pipeline, VertexFormat format, String sampler) {
        this.pipeline = pipeline;
        this.sampler = sampler;
        this.floatsPerVertex = format.getFloats();
    }

    public void addShape(float z, float[] vertexData, int textureId) {
        entries.add(new Entry(z, vertexData, textureId, Scissors.current()));
    }

    public int getStride() {
        return floatsPerVertex * Float.BYTES;
    }

    /** Extra uniforms and textures the pipeline needs besides the projection. */
    public RenderBatch bindings(BiConsumer<RenderPass, FrameUniforms> bindings) {
        this.bindings = bindings;
        return this;
    }

    public void draw(RenderPass pass, GpuBuffer vertices, int firstVertex, List<Entry> runEntries, FrameUniforms uniforms) {
        if (runEntries.isEmpty()) return;

        pass.setPipeline(pipeline);
        pass.setUniform(SpellPipelines.PROJECTION_BLOCK, uniforms.projection());
        if (bindings != null) bindings.accept(pass, uniforms);
        pass.setVertexBuffer(0, vertices);

        int currentTexture = runEntries.getFirst().textureId();
        int vertexOffset = firstVertex;
        int runStartVertex = firstVertex;

        for (Entry entry : runEntries) {
            if (entry.textureId() != currentTexture) {
                drawRun(pass, runStartVertex, vertexOffset - runStartVertex, currentTexture);
                currentTexture = entry.textureId();
                runStartVertex = vertexOffset;
            }
            vertexOffset += entry.data().length / floatsPerVertex;
        }
        drawRun(pass, runStartVertex, vertexOffset - runStartVertex, currentTexture);
    }

    private void drawRun(RenderPass pass, int firstVertex, int vertexCount, int textureId) {
        if (sampler != null) {
            GpuTextureView view = TextureCache.INSTANCE.view(textureId);
            if (view == null) return; // texture failed to load
            pass.bindTexture(sampler, view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        }
        pass.draw(firstVertex, vertexCount);
    }

    public void clear() {
        entries.clear();
    }
}
