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

    private float centerXOffset = 0f;
    private float centerYOffset = 0f;

    private Vector2f referencePoint = new Vector2f(0f, 0f);

    private boolean remove = false;

    private float strength;
    private float minStrength = 0.05f;
    private float currStrength = 0.05f;

    private ShipAPI ship;

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
    public BendingInstance(float size, float strength) {
        this(size, strength, 0.1f);
    }

    /**
     * @param size     width and height of the effect square in world units
     * @param strength distortion strength
     * @param radius   black-hole radius; 0.5 makes the circle touch the edges
     */
    public BendingInstance(float size, float strength, float radius) {
        this.halfSize = size / 2f;
        this.strength = strength;
        this.radius = radius;
        this.currStrength = minStrength;
    }

    public void render(
            ViewportAPI viewport,
            ShaderUniformManager manager
    ) {
        if (viewport == null || manager == null) {
            return;
        }

        float actualCenterX = referencePoint.x;
        float actualCenterY = referencePoint.y;

        if (ship != null) {
            float radians = (float) Math.toRadians(ship.getFacing() - 90f);

            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);

            float rotatedX =
                    centerXOffset * cos
                            - centerYOffset * sin;

            float rotatedY =
                    centerXOffset * sin
                            + centerYOffset * cos;

            actualCenterX += rotatedX;
            actualCenterY += rotatedY;
        } else {
            actualCenterX += centerXOffset;
            actualCenterY += centerYOffset;
        }

        float x = actualCenterX - halfSize;
        float y = actualCenterY - halfSize;
        float xe = actualCenterX + halfSize;
        float ye = actualCenterY + halfSize;

        float windowWidth =
                Global.getSettings().getScreenWidthPixels();

        float windowHeight =
                Global.getSettings().getScreenHeightPixels();

        if (windowWidth <= 0f || windowHeight <= 0f) {
            return;
        }

        float screenLeft =
                viewport.convertWorldXtoScreenX(x);

        float screenBottom =
                viewport.convertWorldYtoScreenY(y);

        float screenRight =
                viewport.convertWorldXtoScreenX(xe);

        float screenTop =
                viewport.convertWorldYtoScreenY(ye);

        /*
         * Do not render instances that are completely outside the screen.
         *
         * Partially visible instances are still rendered and OpenGL clips
         * their geometry normally.
         */
        if (screenRight <= 0f
                || screenLeft >= windowWidth
                || screenTop <= 0f
                || screenBottom >= windowHeight) {
            return;
        }

        /*
         * These are screen/framebuffer UV coordinates.
         *
         * Height must be normalized using screen height, not width.
         */
        float left = screenLeft / windowWidth;
        float bottom = screenBottom / windowHeight;
        float right = screenRight / windowWidth;
        float top = screenTop / windowHeight;

        updateCurrentStrength();

        manager
                .setFloat("strength", currStrength)
                .setFloat("radius", radius)
                .setVector2(
                        "minUV",
                        new Vector2f(left, bottom)
                )
                .setVector2(
                        "maxUV",
                        new Vector2f(right, top)
                )
                .setVector2(
                        "texelSize",
                        new Vector2f(
                                1f / windowWidth,
                                1f / windowHeight
                        )
                );

        GL11.glColor4f(1f, 1f, 1f, 1f);

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

    private void updateCurrentStrength() {
        float targetStrength;

        if (ship == null || ship.getEngineController() == null) {
            targetStrength = strength;
        } else if (ship.getEngineController().isIdle()) {
            targetStrength = minStrength;
        } else {
            targetStrength = strength;
        }

        float changeSpeed =
                Math.max(
                        Math.abs(strength - minStrength) * 0.01f,
                        0.0001f
                );

        currStrength = moveTowards(
                currStrength,
                targetStrength,
                changeSpeed
        );
    }

    private float moveTowards(
            float current,
            float target,
            float maximumChange
    ) {
        if (current < target) {
            return Math.min(current + maximumChange, target);
        }

        if (current > target) {
            return Math.max(current - maximumChange, target);
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
        this.halfSize = size / 2f;
        return this;
    }

    public BendingInstance setStrength(float strength) {
        this.strength = strength;
        return this;
    }

    public BendingInstance setMinStrength(float strength) {
        this.minStrength = strength;

        if (currStrength < minStrength) {
            currStrength = minStrength;
        }

        return this;
    }

    public BendingInstance setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    public BendingInstance setShip(ShipAPI ship) {
        this.ship = ship;

        if (ship != null) {
            setReferencePoint(ship.getLocation());
        }

        return this;
    }

    public void dispose() {
        this.remove = true;
    }

    /**
     * This is a question, not a statement.
     */
    public boolean shouldRemove() {
        return remove;
    }
}