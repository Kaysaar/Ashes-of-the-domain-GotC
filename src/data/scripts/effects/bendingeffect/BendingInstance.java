package data.scripts.effects.bendingeffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.effects.ShaderUniformManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

public class BendingInstance {

    private float radius;
    private float halfSize = 64f;

    private float centerXOffset;
    private float centerYOffset;

    private Vector2f referencePoint =
            new Vector2f(0f, 0f);

    private boolean remove;
    private boolean needsBackBufferUpdate;

    private float strength;
    private float currStrength = 0.04f;
    private float minStrength = 0.05f;

    private ShipAPI ship;

    /*
     * Cached world-space center. Recalculated whenever visibility,
     * overlap, or rendering is checked.
     */
    private float actualCenterX;
    private float actualCenterY;

    public BendingInstance() {
        this(128f);
    }

    /**
     * @param size width and height of the effect square in world units
     */
    public BendingInstance(float size) {
        this(size, 0.05f);
    }

    /**
     * @param size     width and height of the effect square in world units
     * @param strength distortion strength
     */
    public BendingInstance(
            float size,
            float strength
    ) {
        this(size, strength, 0.1f);
    }

    /**
     * @param size     width and height of the effect square in world units
     * @param strength distortion strength
     * @param radius   inner radius of the effect
     */
    public BendingInstance(
            float size,
            float strength,
            float radius
    ) {
        this.halfSize = size / 2f;
        this.strength = strength;
        this.radius = radius;
        this.currStrength = minStrength;
    }

    /**
     * Retained for compatibility with any code that renders an instance
     * directly rather than through BendingEffectHandler.
     */
    public void render(
            ViewportAPI viewport,
            ShaderUniformManager manager
    ) {
        float screenWidth =
                Global.getSettings().getScreenWidthPixels();

        float screenHeight =
                Global.getSettings().getScreenHeightPixels();

        render(
                viewport,
                manager,
                screenWidth,
                screenHeight
        );
    }

    /**
     * Optimized rendering path used by BendingEffectHandler.
     */
    public void render(
            ViewportAPI viewport,
            ShaderUniformManager manager,
            float screenWidth,
            float screenHeight
    ) {
        if (viewport == null
                || manager == null
                || screenWidth <= 0f
                || screenHeight <= 0f) {
            return;
        }

        updateActualCenter();

        float x = actualCenterX - halfSize;
        float y = actualCenterY - halfSize;

        float xe = actualCenterX + halfSize;
        float ye = actualCenterY + halfSize;

        /*
         * Each UV must correspond to the vertex carrying the matching
         * local texture coordinate.
         */
        float left =
                viewport.convertWorldXtoScreenX(x)
                        / screenWidth;

        float lower =
                viewport.convertWorldYtoScreenY(y)
                        / screenHeight;

        float right =
                viewport.convertWorldXtoScreenX(xe)
                        / screenWidth;

        float upper =
                viewport.convertWorldYtoScreenY(ye)
                        / screenHeight;

        updateCurrentStrength();

        manager
                .setFloat("strength", currStrength)
                .setFloat("radius", radius)
                .setVector2(
                        "minUV",
                        new Vector2f(left, lower)
                )
                .setVector2(
                        "maxUV",
                        new Vector2f(right, upper)
                );

        GL11.glColor4f(
                1f,
                1f,
                1f,
                1f
        );

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x, y);

        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(xe, y);

        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(x, ye);

        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(xe, ye);

        GL11.glEnd();
    }

    /**
     * Checks whether any part of this effect quad is currently visible.
     */
    public boolean isVisible(
            ViewportAPI viewport,
            float screenWidth,
            float screenHeight
    ) {
        if (viewport == null
                || screenWidth <= 0f
                || screenHeight <= 0f) {
            return false;
        }

        updateActualCenter();

        float x = actualCenterX - halfSize;
        float y = actualCenterY - halfSize;

        float xe = actualCenterX + halfSize;
        float ye = actualCenterY + halfSize;

        float screenX1 =
                viewport.convertWorldXtoScreenX(x);

        float screenX2 =
                viewport.convertWorldXtoScreenX(xe);

        float screenY1 =
                viewport.convertWorldYtoScreenY(y);

        float screenY2 =
                viewport.convertWorldYtoScreenY(ye);

        float screenLeft =
                Math.min(screenX1, screenX2);

        float screenRight =
                Math.max(screenX1, screenX2);

        float screenBottom =
                Math.min(screenY1, screenY2);

        float screenTop =
                Math.max(screenY1, screenY2);

        return screenRight >= 0f
                && screenLeft <= screenWidth
                && screenTop >= 0f
                && screenBottom <= screenHeight;
    }

    /**
     * Determines whether the circular areas of two bending instances
     * overlap in world space.
     *
     * When they overlap, the later effect should receive an updated
     * framebuffer so it can bend the result of the earlier effect.
     */
    public boolean overlaps(BendingInstance other) {
        if (other == null || other == this) {
            return false;
        }

        updateActualCenter();
        other.updateActualCenter();

        float differenceX =
                actualCenterX - other.actualCenterX;

        float differenceY =
                actualCenterY - other.actualCenterY;

        float combinedRadius =
                halfSize + other.halfSize;

        return differenceX * differenceX
                + differenceY * differenceY
                <= combinedRadius * combinedRadius;
    }

    private void updateActualCenter() {
        if (ship != null && ship.getLocation() != null) {
            actualCenterX = ship.getLocation().x;
            actualCenterY = ship.getLocation().y;

            float radians = (float) Math.toRadians(
                    ship.getFacing() - 90f
            );

            float cosine = (float) Math.cos(radians);
            float sine = (float) Math.sin(radians);

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

    private void updateCurrentStrength() {
        float targetStrength;

        if (ship == null
                || ship.getEngineController() == null
                || !ship.getEngineController().isIdle()) {
            targetStrength = strength;
        } else {
            targetStrength = minStrength;
        }

        float maximumChange = Math.max(
                Math.abs(strength) * 0.01f,
                0.0001f
        );

        currStrength = moveTowards(
                currStrength,
                targetStrength,
                maximumChange
        );
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
        this.halfSize = Math.max(
                0f,
                size / 2f
        );

        return this;
    }

    public BendingInstance setStrength(float strength) {
        this.strength = strength;
        return this;
    }

    public BendingInstance setMinStrength(float minStrength) {
        this.minStrength = minStrength;
        return this;
    }

    public BendingInstance setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    /**
     * Forces this instance to capture the framebuffer again before
     * rendering. Automatic overlap detection normally makes this
     * unnecessary, but the method remains available for special cases.
     */
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

        if (ship != null && ship.getLocation() != null) {
            setReferencePoint(ship.getLocation());
        }

        return this;
    }

    public void dispose() {
        remove = true;
    }

    public boolean shouldRemove() {
        return remove;
    }
}