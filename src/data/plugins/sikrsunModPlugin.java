package data.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import data.scripts.coreui.RefitScreenSunInserter;
import data.scripts.effects.EffectsInitializer;
import data.scripts.effects.bendingeffect.BendingEffectDataHandler;
import data.scripts.effects.bendingeffect.BendingShipEffectData;


public class sikrsunModPlugin extends BaseModPlugin { //very basic mod plugin used to generate the system when a new game is created


    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);
        Global.getSector().addTransientScript(new RefitScreenSunInserter());
        BendingEffectDataHandler.loadData();
//        Global.getSector().addTransientScript(new EffectsInitializer());
//        Global.getCombatEngine().addPlugin(new EffectsInitializer());
    }
}
