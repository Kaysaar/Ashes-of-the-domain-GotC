package data.plugins;

import ashlib.data.scripts.CustomCampaignViewerListenerManager;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.CampaignFleetView;
import data.scripts.campaign.FirstBattlegroupCampaignFleetViewerListener;
import data.scripts.coreui.CoreUIListener;
import data.scripts.effects.bendingeffect.BendingEffectDataHandler;
import data.scripts.effects.stareffect.StarCombatRenderingManager;
import data.scripts.reflection.ReflectionUtilis;

import java.awt.*;

public class sikrsunModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        BendingEffectDataHandler.loadData();
        StarCombatRenderingManager.loadData();
        CustomCampaignViewerListenerManager.addListener("first_bg_star",new FirstBattlegroupCampaignFleetViewerListener());
    }

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);

        BendingEffectDataHandler.loadData();
        StarCombatRenderingManager.loadData();

        Global.getSector()
                .getListenerManager()
                .addListener(
                        new CoreUIListener(),
                        true
                );
    }

}