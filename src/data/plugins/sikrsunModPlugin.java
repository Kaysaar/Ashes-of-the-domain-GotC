package data.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.CampaignFleetMemberView;
import com.fs.starfarer.campaign.fleet.CampaignFleetView;
import com.fs.starfarer.campaign.fleet.FleetMember;
import data.scripts.coreui.CoreUIListener;
import data.scripts.coreui.FirstBattlegroupCampaingFleetViewMember;
import data.scripts.coreui.RefitScreenSunInserter;
import data.scripts.effects.bendingeffect.BendingEffectDataHandler;
import data.scripts.effects.stareffect.StarCombatRenderingManager;
import data.scripts.reflection.ReflectionUtilis;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class sikrsunModPlugin extends BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);

        Global.getSector().addTransientScript(new RefitScreenSunInserter());

        BendingEffectDataHandler.loadData();
        StarCombatRenderingManager.loadData();

        CampaignFleet playerFleet = (CampaignFleet) Global.getSector().getPlayerFleet();
        CampaignFleetView fleetView = playerFleet.getFleetView();

        Object collectionView = ReflectionUtilis.getPrivateVariableFromSuperClass(
                "shipViews",
                fleetView
        );

        if(collectionView!=null){
            Map<Object, CampaignFleetMemberView> views =
                    (Map<Object, CampaignFleetMemberView>) ReflectionUtilis.getPrivateVariableFromSuperClass(
                            "views",
                            collectionView
                    );

            Set<CampaignFleetMemberView> sortedViews =
                    (Set<CampaignFleetMemberView>) ReflectionUtilis.getPrivateVariableFromSuperClass(
                            "sortedViews",
                            collectionView
                    );

            if(views!=null&&sortedViews!=null){
                ArrayList<Map.Entry<Object, CampaignFleetMemberView>> entries =
                        new ArrayList<>(views.entrySet());

                boolean replacedAnything = false;

                for (Map.Entry<Object, CampaignFleetMemberView> entry : entries) {
                    CampaignFleetMemberView oldView = entry.getValue();

                    if(oldView==null||oldView.getMember()==null){
                        continue;
                    }

                    FleetMember member = oldView.getMember();

                    if(StarCombatRenderingManager.getDataFromList(member.getHullId())==null){
                        if(member.getHullSpec()==null
                                ||member.getHullSpec().getBaseHullId()==null
                                ||StarCombatRenderingManager.getDataFromList(
                                member.getHullSpec().getBaseHullId()
                        )==null){
                            continue;
                        }
                    }

                    FirstBattlegroupCampaingFleetViewMember newView =
                            new FirstBattlegroupCampaingFleetViewMember(
                                    playerFleet,
                                    member
                            );

                    sortedViews.remove(oldView);
                    views.put(entry.getKey(),newView);
                    sortedViews.add(newView);

                    replacedAnything = true;
                }

                if(replacedAnything){
                    fleetView.getContrails().clear();
                }
            }
        }

        Global.getSector().getListenerManager().addListener(
                new CoreUIListener(),
                true
        );
    }
}