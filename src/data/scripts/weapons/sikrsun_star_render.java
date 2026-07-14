package data.scripts.weapons;

// import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
// import com.fs.starfarer.api.util.IntervalUtil;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.combat.CombatViewport;
import com.fs.starfarer.combat.entities.terrain.Planet;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Sphere;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class sikrsun_star_render extends BaseCombatLayeredRenderingPlugin {

    private ShipAPI ship;
    private WeaponAPI weapon;
    String starType;
    // private IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);
    float angle = 0;
    boolean UIMode = false;
    float size = 20;
    float scale = 1f;

    public sikrsun_star_render(ShipAPI ship, WeaponAPI weapon, String starType, float size) {
        this.ship = ship;
        this.weapon = weapon;
        this.starType = starType;
        this.size = size;


    }

    public sikrsun_star_render(ShipAPI ship, WeaponAPI weapon, String starType, boolean UIMode, float size) {
        this.ship = ship;
        this.weapon = weapon;
        this.starType = starType;
        this.UIMode = UIMode;
        this.size = size;

    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getRenderRadius() {
        return 10000f;
    }

    public void init(CombatEntityAPI entity) {
        super.init(entity);
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused() && !this.UIMode) return;
        if (!this.UIMode) {
            this.entity.getLocation().set(weapon.getLocation().x, weapon.getLocation().y);

        }
        angle += 0.1f;
        if (angle > 360) {
            angle = 0;
        }
    }

    @Override
    public boolean isExpired() {
        if (ship == null || ship.isHulk() || !Global.getCombatEngine().isEntityInPlay(ship) || weapon == null) {
            return true;
        }
        return false;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {


        renderPlanet(starType, weapon.getLocation(), size, ship.getFacing(), 3f, angle, 0f, viewport.getAlphaMult(),scale);


    }

    public static void renderPlanet(String spec, Vector2f point, float size, float facing, float pitch, float surfaceAngle, float atmoAngle, float alpha,float scale) {
        CustomPanelAPI p1 = Global.getSettings().createCustom(0, 0, new BaseCustomUIPanelPlugin() {
            @Override
            public void render(float alphaMult) {
                CombatViewport vv = new CombatViewport(point.x, point.y, 0, 0);
                vv.setAlphaMult(alpha);
                Planet planet = new Planet(spec, size, 0, point);
                planet.setScale(scale);
                planet.setAngle(surfaceAngle);
                planet.setCloudAngle(atmoAngle);
                planet.setTilt(facing - 90f);
                //planet.renderSphere((CombatViewport) viewport);
                planet.renderSphere(vv);
                planet.renderStarGlow(vv);
            }
        });
        p1.getPosition().setLocation(point.getX(), point.getY());
        p1.render(alpha);
    }

}
