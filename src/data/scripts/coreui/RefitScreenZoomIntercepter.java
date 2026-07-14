package data.scripts.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.combat.CombatViewport;
import data.scripts.reflection.ReflectionUtilis;
import data.scripts.weapons.sikrsun_star_render;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class RefitScreenZoomIntercepter implements CustomUIPanelPlugin {
    ShipAPI ship;
    CustomPanelAPI mainPanel;
    UIPanelAPI shipPanel;
    public CustomPanelAPI getMainPanel(){
        return mainPanel;
    }
    CombatViewport viewport;
    CustomPanelAPI contentPanel;
    String spriteCenterXField,spriteCenterYField;
    SpriteSpecUtilExact.SpriteSpec spec;
    float helper = 1f;
    float verdict = 1.5f;
    float mover = 1f;
    float moverInteract =1f;
    public RefitScreenZoomIntercepter(float width,float height,ShipAPI ship,UIPanelAPI shipPanel){
        mainPanel = Global.getSettings().createCustom(width,height,this);
        this.ship = ship;
        this.shipPanel = shipPanel;
        this.viewport = (CombatViewport) ReflectionUtilis.findFieldByType(shipPanel,CombatViewport.class);
        float spriteCenterX = ship.getSpriteAPI().getCenterX()-ship.getSpriteAPI().getWidth()/2;
        float spriteCenterY = ship.getSpriteAPI().getCenterY()-ship.getSpriteAPI().getHeight()/2;
        spriteCenterXField = ReflectionUtilis.getFloatFieldNameMatchingValue(shipPanel,spriteCenterX);
        spriteCenterYField = ReflectionUtilis.getFloatFieldNameMatchingValue(shipPanel,spriteCenterY);
        spec = SpriteSpecUtilExact.computeSizeWithModules(ship);
        float widths = spec.width;
        float heights = spec.height;
        mover = RefitScreenSunInserter.lastSavedZoom;
        Vector2f vector = SpriteSpecUtilExact.methodRandom(widths,heights,500f,500f);
        float ver = Math.min(vector.x/widths,vector.y/heights);
        if(ver<1.0f){
            helper = ver;
        }
        ShipAPI.HullSize var19 = ship.getHullSpec().getHullSize();
        switch (var19) {
            case FIGHTER -> verdict = 2.0F;
            case FRIGATE ->verdict= 2.0F;
            case DESTROYER -> verdict = 2.0F;
            case CRUISER -> verdict = 1.75F;
            case CAPITAL_SHIP -> verdict = 1.5F;
        }

        RefitScreenSunInserter.lastSavedZoom = Math.max( RefitScreenSunInserter.lastSavedZoom,helper);
        RefitScreenSunInserter.lastSavedZoom = Math.min( RefitScreenSunInserter.lastSavedZoom,verdict);
        moverInteract =  RefitScreenSunInserter.lastSavedZoom;


    }
    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    public void resetDueToShipChange(){

    }
    @Override
    public void render(float alphaMult) {

    }

    @Override
    public void advance(float amount) {
        // IMPORTANT: use the UI’s advance (i.e., the component’s advanceImpl),
        // not a campaign EveryFrameScript amount that can be ~0 when paused.
        if(!ship.equals(ReflectionUtilis.invokeMethodWithAutoProjection("getShip",shipPanel))){
            this.ship = (ShipAPI) ReflectionUtilis.invokeMethodWithAutoProjection("getShip",shipPanel);
            spec = SpriteSpecUtilExact.computeSizeWithModules(ship);
            float widths = spec.width;
            float heights = spec.height;
            Vector2f vector = SpriteSpecUtilExact.methodRandom(widths,heights,500f,500f);
            float ver = Math.min(vector.x/widths,vector.y/heights);
            if(ver<1.0f){
                helper = ver;
            }
            else{
                helper = 1f;
            }
            ShipAPI.HullSize var19 = ship.getHullSpec().getHullSize();
            switch (var19) {
                case FIGHTER -> verdict = 2.0F;
                case FRIGATE ->verdict= 2.0F;
                case DESTROYER -> verdict = 2.0F;
                case CRUISER -> verdict = 1.75F;
                case CAPITAL_SHIP -> verdict = 1.5F;
            }

            RefitScreenSunInserter.lastSavedZoom = Math.max( RefitScreenSunInserter.lastSavedZoom,helper);
            RefitScreenSunInserter.lastSavedZoom = Math.min( RefitScreenSunInserter.lastSavedZoom,verdict);
            moverInteract =  RefitScreenSunInserter.lastSavedZoom;
        }
        if (moverInteract !=  RefitScreenSunInserter.lastSavedZoom) {
            float delta = moverInteract -  RefitScreenSunInserter.lastSavedZoom;
            float step = Math.signum(delta) * amount * (Math.abs(delta * 3f) + 1f);
            if (Math.abs(step) > Math.abs(delta)) step = delta;
            RefitScreenSunInserter.lastSavedZoom += step;

            // vanilla calls a layout update each tick while animating
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (!event.isConsumed() && event.isMouseScrollEvent() && this.shipPanel.getPosition().containsEvent(event)) {
                moverInteract+=0.25f*Math.signum((float) event.getEventValue());
                if(moverInteract<helper){
                    moverInteract = helper;
                }
                if(moverInteract>verdict){
                    moverInteract = verdict;
                }
            }
        }

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
}
