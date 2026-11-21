#version 330

layout(std140) uniform SpellProjection {
    mat4 uProjection;
};

in vec2 aPos;
in vec2 aSize;
in vec4 aColor;
in vec4 aRadius;
in float aSmooth;
in vec2 aUV;
in vec4 aGlass;
in float aHighlight;

out vec2 vSize;
out vec4 vColor;
out vec4 vRadius;
out float vSmooth;
out vec2 vUV;
out vec4 vGlass;
out float vHighlight;
out vec2 vScreenUV;

void main() {
    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
    vColor = aColor;
    vSize = aSize;
    vRadius = aRadius;
    vSmooth = aSmooth;
    vUV = aUV;
    vGlass = aGlass;
    vHighlight = aHighlight;
    vScreenUV = gl_Position.xy * 0.5 + 0.5;
}
