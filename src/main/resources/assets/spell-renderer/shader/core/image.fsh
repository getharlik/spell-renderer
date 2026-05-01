#version 330 core

in vec2 vSize;
in vec4 vColor;
in vec4 vRadius;
in float vSmooth;
in vec2 vUV;

uniform sampler2D uTexture;

out vec4 fragColor;

float squircleSDF(vec2 p, vec2 halfSize, float r, float s) {
    float ext = r * (1.0 + s);
    ext = min(ext, min(halfSize.x, halfSize.y));

    float n = 2.0 + s * 6.0;

    vec2 q = abs(p) - halfSize + ext;
    vec2 qMax = max(q, 0.0);

    float outer = pow(pow(qMax.x, n) + pow(qMax.y, n), 1.0 / n);
    float inner = min(max(q.x, q.y), 0.0);
    return outer + inner - ext;
}

void main() {
    vec4 textureColor = texture(uTexture, vUV / vSize);
    vec4 color = textureColor * vColor;

    vec2 halfSize = vSize * 0.5;
    vec2 p = vUV - halfSize;

    float r;
    if (p.x < 0.0 && p.y < 0.0) r = vRadius.x;
    else if (p.x >= 0.0 && p.y < 0.0) r = vRadius.y;
    else if (p.x >= 0.0 && p.y >= 0.0) r = vRadius.z;
    else r = vRadius.w;

    float dist = squircleSDF(p, halfSize, r, vSmooth);

    float aa = fwidth(dist);
    float alpha = 1.0 - smoothstep(-aa, aa, dist);
    if (alpha <= 0.0) discard;

    fragColor = vec4(color.rgb, color.a * alpha);
}
