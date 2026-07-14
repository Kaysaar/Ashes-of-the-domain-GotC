package data.scripts.effects.bendingeffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.effects.ShaderUniformManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

public class BendingInstance {
    private float radius;
    private float halfSize = 64;
    private float centerXOffset = 0;
    private float centerYOffset = 0;
    private Vector2f referencePoint = new Vector2f(0, 0);
    private boolean remove = false;
    private float strength;
    private ShipAPI ship = null;
    private float currStrength = 0.04f;
    private float minStrength = 0.05f;
    public BendingInstance() {
        this(128f);
    }

    /**
     * @param size The width and height of the effect square in world units.
     */
    public BendingInstance(float size) {
        this(size, 0.05f);

    }

    /**
     * @param size     The width and height of the effect square in world units.
     * @param strength Distortion strength
     */
    public BendingInstance(float size, float strength) {
        this(size, strength, 0.1f);
    }

    /**
     * @param size     The width and height of the effect square in world units.
     * @param strength Distortion strength
     * @param radius   The black hole radius, .5 will make it so that the circle will touch the edges
     */
    public BendingInstance(float size, float strength, float radius) {
        this.halfSize = size / 2f;
        this.strength = strength;
        this.radius = radius;
        currStrength = minStrength;
    }

    public void render(ViewportAPI viewport, ShaderUniformManager manager) {
        float actualCenterX = referencePoint.x;
        float actualCenterY = referencePoint.y;

        if (ship != null) {
            float dir = ship.getFacing();
            float radians = (float) Math.toRadians(dir - 90);

            float rotatedX = (float) (centerXOffset * Math.cos(radians) - centerYOffset * Math.sin(radians));
            float rotatedY = (float) (centerXOffset * Math.sin(radians) + centerYOffset * Math.cos(radians));

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

        float windowWidth = Global.getSettings().getScreenWidthPixels();
        float windowHeight = Global.getSettings().getScreenWidthPixels();

        // normalise these as they will be used for texture coordinates
        float left = viewport.convertWorldXtoScreenX(x) / windowWidth;
        float top = viewport.convertWorldYtoScreenY(y) / windowHeight;
        float right = viewport.convertWorldXtoScreenX(xe) / windowWidth;
        float bottom = viewport.convertWorldYtoScreenY(ye) / windowHeight;

        if(!ship.getEngineController().isIdle()){
            currStrength+=strength*0.01f;
            if(currStrength>=strength){
                currStrength=strength;
            }
        }
        else{
            currStrength-=strength*0.01f;
            if(currStrength<=minStrength){
                currStrength=minStrength;
            }
        }
        manager // these need to be updated as theyre instance specific
                .setFloat("strength", currStrength)
                .setFloat("radius", this.radius)
                .setVector2("minUV", new Vector2f(left, top))
                .setVector2("maxUV", new Vector2f(right, bottom));

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(xe, y);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, ye);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(xe, ye);
        GL11.glEnd();
    }

    /// sets the new center of the instance
    public BendingInstance updateOffset(float centerX, float centerY) {
        this.centerXOffset = centerX;
        this.centerYOffset = centerY;
        return this;
    }

    /// sets the new referance point of the instance
    public BendingInstance setReferencePoint(Vector2f referancePoint) {
        this.referencePoint = referancePoint;
        return this;
    }

    public BendingInstance setSize(float size) {
        this.halfSize = size / 2;
        return this;
    }

    public BendingInstance setStrength(float strength) {
        this.strength = strength;
        return this;
    }
    public BendingInstance setMinStrength(float strength) {
        this.minStrength = strength;
        return this;
    }

    public BendingInstance setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    public BendingInstance setShip(ShipAPI ship) {
        this.setReferencePoint(ship.getLocation());
        this.ship = ship;
        return this;
    }

    public void dispose() {
        this.remove = true;
    }

    /// this is a question, not a statement
    public boolean shouldRemove() {
        return this.remove;
    }
}