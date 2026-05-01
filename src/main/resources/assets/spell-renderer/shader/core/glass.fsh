#version 330 core

// thnx to https://medium.com/@aghajari/liquid-glass-ios-effect-explanation-dabadd6414ae

in vec2 vSize;
in vec4 vColor;
in vec4 vRadius;
in float vSmooth;
in vec2 vUV;

uniform sampler2D uBackground;
uniform vec2 uResolution;

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

vec3 getBlurredColor(vec2 coord, float blurRadius) {
    vec3 color = vec3(0.0);
    float totalWeight = 0.0;

    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            vec2 offset = vec2(float(x), float(y)) * blurRadius / uResolution;
            float weight = exp(-0.5 * (float(x*x + y*y)) / 2.0);

            color += texture(uBackground, coord + offset).rgb * weight;
            totalWeight += weight;
        }
    }

    return color / totalWeight;
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
    if (dist > aa) discard;

    float distFromCenter = 1.0 - clamp(-dist / (min(halfSize.x, halfSize.y) * 0.3), 0.0, 1.0);
    float distortion = 1.0 - sqrt(max(1.0 - distFromCenter * distFromCenter, 0.0));
    vec2 normCoord = p / halfSize;
    vec2 screenUV = gl_FragCoord.xy / uResolution;

    vec2 offsetPx = distortion * normCoord * halfSize;
    offsetPx.y = -offsetPx.y;
    vec2 sampleUV = screenUV - offsetPx / uResolution;

    float edgeFactor = smoothstep(0.0, 0.02, -dist);
    vec2 shift = normCoord * edgeFactor * 3.0;
    shift.y = -shift.y;
    vec2 shiftUV = shift / uResolution;

    float blurRadiusPx = 1.2 * (1.0 - distFromCenter * 0.5);
    vec3 glassColor = vec3(getBlurredColor(sampleUV - shiftUV, blurRadiusPx).r,
                    getBlurredColor(sampleUV, blurRadiusPx).g,
                    getBlurredColor(sampleUV + shiftUV, blurRadiusPx).b
    );

    glassColor *= vColor.rgb;
    float alpha = 1.0 - smoothstep(-aa, aa, dist);
    fragColor = vec4(glassColor, alpha * vColor.a);
}
