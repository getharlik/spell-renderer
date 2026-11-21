package dev.harlik.impl.batch;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.harlik.api.scissors.Region;
import dev.harlik.impl.buffer.VertexFormat;
import dev.harlik.impl.gpu.FrameUniforms;
import dev.harlik.impl.gpu.SpellFrame;
import dev.harlik.impl.gpu.SpellPipelines;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.lwjgl.system.MemoryUtil;

public class BatchManager {

    public static final BatchManager INSTANCE = new BatchManager();

    @Getter
    private final RenderBatch rectangle = new RenderBatch(SpellPipelines.RECTANGLE, VertexFormat.RECTANGLE, null);
    @Getter
    private final RenderBatch image = new RenderBatch(SpellPipelines.IMAGE, VertexFormat.RECTANGLE, SpellPipelines.TEXTURE_SAMPLER);
    @Getter
    private final RenderBatch text = new RenderBatch(SpellPipelines.TEXT, VertexFormat.TEXT, SpellPipelines.ATLAS_SAMPLER);
    @Getter
    private final RenderBatch glass = new RenderBatch(SpellPipelines.GLASS, VertexFormat.GLASS, null)
            .bindings((pass, uniforms) -> {
                pass.bindTexture(SpellPipelines.BACKGROUND_SAMPLER, BlurPyramid.INSTANCE.getFullView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true));
                pass.setUniform(SpellPipelines.GLASS_BLOCK, uniforms.glass());
            });

    private ByteBuffer vertexData;

    public void flushBatches(SpellFrame frame, float[] projection) {
        try {
            List<Run> runs = collectRuns();
            if (runs.isEmpty()) return;

            // Everything is uploaded up front, uploads are not allowed while a render pass is open
            long[] offsets = new long[runs.size()];
            GpuBuffer vertices = frame.uploadVertices(packVertices(runs, offsets));

            boolean withGlass = runs.stream().anyMatch(run -> run.batch() == glass);
            if (withGlass) BlurPyramid.INSTANCE.ensure(frame.getWidth(), frame.getHeight());
            FrameUniforms uniforms = FrameUniforms.upload(frame, projection, withGlass);

            frame.beginPass();
            for (int i = 0; i < runs.size(); i++) {
                Run run = runs.get(i);
                if (run.batch() == glass) {
                    // The glass samples everything drawn so far, so the pass is split to capture it
                    frame.endPass();
                    BlurPyramid.INSTANCE.build(frame, uniforms);
                    frame.beginPass();
                }
                if (!frame.scissors(run.scissors())) continue;
                int firstVertex = (int) (offsets[i] / run.batch().getStride());
                run.batch().draw(frame.pass(), vertices, firstVertex, run.entries(), uniforms);
            }
            frame.endPass();
        } finally {
            rectangle.clear();
            image.clear();
            text.clear();
            glass.clear();
        }
    }

    /** Sorts every entry by z and splits them into runs that share a batch and scissors. */
    private List<Run> collectRuns() {
        List<EntryRef> all = new ArrayList<>();
        for (RenderBatch.Entry e : rectangle.getEntries()) all.add(new EntryRef(rectangle, e));
        for (RenderBatch.Entry e : image.getEntries())     all.add(new EntryRef(image, e));
        for (RenderBatch.Entry e : text.getEntries())      all.add(new EntryRef(text, e));
        for (RenderBatch.Entry e : glass.getEntries())     all.add(new EntryRef(glass, e));

        all.sort(Comparator.comparingDouble(ref -> ref.entry().z()));

        List<Run> runs = new ArrayList<>();
        Run current = null;
        for (EntryRef ref : all) {
            Region entryScissors = ref.entry().scissors();
            if (current == null || ref.batch() != current.batch() || !Objects.equals(entryScissors, current.scissors())) {
                current = new Run(ref.batch(), entryScissors, new ArrayList<>());
                runs.add(current);
            }
            current.entries().add(ref.entry());
        }
        return runs;
    }

    private ByteBuffer packVertices(List<Run> runs, long[] offsets) {
        int total = 0;
        for (int i = 0; i < runs.size(); i++) {
            int floats = 0;
            for (RenderBatch.Entry e : runs.get(i).entries()) floats += e.data().length;
            total = alignUp(total, runs.get(i).batch().getStride());
            offsets[i] = total;
            total += floats * Float.BYTES;
        }

        if (vertexData == null || vertexData.capacity() < total) {
            int capacity = vertexData == null ? Math.max(total, 64 * 1024) : vertexData.capacity();
            while (capacity < total) capacity *= 2;
            if (vertexData != null) MemoryUtil.memFree(vertexData);
            vertexData = MemoryUtil.memAlloc(capacity);
        }
        vertexData.clear();

        for (int i = 0; i < runs.size(); i++) {
            FloatBuffer floats = vertexData.position((int) offsets[i]).asFloatBuffer();
            for (RenderBatch.Entry e : runs.get(i).entries()) floats.put(e.data());
        }
        vertexData.position(0).limit(total);
        return vertexData;
    }

    private static int alignUp(int value, int alignment) {
        return ((value + alignment - 1) / alignment) * alignment;
    }

    public void destroy() {
        BlurPyramid.INSTANCE.destroy();
        if (vertexData != null) {
            MemoryUtil.memFree(vertexData);
            vertexData = null;
        }
    }

    private record EntryRef(RenderBatch batch, RenderBatch.Entry entry) {}

    private record Run(RenderBatch batch, Region scissors, List<RenderBatch.Entry> entries) {}
}
