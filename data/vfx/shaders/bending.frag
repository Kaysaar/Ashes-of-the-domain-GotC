#version 120

uniform sampler2D textureSampler;

uniform float strength;
uniform float radius;

uniform vec2 minUV;
uniform vec2 maxUV;
uniform vec2 texelSize;

void main()
{
    /*
     * Coordinates inside the square effect quad.
     * localUV always ranges from 0 to 1.
     */
    vec2 localUV = gl_TexCoord[0].st;

    /*
     * Corresponding position in the captured screen texture.
     */
    vec2 screenUV = mix(minUV, maxUV, localUV);

    /*
     * Position around the effect center.
     */
    vec2 localPosition = localUV - vec2(0.5);

    float distanceFromCenter = length(localPosition);

    /*
     * Circular outer boundary.
     *
     * Start fading before reaching the quad border so no square edge can
     * ever become visible.
     */
    const float outerRadius = 0.49;
    const float fadeStart = 0.38;

    float effectAlpha =
        1.0 - smoothstep(
            fadeStart,
            outerRadius,
            distanceFromCenter
        );

    /*
     * Outside the circular bending area, do not touch the framebuffer.
     */
    if (effectAlpha <= 0.001)
    {
        discard;
    }

    /*
     * Preserve the optional black inner core from the original shader.
     */
    if (distanceFromCenter <= radius)
    {
        gl_FragColor = vec4(
            vec3(0.0),
            effectAlpha
        );

        return;
    }

    vec2 direction =
        localPosition
        / max(distanceFromCenter, 0.0001);

    float distanceFromInnerRadius =
        distanceFromCenter - radius;

    /*
     * Distortion becomes stronger near the inner radius.
     */
    float distortionAmount =
        strength
        / (
            distanceFromInnerRadius
            * distanceFromInnerRadius
            + 0.1
        );

    /*
     * Convert displacement inside the local quad into screen-texture UV
     * displacement.
     */
    vec2 quadScreenSize = maxUV - minUV;

    vec2 distortedUV =
        screenUV
        - direction
        * quadScreenSize
        * distortionAmount
        * effectAlpha;

    /*
     * Prevent the shader from sampling outside the captured framebuffer
     * when the effect is close to a screen border.
     */
    vec2 minimumSafeUV = texelSize * 0.5;
    vec2 maximumSafeUV = vec2(1.0) - minimumSafeUV;

    distortedUV = clamp(
        distortedUV,
        minimumSafeUV,
        maximumSafeUV
    );

    vec3 distortedColor =
        texture2D(
            textureSampler,
            distortedUV
        ).rgb;

    /*
     * Alpha is zero toward the outer edge, allowing the normal background
     * to remain completely untouched there.
     */
    gl_FragColor = vec4(
        distortedColor,
        effectAlpha
    );
}