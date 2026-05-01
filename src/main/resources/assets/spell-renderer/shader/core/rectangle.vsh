#version 330 core

layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aSize;
layout(location = 2) in vec4 aColor;
layout(location = 3) in vec4 aRadius;
layout(location = 4) in float aSmooth;
layout(location = 5) in vec2 aUV;

uniform mat4 uProjection;

out vec2 vSize;
out vec4 vColor;
out vec4 vRadius;
out float vSmooth;
out vec2 vUV;

void main() {
    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
    vColor = aColor;
    vSize = aSize;
    vRadius = aRadius;
    vSmooth = aSmooth;
    vUV = aUV;
}