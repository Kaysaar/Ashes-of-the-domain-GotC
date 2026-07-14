package data.scripts.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import data.scripts.effects.bendingeffect.BendingEffectDataHandler;
import data.scripts.effects.stareffect.StarCombatRenderingManager;

public class CoreUIListener implements CoreUITabListener {
    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
        if(Global.getSettings().isDevMode()){
            BendingEffectDataHandler.loadData();
            StarCombatRenderingManager.loadData();
        }

    }
}
