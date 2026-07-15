package data.scripts.effects.bendingeffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.effects.ShaderUniformManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

public class BendingInstance {

    public enum ShapeMode {
        CIRCLE,
        CYLINDER
    }

    private static final float RENDER_EPSILON = 0.0001f;

    private ShapeMode shapeMode = ShapeMode.CIRCLE;

    /*
     * Circle mode:
     *     radius is the normalized inner radius.
     *
     * Cylinder mode:
     *     radius is the world-space half-width.
     */
    private float radius;

    private float halfWidth = 64f;
    private float halfHeight = 64f;

    private float centerXOffset;
    private float centerYOffset;

    /*
     * Relative effect rotation in degrees.
     *
     * For a ship-bound instance:
     *     0 degrees follows the ship sprite's vertical axis.
     */
    private float angle;

    private Vector2f referencePoint =
            new Vector2f(0f, 0f);

    private boolean remove;
    private boolean needsBackBufferUpdate;

    /*
     * Becomes true after the engines are disabled or flamed out.
     *
     * Once engines recover, the effect first rises back to minStrength.
     * Only after reaching minStrength does it resume normal behavior.
     */
    private boolean recoveringFromEngineShutdown;

    private float strength = 0.05f;
    private float minStrength = 0.05f;
    private float currStrength = 0.05f;

    private ShipAPI ship;

    private float actualCenterX;
    private float actualCenterY;

    public BendingInstance() {
        this(128f);
    }

    public BendingInstance(float size) {
        this(size, 0.05f);
    }

    public BendingInstance(
            float size,
            float strength
    ) {
        this(size, strength, 0.1f);
    }

    public BendingInstance(
            float size,
            float strength,
            float radius
    ) {
        setSize(size);

        this.strength = strength;
        this.radius = radius;
        this.currStrength = minStrength;
    }

    public void advance(float amount) {
        if (amount <= 0f) {
            return;
        }

        float targetStrength;
        boolean enginesUnavailable = false;

        if (ship == null || ship.getEngineController() == null) {
            /*
             * Non-ship-bound bending instances remain fully active.
             */
            recoveringFromEngineShutdown = false;
            targetStrength = strength;
        } else {
            enginesUnavailable =
                    ship.getEngineController().isDisabled()
                            || ship.getEngineController().isFlamedOut();

            if (enginesUnavailable) {
                /*
                 * Fade the effect completely out.
                 */
                recoveringFromEngineShutdown = true;
                targetStrength = 0f;
            } else if (recoveringFromEngineShutdown) {
                /*
                 * The engines have recovered.
                 *
                 * Return to the minimum strength first rather than
                 * immediately targeting maximum strength.
                 */
                targetStrength = minStrength;
            } else if (ship.getEngineController().isIdle()) {
                targetStrength = minStrength;
            } else {
                targetStrength = strength;
            }
        }

        /*
         * Frame-rate-independent transition speed.
         */
        float largestConfiguredStrength = Math.max(
                Math.abs(strength),
                Math.abs(minStrength)
        );

        float transitionPerSecond = Math.max(
                largestConfiguredStrength * 0.6f,
                0.01f
        );

        currStrength = moveTowards(
                currStrength,
                targetStrength,
                transitionPerSecond * amount
        );

        /*
         * Snap tiny residual values to exactly zero. This allows the
         * renderer to completely skip the instance.
         */
        if (enginesUnavailable
                && currStrength <= RENDER_EPSILON) {
            currStrength = 0f;
        }

        /*
         * Once minimum strength has been restored, normal idle/active
         * strength behavior may resume.
         */
        if (!enginesUnavailable
                && recoveringFromEngineShutdown
                && currStrength >= minStrength - RENDER_EPSILON) {

            currStrength = minStrength;
            recoveringFromEngineShutdown = false;
        }
    }

    public void render(
            ViewportAPI viewport,
            ShaderUniformManager manager
    ) {
        render(
                viewport,
                manager,
                Global.getSettings().getScreenWidthPixels(),
                Global.getSettings().getScreenHeightPixels()
        );
    }

