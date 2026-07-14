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
import com.fs.starfarer.combat.entities.terrain.Planet;
import data.scripts.reflection.ReflectionUtilis;
import data.scripts.weapons.sikrsun_star_render;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class SunRedneringUI implements CustomUIPanelPlugin {
    ShipAPI ship;
    WeaponAPI weapon;
    CustomPanelAPI mainPanel;
    UIPanelAPI shipPanel;
    public CustomPanelAPI getMainPanel(){
        return mainPanel;
    }
    CombatViewport viewport;
    CustomPanelAPI contentPanel;
    String spriteCenterXField,spriteCenterYField;
    SpriteSpecUtilExact.SpriteSpec spec;
    sikrsun_star_render ren;
    public SunRedneringUI(float width,float height,ShipAPI ship,WeaponAPI weapon,UIPanelAPI shipPanel){
        mainPanel = Global.getSettings().createCustom(width,height,this);
        this.ship = ship;
        this.weapon = weapon;
        this.shipPanel = shipPanel;
        this.viewport = (CombatViewport) ReflectionUtilis.findFieldByType(shipPanel,CombatViewport.class);
        float spriteCenterX = ship.getSpriteAPI().getCenterX()-ship.getSpriteAPI().getWidth()/2;
        float spriteCenterY = ship.getSpriteAPI().getCenterY()-ship.getSpriteAPI().getHeight()/2;
        spriteCenterXField = ReflectionUtilis.getFloatFieldNameMatchingValue(shipPanel,spriteCenterX);
        spriteCenterYField = ReflectionUtilis.getFloatFieldNameMatchingValue(shipPanel,spriteCenterY);
        ren = new sikrsun_star_render(ship,weapon,"star_yellow",true,20);
        
    }
    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    
    @Override
    public void render(float alphaMult) {
        GL11.glPushMatrix();
        
        
      
        GL11.glTranslatef((float)Math.round(shipPanel.getPosition().getCenterX() + (float)ReflectionUtilis.getPrivateVariable(spriteCenterXField,shipPanel) * RefitScreenSunInserter.lastSavedZoom ), (float)Math.round(shipPanel.getPosition().getCenterY() +(float)ReflectionUtilis.getPrivateVariable(spriteCenterYField,shipPanel)  * RefitScreenSunInserter.lastSavedZoom ), 0.0F);
        GL11.glScalef(RefitScreenSunInserter.lastSavedZoom, RefitScreenSunInserter.lastSavedZoom, 1.0F);
        for (CombatEngineLayers activeLayer : ship.getActiveLayers()) {
            ren.render(activeLayer,viewport);
        }
        GL11.glPopMatrix();

    }

    @Override
    public void advance(float amount) {

        ren.advance(amount/2);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }


    @Override
    public void buttonPressed(Object buttonId) {

    }
}
