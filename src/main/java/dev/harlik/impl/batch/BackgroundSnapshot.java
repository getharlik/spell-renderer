package dev.harlik.impl.batch;

import java.nio.ByteBuffer;
import lombok.Getter;
import static org.lwjgl.opengl.GL33.*;

public class BackgroundSnapshot {

    public static final BackgroundSnapshot INSTANCE = new BackgroundSnapshot();

    @Getter
    private int textureId = 0;
    @Getter
    private int width = 0;
    @Getter
    private int height = 0;

    public void capture(int w, int h) {
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);

        if (textureId == 0) {
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }

        if (w != width || h != height) {
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            width = w;
            height = h;
        }

        glBindTexture(GL_TEXTURE_2D, textureId);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        glBindTexture(GL_TEXTURE_2D, previousTexture);
    }

    public void destroy() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
            width = 0;
            height = 0;
        }
    }
}
