package dev.harlik.impl.batch;

import dev.harlik.api.scissors.Region;
import dev.harlik.api.scissors.Scissors;
import dev.harlik.impl.buffer.VertexBuffer;
import dev.harlik.impl.buffer.VertexFormat;
import dev.harlik.impl.shader.Shader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL33.*;

public class RenderBatch {

    record Entry(float z, float[] data, int textureId, Region scissors) {}

    private Shader shader;
    private VertexFormat format;
    private VertexBuffer buffer;

    @Getter
    private List<Entry> entries = new ArrayList<>();
    private int drawMode;
    private final int floatsPerVertex;
    private Runnable preDraw;
    private Runnable postDraw;
    private FloatBuffer floatBuffer;

    public RenderBatch(Shader shader, VertexFormat format, int drawMode) {
        this.shader = shader;
        this.format = format;
        this.drawMode = drawMode;
        this.buffer = new VertexBuffer();
        this.floatsPerVertex = format.getFloats();
    }

    public void addShape(float z, float[] vertexData, int textureId) {
        entries.add(new Entry(z, vertexData, textureId, Scissors.current()));
    }

    public RenderBatch preDraw(Runnable preDraw) {
        this.preDraw = preDraw;
        return this;
    }

    public RenderBatch postDraw(Runnable postDraw) {
        this.postDraw = postDraw;
        return this;
    }

    public void flush(float[] projection, List<Entry> runEntries) {
        if (runEntries.isEmpty()) return;

        int runFloats = 0;
        for (Entry e : runEntries) runFloats += e.data().length;

        if (floatBuffer == null || floatBuffer.capacity() < runFloats) {
            int newCapacity = floatBuffer == null ? runFloats : floatBuffer.capacity();
            while (newCapacity < runFloats) newCapacity *= 2;
            if (floatBuffer != null) MemoryUtil.memFree(floatBuffer);
            floatBuffer = MemoryUtil.memAllocFloat(newCapacity);
        }
        floatBuffer.clear();

        for (Entry entry : runEntries) {
            floatBuffer.put(entry.data());
        }
        floatBuffer.flip();

        buffer.setup(floatBuffer, format);
        shader.bind();
        shader.setUniformMat4("uProjection", projection);
        shader.setUniform1i("uTexture", 0);
        if (preDraw != null) preDraw.run();

        int currentTexture = -1;
        int vertexOffset = 0;
        int runStartVertex = 0;

        for (int i = 0; i <= runEntries.size(); i++) {
            int tex = (i < runEntries.size()) ? runEntries.get(i).textureId() : -2;
            if (tex != currentTexture) {
                if (i > 0) {
                    drawRun(runStartVertex, vertexOffset - runStartVertex, currentTexture);
                }
                currentTexture = tex;
                runStartVertex = vertexOffset;
            }
            if (i < runEntries.size()) {
                vertexOffset += runEntries.get(i).data().length / floatsPerVertex;
            }
        }

        shader.unbind();
        if (postDraw != null) postDraw.run();
    }

    private void drawRun(int vertexOffset, int vertexCount, int textureId) {
        if (textureId > 0) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
        buffer.draw(drawMode, vertexOffset, vertexCount);
    }

    public void clear() {
        entries.clear();
    }

    public void destroy() {
        buffer.destroy();
        if (floatBuffer != null) {
            MemoryUtil.memFree(floatBuffer);
            floatBuffer = null;
        }
    }
}
