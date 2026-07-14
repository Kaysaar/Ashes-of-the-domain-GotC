package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.effects.stareffect.StarCombatRenderingData;
import data.scripts.effects.stareffect.StarCombatRenderingManager;

public class sikrsun_yellow_sun implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false;
    float size =10f;
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(!runOnce&&amount>=0){
            StarCombatRenderingData data = StarCombatRenderingManager.getDataFromList(weapon.getShip().getHullSpec().getHullId());
            if(data.getStarSlotData(weapon.getSlot().getId())!=null){
                sikrsun_star_render plugin = new sikrsun_star_render(weapon.getShip(), weapon,"star_yellow",data.getStarSlotData(weapon.getSlot().getId()).starSize);
                engine.addLayeredRenderingPlugin(plugin);

            }


            runOnce = true;
        }
    }
    
}
