#version 330 core

in vec2 vUV;
in vec4 vColor;
in float vPxRange;

uniform sampler2D uAtlas;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec3 s = texture(uAtlas, vUV).rgb;
    float d = median(s.r, s.g, s.b);
    float screenPxDistance = vPxRange * (d - 0.5);
    float alpha = clamp(screenPxDistance + 0.5, 0.0, 1.0);
    if (alpha <= 0.0) discard;
    fragColor = vec4(vColor.rgb, vColor.a * alpha);
}