    public void render(
            ViewportAPI viewport,
            ShaderUniformManager manager,
            float screenWidth,
            float screenHeight
    ) {
        /*
         * Do not issue any OpenGL draw calls while fully faded out.
         */
        if (!shouldRender()
                || viewport == null
                || manager == null
                || screenWidth <= 0f
                || screenHeight <= 0f) {
            return;
        }

        updateActualCenter();

        float rotation =
                getWorldRotationRadians();

        float cosine =
                (float) Math.cos(rotation);

        float sine =
                (float) Math.sin(rotation);

        /*
         * Half-width axis, pointing toward local right.
         */
        float rightX =
                cosine * halfWidth;

        float rightY =
                sine * halfWidth;

        /*
         * Half-height axis, pointing toward local top.
         */
        float upX =
                -sine * halfHeight;

        float upY =
                cosine * halfHeight;

        float bottomLeftX =
                actualCenterX - rightX - upX;

        float bottomLeftY =
                actualCenterY - rightY - upY;

        float bottomRightX =
                actualCenterX + rightX - upX;

        float bottomRightY =
                actualCenterY + rightY - upY;

        float topLeftX =
                actualCenterX - rightX + upX;

        float topLeftY =
                actualCenterY - rightY + upY;

        float topRightX =
                actualCenterX + rightX + upX;

        float topRightY =
                actualCenterY + rightY + upY;

        float centerScreenX =
                viewport.convertWorldXtoScreenX(actualCenterX)
                        / screenWidth;

        float centerScreenY =
                viewport.convertWorldYtoScreenY(actualCenterY)
                        / screenHeight;

        float rightScreenX =
                viewport.convertWorldXtoScreenX(
                        actualCenterX + rightX
                ) / screenWidth;

        float rightScreenY =
                viewport.convertWorldYtoScreenY(
                        actualCenterY + rightY
                ) / screenHeight;

        float upScreenX =
                viewport.convertWorldXtoScreenX(
                        actualCenterX + upX
                ) / screenWidth;

        float upScreenY =
                viewport.convertWorldYtoScreenY(
                        actualCenterY + upY
                ) / screenHeight;

        Vector2f centerUV =
                new Vector2f(
                        centerScreenX,
                        centerScreenY
                );

        Vector2f axisXUV =
                new Vector2f(
                        rightScreenX - centerScreenX,
                        rightScreenY - centerScreenY
                );

        Vector2f axisYUV =
                new Vector2f(
                        upScreenX - centerScreenX,
                        upScreenY - centerScreenY
                );

        float innerRadius =
                shapeMode == ShapeMode.CIRCLE
                        ? clamp(radius * 2f, 0f, 1f)
                        : 0f;

        manager
                .setFloat("strength", currStrength)
                .setFloat("innerRadius", innerRadius)
                .setInt(
                        "shapeMode",
                        shapeMode == ShapeMode.CYLINDER
                                ? 1
                                : 0
                )
                .setVector2(
                        "effectDimensions",
                        new Vector2f(
                                halfWidth * 2f,
                                halfHeight * 2f
                        )
                )
                .setVector2("centerUV", centerUV)
                .setVector2("axisXUV", axisXUV)
                .setVector2("axisYUV", axisYUV);

        GL11.glColor4f(
                1f,
                1f,
                1f,
                1f
        );

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(
                bottomLeftX,
                bottomLeftY
        );

        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(
                bottomRightX,
                bottomRightY
        );

        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(
                topLeftX,
                topLeftY
        );

        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(
                topRightX,
                topRightY
        );

        GL11.glEnd();
    }

    public boolean isVisible(
            ViewportAPI viewport,
            float screenWidth,
            float screenHeight
    ) {
        /*
         * This also makes the optimized handler exclude fully faded
         * instances before copying the framebuffer.
         */
        if (!shouldRender()
                || viewport == null
                || screenWidth <= 0f
                || screenHeight <= 0f) {
            return false;
        }

        updateActualCenter();

        float rotation =
                getWorldRotationRadians();

        float cosine =
                Math.abs((float) Math.cos(rotation));

        float sine =
                Math.abs((float) Math.sin(rotation));

        float extentX =
                cosine * halfWidth
                        + sine * halfHeight;

        float extentY =
                sine * halfWidth
                        + cosine * halfHeight;

        float firstScreenX =
                viewport.convertWorldXtoScreenX(
                        actualCenterX - extentX
                );

        float secondScreenX =
                viewport.convertWorldXtoScreenX(
                        actualCenterX + extentX
                );

        float firstScreenY =
                viewport.convertWorldYtoScreenY(
                        actualCenterY - extentY
                );

        float secondScreenY =
                viewport.convertWorldYtoScreenY(
                        actualCenterY + extentY
                );

        float left =
                Math.min(firstScreenX, secondScreenX);

        float right =
                Math.max(firstScreenX, secondScreenX);

        float bottom =
                Math.min(firstScreenY, secondScreenY);

        float top =
                Math.max(firstScreenY, secondScreenY);

        return right >= 0f
                && left <= screenWidth
                && top >= 0f
                && bottom <= screenHeight;
    }

