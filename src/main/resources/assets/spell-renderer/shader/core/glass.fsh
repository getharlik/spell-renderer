#version 330 core

// thnx to https://medium.com/@aghajari/liquid-glass-ios-effect-explanation-dabadd6414ae

in vec2 vSize;
in vec4 vColor;
in vec4 vRadius;
in float vSmooth;
in vec2 vUV;
in vec4 vGlass; // blur, refraction, chromatic, saturation
in float vHighlight;

uniform sampler2D uBackground; // mipmapped Kawase pyramid, level 0 is the sharp background
uniform vec2 uResolution;      // framebuffer pixels
uniform float uScale;          // framebuffer pixels per shape unit
uniform float uMaxLod;

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

// Samples the pyramid at the level matching blurPx, smoothed with the dual Kawase upsample pattern.
vec3 sampleBlur(vec2 uv, float blurPx) {
    vec2 halfPx = 0.5 / uResolution;
    uv = clamp(uv, halfPx, 1.0 - halfPx);
    if (blurPx < 0.5) return textureLod(uBackground, uv, 0.0).rgb;

    float lod = clamp(log2(blurPx), 0.0, uMaxLod);
    vec2 h = exp2(lod) * halfPx;

    vec3 sum = textureLod(uBackground, uv + vec2(-h.x * 2.0, 0.0), lod).rgb;
    sum += textureLod(uBackground, uv + vec2(h.x * 2.0, 0.0), lod).rgb;
    sum += textureLod(uBackground, uv + vec2(0.0, h.y * 2.0), lod).rgb;
    sum += textureLod(uBackground, uv + vec2(0.0, -h.y * 2.0), lod).rgb;
    sum += textureLod(uBackground, uv + vec2(-h.x, h.y), lod).rgb * 2.0;
    sum += textureLod(uBackground, uv + vec2(h.x, h.y), lod).rgb * 2.0;
    sum += textureLod(uBackground, uv + vec2(h.x, -h.y), lod).rgb * 2.0;
    sum += textureLod(uBackground, uv + vec2(-h.x, -h.y), lod).rgb * 2.0;
    return sum / 12.0;
}

void main() {
    vec2 halfSize = vSize * 0.5;
    vec2 p = vUV - halfSize;

    float r;
    if (p.x < 0.0 && p.y < 0.0) r = vRadius.x;
    else if (p.x >= 0.0 && p.y < 0.0) r = vRadius.y;
    else if (p.x >= 0.0 && p.y >= 0.0) r = vRadius.z;
    else r = vRadius.w;

    float dist = squircleSDF(p, halfSize, r, vSmooth);
    float aa = fwidth(dist);
    vec2 grad = vec2(dFdx(dist), dFdy(dist));
    if (dist > aa) discard;

    float blur = vGlass.x;
    float refraction = vGlass.y;
    float chromatic = vGlass.z;
    float saturation = vGlass.w;

    // Outward edge normal in screen space (y-up, same as gl_FragCoord)
    vec2 normal = grad / max(length(grad), 1e-6);

    // 0 deep inside, 1 at the rim
    float edgeT = 1.0 - clamp(-dist / (min(halfSize.x, halfSize.y) * 0.3), 0.0, 1.0);
    float lens = 1.0 - sqrt(max(1.0 - edgeT * edgeT, 0.0));

    vec2 screenUV = gl_FragCoord.xy / uResolution;
    vec2 refractPx = lens * p * refraction * uScale;
    refractPx.y = -refractPx.y; // shape space is y-down
    vec2 uv = screenUV - refractPx / uResolution;

    float blurPx = blur * uScale;
    vec3 color;
    if (chromatic > 0.0) {
        vec2 shift = normal * edgeT * chromatic * uScale / uResolution;
        color = vec3(sampleBlur(uv - shift, blurPx).r,
                     sampleBlur(uv, blurPx).g,
                     sampleBlur(uv + shift, blurPx).b);
    } else {
        color = sampleBlur(uv, blurPx);
    }

    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = clamp(mix(vec3(luma), color, saturation), 0.0, 1.0);
    color *= vColor.rgb;

    if (vHighlight > 0.0) {
        float rim = 1.0 - smoothstep(0.0, 1.5, -dist);
        float facing = abs(dot(normal, normalize(vec2(-1.0, 1.0)))); // top-left & bottom-right
        color += vHighlight * rim * (0.35 + 0.65 * facing);
    }

    float alpha = 1.0 - smoothstep(-aa, aa, dist);
    fragColor = vec4(color, alpha * vColor.a);
}
