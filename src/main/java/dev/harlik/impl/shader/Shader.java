package dev.harlik.impl.shader;

import dev.harlik.SpellRenderer;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import static org.lwjgl.opengl.GL33.*;

public class Shader {

    @Getter
    private final int id;
    private final Map<String, Integer> cache = new HashMap<>();

    public Shader(String vert, String frag) {
        int vertex = ShaderManager.INSTANCE.readResources(vert, ShaderType.VERTEX);
        int fragment = ShaderManager.INSTANCE.readResources(frag, ShaderType.FRAGMENT);

        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) glDeleteShader(vertex);
            if (fragment != 0) glDeleteShader(fragment);
            this.id = 0;
            return;
        }

        int program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glLinkProgram(program);
        glDetachShader(program, vertex);
        glDetachShader(program, fragment);
        glDeleteShader(vertex);
        glDeleteShader(fragment);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            SpellRenderer.LOGGER.error("Could not link shader. {},{}", frag, glGetProgramInfoLog(program));
            glDeleteProgram(program);
            this.id = 0;
            return;
        }
        this.id = program;
    }

    private int getUniformLocation(String name) {
        return cache.computeIfAbsent(name, n -> glGetUniformLocation(id, name));
    }

    public void setUniform1i(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }

    public void setUniform2f(String name, float x, float y) {
        glUniform2f(getUniformLocation(name), x, y);
    }

    public void setUniform4f(String name, float x, float y, float z, float w) {
        glUniform4f(getUniformLocation(name), x, y, z, w);
    }

    public void setUniformMat4(String name, float[] matrix) {
        glUniformMatrix4fv(getUniformLocation(name), false, matrix);
    }

    public void bind() {
        glUseProgram(id);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void destroy() {
        glDeleteProgram(id);
    }
}
