package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;

import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;

//Made by PureTilt for Astarat, used with permission
public class sikrsun_engine_glow implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false;
    //private float trail_id;
    //private SpriteAPI trail_sprite;
    private int range = 30; //6
    private ShipAPI ship;
    private float currentBrightness = 0.5f;
    private final float timeToChange = 0.75f;
    private Color engineColor;
    private boolean flameOut = false;
    private ArrayList<ShipEngineControllerAPI.ShipEngineAPI> thrusters = new ArrayList<ShipEngineControllerAPI.ShipEngineAPI>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        //some initial setup
        if (!this.runOnce) {
            runOnce = true;
            ship = weapon.getShip();
            //trail_id = MagicTrailPlugin.getUniqueID();
            //trail_sprite = Global.getSettings().getSprite("fx", "sikrsun_trail_1");
            //list the linked thrusters
            for (ShipEngineControllerAPI.ShipEngineAPI e : ship.getEngineController().getShipEngines()) {
                if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), range)) {
                    thrusters.add(e);
                }
            }
            if (!thrusters.isEmpty()) engineColor = thrusters.get(0).getEngineColor();
        }

        //default brightness is idle
        float targetBrightness = 0.4f;

        //change brightness depending on current actions
        if (ship.getEngineController().isAccelerating() || ship.getEngineController().isAcceleratingBackwards()) {
            targetBrightness = 1f;
        } else if (ship.getEngineController().isTurningLeft() || ship.getEngineController().isTurningRight() || ship.getEngineController().isStrafingLeft() || ship.getEngineController().isStrafingRight()) {
            targetBrightness = 0.75f;
        } else if (ship.getEngineController().isDecelerating()){
            targetBrightness = 0.6f;
        }

        //smooth glow change
        if (currentBrightness > targetBrightness) {
            currentBrightness -= amount / timeToChange;
            if (currentBrightness < targetBrightness)
                currentBrightness = targetBrightness;
        } else if (currentBrightness < targetBrightness) {
            currentBrightness += amount / timeToChange;
            if (currentBrightness > targetBrightness)
                currentBrightness = targetBrightness;
        }

        //set glow to 0 if all thrusters flame out
        for(ShipEngineControllerAPI.ShipEngineAPI thruster : thrusters){
            if(!thruster.isDisabled()){
                flameOut = false;
                break;
            }else{
                flameOut = true;
            }
        }
        if (flameOut || ship.isHulk() || ship.isPhased()) {
            currentBrightness = 0f;
        }

        //make color and apply it to sprite
        //Color colorToUse = new Color(COLOR_NORMAL[0], COLOR_NORMAL[1], COLOR_NORMAL[2], currentBrightness * MAX_OPACITY);
        Color shift = ship.getEngineController().getFlameColorShifter().getCurr();
        float ratio = shift.getAlpha() / 255f;
        int Red = Math.min(255, Math.round(engineColor.getRed() * (1f - ratio) + shift.getRed() * ratio));
        int Green = Math.min(255, Math.round(engineColor.getGreen() * (1f - ratio) + shift.getGreen() * ratio));
        int Blue = Math.min(255, Math.round(engineColor.getBlue() * (1f - ratio) + shift.getBlue() * ratio));
        /*
        if (ship.getVariant().hasHullMod("safetyoverrides")) {
            Red = Math.round((Red * 0.8f) + (255 * 0.2f)) - 1;
            Green = Math.round((Green * 0.8f) + (100 * 0.2f)) - 1;
            Blue = Math.round((Blue * 0.8f) + (255 * 0.2f)) - 1;
        }
         */
        Color colorToUse = new Color(Red, Green, Blue, Math.round(currentBrightness * 255));
        weapon.getSprite().setColor(colorToUse);

        //MagicTrailPlugin.AddTrailMemberAdvanced(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle, startAngularVelocity, endAngularVelocity, 
            //startSize, endSize, startColor, endColor, opacity, inDuration, mainDuration, outDuration, additive, textureLoopLength, textureScrollSpeed, textureOffset, offsetVelocity, advancedOptions, layerToRenderOn, frameOffsetMult);    
        //MagicTrailPlugin.AddTrailMemberAdvanced(ship, trail_id, trail_sprite, weapon.getLocation(), 10f, 15f, Misc.getAngleInDegrees(ship.getVelocity()), 0f, 0f, 40f, 30f, colorToUse, colorToUse, 
            //currentBrightness / 2, 0.3f, 0f, 1f, false, 1f, 1f, null, null, null, 0.1f);
    }
}