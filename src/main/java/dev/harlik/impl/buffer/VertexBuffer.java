package dev.harlik.impl.buffer;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL33.glGenBuffers;

public class VertexBuffer {

    private final int vao;
    private final int vbo;

    public VertexBuffer() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
    }

    public void setup(FloatBuffer buffer, VertexFormat format) {
        int previousVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);

        int stride = format.getStride();
        int offset = 0;

        int i = 0;
        for (int size : format.attributes().values()) {
            glEnableVertexAttribArray(i);
            glVertexAttribPointer(i, size, GL_FLOAT, false, stride, offset);
            offset += size * Float.BYTES;
            i++;
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(previousVao);
    }

    public void draw(int drawMode, int offset, int vertices) {
        int previousVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        glBindVertexArray(vao);
        glDrawArrays(drawMode, offset, vertices);
        glBindVertexArray(previousVao);
    }

    public void destroy() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
}
