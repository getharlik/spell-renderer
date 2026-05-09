package dev.harlik.impl.batch;

import dev.harlik.api.scale.Scale;
import dev.harlik.api.scissors.Region;
import dev.harlik.impl.buffer.VertexFormat;
import dev.harlik.impl.shader.Shader;
import dev.harlik.impl.shader.ShaderManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import static org.lwjgl.opengl.GL33.*;

public class BatchManager {

    public static final BatchManager INSTANCE = new BatchManager();

    @Getter
    private RenderBatch rectangle, image, glass, text;

    public void init() {
        rectangle = new RenderBatch(
                ShaderManager.INSTANCE.getShader("rectangle"),
                VertexFormat.RECTANGLE,
                GL_TRIANGLES
        );
        image = new RenderBatch(
                ShaderManager.INSTANCE.getShader("image"),
                VertexFormat.RECTANGLE,
                GL_TRIANGLES
        );
        glass = new RenderBatch(
                ShaderManager.INSTANCE.getShader("glass"),
                VertexFormat.RECTANGLE,
                GL_TRIANGLES
        );
        text = new RenderBatch(
                ShaderManager.INSTANCE.getShader("text"),
                VertexFormat.TEXT,
                GL_TRIANGLES
        );

        Shader glassShader = ShaderManager.INSTANCE.getShader("glass");
        int[] savedUnit1Texture = new int[1];
        glass.preDraw(() -> {
            var window = Minecraft.getInstance().getWindow();
            int w = window.getWidth();
            int h = window.getHeight();

            BackgroundSnapshot.INSTANCE.capture(w, h);

            glActiveTexture(GL_TEXTURE1);
            savedUnit1Texture[0] = glGetInteger(GL_TEXTURE_BINDING_2D);
            glBindTexture(GL_TEXTURE_2D, BackgroundSnapshot.INSTANCE.getTextureId());
            glActiveTexture(GL_TEXTURE0);

            glassShader.setUniform1i("uBackground", 1);
            glassShader.setUniform2f("uResolution", (float) w, (float) h);
        });
        glass.postDraw(() -> {
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, savedUnit1Texture[0]);
            glActiveTexture(GL_TEXTURE0);
        });
    }

    public void flushBatches(float[] projection) {
        List<EntryRef> all = new ArrayList<>();
        for (RenderBatch.Entry e : rectangle.getEntries()) all.add(new EntryRef(rectangle, e));
        for (RenderBatch.Entry e : image.getEntries())     all.add(new EntryRef(image, e));
        for (RenderBatch.Entry e : text.getEntries())      all.add(new EntryRef(text, e));
        for (RenderBatch.Entry e : glass.getEntries())     all.add(new EntryRef(glass, e));

        all.sort(Comparator.comparingDouble(ref -> ref.entry().z()));

        int[] vp = new int[4];
        glGetIntegerv(GL_VIEWPORT, vp);
        float scale = vp[2] / Scale.INSTANCE.getViewportWidth();

        glDisable(GL_SCISSOR_TEST);

        RenderBatch currentBatch = null;
        Region currentScissors = null;
        List<RenderBatch.Entry> currentRun = new ArrayList<>();

        for (EntryRef ref : all) {
            Region entryScissors = ref.entry.scissors();
            if ((ref.batch() != currentBatch) || !Objects.equals(entryScissors, currentScissors)) {
                if (currentBatch != null && !currentRun.isEmpty()) {
                    currentBatch.flush(projection, currentRun);
                    currentRun.clear();
                }
                currentBatch = ref.batch();
                if (!Objects.equals(entryScissors, currentScissors)) {
                    applyScissors(entryScissors, scale, vp[3]);
                    currentScissors = entryScissors;
                }
            }
            currentRun.add(ref.entry());
        }
        if (currentBatch != null && !currentRun.isEmpty()) {
            currentBatch.flush(projection, currentRun);
        }

        rectangle.clear();
        image.clear();
        text.clear();
        glass.clear();
    }

    private void applyScissors(Region region, float scale, int viewportHeight) {
        if (region == null) {
            glDisable(GL_SCISSOR_TEST);
            return;
        }
        int sx       = (int) Math.floor(region.x() * scale);
        int sxMax    = (int) Math.ceil((region.x() + region.w()) * scale);
        int syTopMin = (int) Math.floor(region.y() * scale);
        int syTopMax = (int) Math.ceil((region.y() + region.h()) * scale);
        int sw       = Math.max(0, sxMax - sx);
        int sh       = Math.max(0, syTopMax - syTopMin);
        int sy       = viewportHeight - syTopMax;

        glEnable(GL_SCISSOR_TEST);
        glScissor(sx, sy, sw, sh);
    }

    public void destroy() {
        rectangle.destroy();
        image.destroy();
        text.destroy();
        glass.destroy();
        BackgroundSnapshot.INSTANCE.destroy();
    }

    private record EntryRef(RenderBatch batch, RenderBatch.Entry entry) {}

}
