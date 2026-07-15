#version 120

uniform sampler2D textureSampler;

uniform float strength;

/*
 * Normalized inner radius:
 *
 * 0.0 = no black center
 * 1.0 = reaches the outer boundary
 *
 * Only used by circle mode.
 */
uniform float innerRadius;

/*
 * 0 = circle
 * 1 = cylinder/capsule
 */
uniform int shapeMode;

/*
 * Full world-space width and height.
 */
uniform vec2 effectDimensions;

/*
 * Framebuffer mapping for the rotated effect quad.
 */
uniform vec2 centerUV;
uniform vec2 axisXUV;
uniform vec2 axisYUV;

uniform vec2 texelSize;

vec2 localToScreenUV(vec2 localUV)
{
    vec2 centered = localUV - vec2(0.5);

    return centerUV
        + centered.x * 2.0 * axisXUV
        + centered.y * 2.0 * axisYUV;
}

void main()
{
    vec2 localUV =
        gl_TexCoord[0].st;

    /*
     * Local world-space coordinates around the effect center.
     */
    vec2 localPosition =
        (localUV - vec2(0.5))
        * effectDimensions;

    float shapeRadius =
        max(effectDimensions.x * 0.5, 0.0001);

    /*
     * For a circle, the closest axis point is the center.
     *
     * For a cylinder, it is the closest point on the cylinder's central
     * vertical line segment.
     */
    vec2 closestAxisPoint =
        vec2(0.0);

    if (shapeMode == 1)
    {
        float halfAxisLength = max(
            effectDimensions.y * 0.5 - shapeRadius,
            0.0
        );

        closestAxisPoint.y = clamp(
            localPosition.y,
            -halfAxisLength,
            halfAxisLength
        );
    }

    vec2 radialVector =
        localPosition - closestAxisPoint;

    float radialDistance =
        length(radialVector);

    float normalizedDistance =
        radialDistance / shapeRadius;

    /*
     * Smoothly fade the rendered effect before reaching the outside
     * boundary. This prevents the rectangular quad from becoming visible.
     */
    float effectAlpha =
        1.0 - smoothstep(
            0.72,
            1.0,
            normalizedDistance
        );

    if (effectAlpha <= 0.001)
    {
        discard;
    }

    /*
     * Preserve the original optional black center for circles.
     */
    if (shapeMode == 0
        && innerRadius > 0.0
        && normalizedDistance <= innerRadius)
    {
        gl_FragColor = vec4(
            vec3(0.0),
            effectAlpha * gl_Color.a
        );

        return;
    }

    vec2 radialDirection =
        vec2(0.0);

    if (radialDistance > 0.0001)
    {
        radialDirection =
            radialVector / radialDistance;
    }

    float distanceFromInner =
        max(
            normalizedDistance - innerRadius,
            0.0
        );

    float distortionAmount =
        strength
        / (
            distanceFromInner * distanceFromInner
            + 0.1
        );

    /*
     * Use the cylinder diameter as the distortion scale. This gives the
     * circular and cylinder modes comparable distortion strength.
     */
    vec2 displacementWorld =
        radialDirection
        * distortionAmount
        * shapeRadius
        * 2.0
        * effectAlpha;

    vec2 displacedLocalUV =
        localUV
        - displacementWorld
        / effectDimensions;

    vec2 distortedUV =
        localToScreenUV(displacedLocalUV);

    vec2 minimumSafeUV =
        texelSize * 0.5;

    vec2 maximumSafeUV =
        vec2(1.0) - minimumSafeUV;

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

    gl_FragColor = vec4(
        distortedColor,
        effectAlpha * gl_Color.a
    );
}