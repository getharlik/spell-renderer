#version 330 core

// Fullscreen triangle generated from gl_VertexID, draw with 3 vertices and no attributes.

out vec2 vUV;

void main() {
    vec2 pos = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    vUV = pos;
    gl_Position = vec4(pos * 2.0 - 1.0, 0.0, 1.0);
}
