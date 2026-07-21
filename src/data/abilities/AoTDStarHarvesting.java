package data.abilities;

import ashlib.shmo.api.ShmoGlobal;
import ashlib.shmo.api.campaign.StarSiphonEffect;
import ashlib.shmo.api.campaign.StarSiphonManager;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.effects.stareffect.StarCombatRenderingManager;

import java.awt.*;
import java.util.ArrayList;

public class AoTDStarHarvesting extends BaseToggleAbility {
    public static float range = 1000f;

    public static ArrayList<PlanetAPI> getAllStarsInSystem(CampaignFleetAPI fleet){
        ArrayList<PlanetAPI> stars = new ArrayList<>();
        if(fleet==null||fleet.getStarSystem()==null)return stars;

        for (PlanetAPI object : fleet.getStarSystem().getPlanets()) {
            if(object.isStar()){
                stars.add(object);
            }
        }
        return stars;
    }

    public static PlanetAPI getNearestStarInSystemFromFleetWithinArea(CampaignFleetAPI fleet,float area){
        if(fleet==null||fleet.getStarSystem()==null)return null;

        PlanetAPI nearest = null;
        float nearestDistance = Float.MAX_VALUE;

        for (PlanetAPI star : getAllStarsInSystem(fleet)) {
            float distance = Misc.getDistance(fleet,star)-(fleet.getRadius()+star.getRadius());
            if(distance<=area&&distance<nearestDistance){
                nearest = star;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public static boolean hasFirstBattlegroupShips(CampaignFleetAPI fleet){
        if(fleet==null)return false;
        return fleet.getFleetData().getMembersListCopy().stream()
                .anyMatch(x -> StarCombatRenderingManager.getDataFromList(x.getHullSpec().getHullId())!=null);
    }

    @Override
    protected void activateImpl() {
        CampaignFleetAPI fleet = getFleet();
        if(fleet==null)return;

        PlanetAPI nearestStar = getNearestStarInSystemFromFleetWithinArea(fleet,range);
        if(nearestStar==null)return;
        if(!isUsable()&&isActive()){
            deactivate();
            return;
        }
        final StarSiphonManager plugin = ShmoGlobal.getStarSiphonManager();
        if(plugin==null)return;

        final StarSiphonEffect starSiphonEffect = plugin.getSiphonForEntity(fleet);
        if(starSiphonEffect==null)return;
        if(starSiphonEffect.isActive())return;
        starSiphonEffect.setCollectionOffset(0,0);
        starSiphonEffect.setTimeScale(0.6f);
        starSiphonEffect.setConsumer(fleet);
        starSiphonEffect.setStar(nearestStar);
        starSiphonEffect.activate();
    }

    @Override
    protected void deactivateImpl() {
        CampaignFleetAPI fleet = getFleet();
        if(fleet==null)return;

        final StarSiphonManager plugin = ShmoGlobal.getStarSiphonManager();
        if(plugin==null)return;

        final StarSiphonEffect starSiphonEffect = plugin.getSiphonForEntity(fleet);
        if(starSiphonEffect!=null){
            starSiphonEffect.deactivate();
        }
    }

    @Override
    public boolean isUsable() {
        CampaignFleetAPI fleet = getFleet();
        if(fleet==null)return false;
        if(!hasFirstBattlegroupShips(fleet))return false;
        return getNearestStarInSystemFromFleetWithinArea(fleet,range)!=null;
    }

    @Override
    protected void applyEffect(float amount, float level) {
        CampaignFleetAPI fleet = getFleet();
        if(fleet==null)return;

        float currSlow = 1-level;
        fleet.getStats().getFleetwideMaxBurnMod().modifyMult(getModId(),currSlow,"Star Harvesting");
        if(currSlow==0){
            fleet.setVelocity(0,0);
        }
        if(level<=0){
            cleanupImpl();
        }
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    public void fleetJoinedBattle(BattleAPI battle) {
        deactivate();
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        Color gray = Misc.getGrayColor();
        Color highlight = Misc.getHighlightColor();

        String status = " (off)";
        if(turnedOn){
            status = " (on)";
        }

        if(!Global.CODEX_TOOLTIP_MODE){
            LabelAPI title = tooltip.addTitle("Star Harvesting"+status);
            title.highlightLast(status);
            title.setHighlightColor(gray);
        }
        else{
            tooltip.addSpacer(-10f);
        }

        float pad = 10f;

        tooltip.addPara(
                "First Battlegroup vessels use %s as their primary fuel. Their fuel reserves can be replenished by harvesting it directly from stars.",
                pad,
                highlight,
                "Solarite"
        );

        tooltip.addPara(
                "Activating Star Harvest begins harvesting Solarite from a nearby star. The fleet %s while the siphoning process is active.",
                pad,
                Misc.getNegativeHighlightColor(),
                "cannot move"
        );

        CampaignFleetAPI fleet = getFleet();

        if(!hasFirstBattlegroupShips(fleet)){
            tooltip.addPara(
                    "Only fleets containing %s can harvest Solarite.",
                    pad,
                    Misc.getNegativeHighlightColor(),
                    "First Battlegroup ships"
            );
        }

        if(getAllStarsInSystem(fleet).isEmpty()){
            tooltip.addPara(
                    "There are %s in the current star system.",
                    pad,
                    Misc.getNegativeHighlightColor(),
                    "no stars"
            );
        }
        else if(getNearestStarInSystemFromFleetWithinArea(fleet,range)==null){
            tooltip.addPara(
                    "The fleet must be within %s units of a star to begin harvesting Solarite.",
                    pad,
                    Misc.getNegativeHighlightColor(),
                    Misc.getWithDGS((int)range)
            );
        }

        addIncompatibleToTooltip(tooltip,expanded);
    }

    @Override
    protected void cleanupImpl() {
        CampaignFleetAPI fleet = getFleet();
        if(fleet==null)return;
        fleet.getStats().getFleetwideMaxBurnMod().unmodify(getModId());
    }
}