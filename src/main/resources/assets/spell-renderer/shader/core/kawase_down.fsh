#version 330 core

// Dual Kawase downsample: renders the source level into a level half its size.

in vec2 vUV;

uniform sampler2D uSource;
uniform vec2 uTexel; // 1 / source level size

out vec4 fragColor;

void main() {
    vec4 sum = texture(uSource, vUV) * 4.0;
    sum += texture(uSource, vUV - uTexel);
    sum += texture(uSource, vUV + uTexel);
    sum += texture(uSource, vUV + vec2(uTexel.x, -uTexel.y));
    sum += texture(uSource, vUV - vec2(uTexel.x, -uTexel.y));
    fragColor = sum / 8.0;
}
