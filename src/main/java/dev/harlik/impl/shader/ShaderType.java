package dev.harlik.impl.shader;

import static org.lwjgl.opengl.GL33.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL33.GL_VERTEX_SHADER;

public enum ShaderType {
    VERTEX(GL_VERTEX_SHADER, ".vsh"),
    FRAGMENT(GL_FRAGMENT_SHADER, ".fsh");

    public final int glType;
    public final String fileExtension;

    ShaderType(int glType, String fileExtension) {
        this.glType = glType;
        this.fileExtension = fileExtension;
    }
}