    public boolean overlaps(
            BendingInstance other
    ) {
        if (other == null
                || other == this
                || !shouldRender()
                || !other.shouldRender()) {
            return false;
        }

        updateActualCenter();
        other.updateActualCenter();

        float differenceX =
                actualCenterX - other.actualCenterX;

        float differenceY =
                actualCenterY - other.actualCenterY;

        float combinedRadius =
                getBoundingRadius()
                        + other.getBoundingRadius();

        return differenceX * differenceX
                + differenceY * differenceY
                <= combinedRadius * combinedRadius;
    }

    private float getBoundingRadius() {
        return (float) Math.sqrt(
                halfWidth * halfWidth
                        + halfHeight * halfHeight
        );
    }

    private void updateActualCenter() {
        if (ship != null
                && ship.getLocation() != null) {
            actualCenterX =
                    ship.getLocation().x;

            actualCenterY =
                    ship.getLocation().y;

            float shipRotation =
                    (float) Math.toRadians(
                            ship.getFacing() - 90f
                    );

            float cosine =
                    (float) Math.cos(shipRotation);

            float sine =
                    (float) Math.sin(shipRotation);

            float rotatedX =
                    centerXOffset * cosine
                            - centerYOffset * sine;

            float rotatedY =
                    centerXOffset * sine
                            + centerYOffset * cosine;

            actualCenterX += rotatedX;
            actualCenterY += rotatedY;

            return;
        }

        actualCenterX =
                referencePoint.x + centerXOffset;

        actualCenterY =
                referencePoint.y + centerYOffset;
    }

    private float getWorldRotationRadians() {
        float worldAngle = angle;

        if (ship != null) {
            worldAngle += ship.getFacing() - 90f;
        }

        return (float) Math.toRadians(worldAngle);
    }

    private float moveTowards(
            float current,
            float target,
            float maximumChange
    ) {
        if (current < target) {
            return Math.min(
                    current + maximumChange,
                    target
            );
        }

        if (current > target) {
            return Math.max(
                    current - maximumChange,
                    target
            );
        }

        return target;
    }

    public boolean shouldRender() {
        return currStrength > RENDER_EPSILON;
    }

    public float getCurrentStrength() {
        return currStrength;
    }

    public BendingInstance updateOffset(
            float centerX,
            float centerY
    ) {
        this.centerXOffset = centerX;
        this.centerYOffset = centerY;
        return this;
    }

    public BendingInstance setReferencePoint(
            Vector2f referencePoint
    ) {
        if (referencePoint != null) {
            this.referencePoint = referencePoint;
        }

        return this;
    }

    public BendingInstance setSize(float size) {
        float safeSize =
                Math.max(size, 0.001f);

        shapeMode = ShapeMode.CIRCLE;

        halfWidth = safeSize / 2f;
        halfHeight = safeSize / 2f;

        return this;
    }

    public BendingInstance setCylinder(
            float cylinderRadius,
            float cylinderHeight
    ) {
        float safeRadius =
                Math.max(cylinderRadius, 0.001f);

        float safeHeight =
                Math.max(
                        cylinderHeight,
                        safeRadius * 2f
                );

        shapeMode = ShapeMode.CYLINDER;

        radius = safeRadius;

        halfWidth = safeRadius;
        halfHeight = safeHeight / 2f;

        return this;
    }

    public BendingInstance setAngle(float angle) {
        this.angle = angle;
        return this;
    }

    public BendingInstance setStrength(float strength) {
        this.strength = strength;
        return this;
    }

    public BendingInstance setMinStrength(
            float minStrength
    ) {
        this.minStrength = minStrength;
        this.currStrength = minStrength;
        return this;
    }

    public BendingInstance setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    public BendingInstance setNeedsBackBufferUpdate(
            boolean needsBackBufferUpdate
    ) {
        this.needsBackBufferUpdate =
                needsBackBufferUpdate;

        return this;
    }

    public boolean needsBackBufferUpdate() {
        return needsBackBufferUpdate;
    }

    public BendingInstance setShip(ShipAPI ship) {
        this.ship = ship;

        if (ship != null
                && ship.getLocation() != null) {
            setReferencePoint(ship.getLocation());
        }

        return this;
    }

    public ShapeMode getShapeMode() {
        return shapeMode;
    }

    public void dispose() {
        remove = true;
    }

    public boolean shouldRemove() {
        return remove;
    }

    private static float clamp(
            float value,
            float minimum,
            float maximum
    ) {
        return Math.max(
                minimum,
                Math.min(value, maximum)
        );
    }
}