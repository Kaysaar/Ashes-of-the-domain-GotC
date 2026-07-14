#version 120

uniform sampler2D textureSampler;
uniform float strength;
uniform float radius;

uniform vec2 minUV;
uniform vec2 maxUV;

float sdCircle(vec2 p, float r)
{
    return length(p) - r;
}

void main()
{
    vec2 uv = mix(minUV, maxUV, gl_TexCoord[0].st); // fix uv coordinates
    vec2 center = (maxUV + minUV) / 2.0; // center
    vec2 zoomScale = maxUV - minUV; // zoom mult

    vec2 p = uv - center; // difference from center

    float aspect = zoomScale.x / zoomScale.y;
    p.x *= aspect;

    float dist = sdCircle(p / zoomScale, radius);

    vec3 col = vec3(1.);
    if (dist > 0.0)
    {
        float edgeDist = length(p / zoomScale);
        float falloff = 1.0 - smoothstep(0.0, 0.5, edgeDist);

        vec2 dir = (strength) * normalize(p) / (dist * dist + 0.1);
        vec2 finalDir = dir * falloff;

        vec2 distortedUV = uv - (finalDir * zoomScale);
        col = texture2D(textureSampler, distortedUV).rgb;
    }
    else
        col = vec3(1.0 - sign(radius));

    gl_FragColor = vec4(col.rgb, gl_Color.a);
}
