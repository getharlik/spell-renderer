package dev.harlik.impl.batch;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.harlik.impl.shader.Shader;
import dev.harlik.impl.shader.ShaderManager;
import java.nio.ByteBuffer;
import lombok.Getter;
import static org.lwjgl.opengl.GL33.*;

/**
 * Copy of the framebuffer behind the glass, with a dual Kawase downsample chain stored in its mip levels.
 * Level 0 is the sharp background, every next level is half the size and roughly twice as blurred.
 */
public class BlurPyramid {

    public static final BlurPyramid INSTANCE = new BlurPyramid();

    private static final int MAX_LEVELS = 6;

    @Getter
    private int textureId = 0;
    /** Highest mip level in the pyramid. */
    @Getter
    private int levels = 0;
    private int width = 0;
    private int height = 0;
    private int[] fbos = new int[0];
    private int vao = 0;

    /** Captures the current framebuffer and builds the pyramid. Leaves the texture bound on the active texture unit. */
    public void build(int w, int h) {
        if (textureId == 0 || w != width || h != height) allocate(w, h);

        glBindTexture(GL_TEXTURE_2D, textureId);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

        if (levels > 0) downsample();
    }

    private void downsample() {
        Shader shader = ShaderManager.INSTANCE.getShader("kawase_down");

        int prevDrawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int prevProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int[] prevViewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, prevViewport);
        boolean scissor = glIsEnabled(GL_SCISSOR_TEST);
        boolean blend = glIsEnabled(GL_BLEND);

        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        shader.bind();
        shader.setUniform1i("uSource", glGetInteger(GL_ACTIVE_TEXTURE) - GL_TEXTURE0);
        glBindVertexArray(vao);

        for (int i = 1; i <= levels; i++) {
            // Only the source level is readable, so writing to level i is not a feedback loop
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, i - 1);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, i - 1);
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, fbos[i - 1]);
            GlStateManager._viewport(0, 0, levelSize(width, i), levelSize(height, i));
            shader.setUniform2f("uTexel", 1f / levelSize(width, i - 1), 1f / levelSize(height, i - 1));
            glDrawArrays(GL_TRIANGLES, 0, 3);
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels);

        glBindVertexArray(prevVao);
        glUseProgram(prevProgram);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDrawFbo);
        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFbo);
        GlStateManager._viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        if (scissor) glEnable(GL_SCISSOR_TEST);
        if (blend) glEnable(GL_BLEND);
    }

    private void allocate(int w, int h) {
        releaseTexture();
        width = w;
        height = h;
        levels = Math.min(MAX_LEVELS, 31 - Integer.numberOfLeadingZeros(Math.max(1, Math.min(w, h))));

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        for (int i = 0; i <= levels; i++) {
            glTexImage2D(GL_TEXTURE_2D, i, GL_RGBA8, levelSize(w, i), levelSize(h, i), 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels);

        int prevDrawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        fbos = new int[levels];
        for (int i = 1; i <= levels; i++) {
            fbos[i - 1] = glGenFramebuffers();
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, fbos[i - 1]);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, i);
        }
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDrawFbo);
        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFbo);

        if (vao == 0) vao = glGenVertexArrays();
    }

    private static int levelSize(int size, int level) {
        return Math.max(1, size >> level);
    }

    private void releaseTexture() {
        for (int fbo : fbos) glDeleteFramebuffers(fbo);
        fbos = new int[0];
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
        levels = 0;
        width = 0;
        height = 0;
    }

    public void destroy() {
        releaseTexture();
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            vao = 0;
        }
    }
}
