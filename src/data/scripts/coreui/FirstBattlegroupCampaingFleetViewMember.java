package data.scripts.coreui;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.CampaignFleetMemberView;
import com.fs.starfarer.campaign.fleet.CampaignFleetView;
import com.fs.starfarer.campaign.fleet.FleetMember;
import data.scripts.effects.stareffect.StarCombatRenderingData;
import data.scripts.effects.stareffect.StarCombatRenderingManager;
import data.scripts.reflection.ReflectionUtilis;
import data.scripts.weapons.sikrsun_star_render;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;

public class FirstBattlegroupCampaingFleetViewMember extends CampaignFleetMemberView {

    ArrayList<CampaignStarData> stars = new ArrayList<>();
    float angle = 0f;

    public FirstBattlegroupCampaingFleetViewMember(
            CampaignFleet campaignFleet,
            FleetMember fleetMember
    ) {
        super(campaignFleet,fleetMember);
        collectStars(fleetMember);
    }

    public void collectStars(FleetMember fleetMember){
        stars.clear();

        if(fleetMember==null||fleetMember.getHullSpec()==null){
            return;
        }

        StarCombatRenderingData data =
                StarCombatRenderingManager.getDataFromList(
                        fleetMember.getHullId()
                );

        if(data==null&&fleetMember.getHullSpec().getBaseHullId()!=null){
            data = StarCombatRenderingManager.getDataFromList(
                    fleetMember.getHullSpec().getBaseHullId()
            );
        }

        if(data==null){
            return;
        }

        for (WeaponSlotAPI slot : fleetMember.getHullSpec().getAllWeaponSlotsCopy()) {
            if(slot==null||slot.getId()==null||data.getStarSlotData(slot.getId())==null){
                continue;
            }

            float size = data.getStarSlotData(slot.getId()).starSize;

            if(size<=0){
                continue;
            }

            stars.add(new CampaignStarData(slot.getId(),size));
        }
    }

    @Override
    public void advance(float amount, CampaignFleetView fleetView) {
        /*
         * CampaignFleetMemberView only uses fleetView here for contrails.
         * Passing null stops it from creating or updating any contrail.
         */
        super.advance(amount,null);

        if(amount>0){
            angle += 0.1f;

            if(angle>360){
                angle = 0;
            }
        }
    }

    @Override
    public void render(
            float lightAngle,
            Color lightColor,
            float alphaMult,
            float shadowAlphaMult
    ) {
        super.render(
                lightAngle,
                lightColor,
                alphaMult,
                shadowAlphaMult
        );

        if(stars==null||stars.isEmpty()||getMember()==null){
            return;
        }

        Vector2f shipLocation = getShipLocation();

        if(shipLocation==null){
            return;
        }

        float facing = getShipFacing();
        float scale = getScaleMult();
        float alpha = getAlpha(alphaMult);

        for (CampaignStarData star : stars) {
            Object slot = ReflectionUtilis.invokeMethodWithAutoProjection(
                    "getSlot",
                    getMember().getVariant(),
                    star.slotId
            );

            if(slot==null){
                continue;
            }

            Vector2f relativePosition;

            try{
                relativePosition = (Vector2f) ReflectionUtilis.invokeMethodWithAutoProjection(
                        "computeRelativePosition",
                        slot,
                        facing
                );
            }
            catch (Exception e){
                continue;
            }

            if(relativePosition==null){
                continue;
            }

            relativePosition = new Vector2f(relativePosition);
            relativePosition.scale(scale);

            Vector2f point = new Vector2f(
                    shipLocation.x+relativePosition.x,
                    shipLocation.y+relativePosition.y
            );

            sikrsun_star_render.renderPlanet(
                    "star_yellow",
                    point,
                    star.size,
                    facing,
                    3f,
                    angle,
                    0f,
                    alpha,
                    scale
            );
        }
    }

    public Vector2f getShipLocation(){
        Object originObject = ReflectionUtilis.getPrivateVariableFromSuperClass(
                "originLoc",
                this
        );

        if(!(originObject instanceof Vector2f)){
            return null;
        }

        Vector2f origin = (Vector2f) originObject;
        Vector2f movement = getMovementModule().getLocation();

        return new Vector2f(
                origin.x+movement.x,
                origin.y+movement.y
        );
    }

    public float getShipFacing(){
        if(getMember()!=null&&getMember().isStation()){
            return getFleet().getFacing();
        }

        Object facing = ReflectionUtilis.getPrivateVariableFromSuperClass(
                "facing",
                this
        );

        if(facing instanceof Number){
            return ((Number) facing).floatValue();
        }

        return getFleet().getFacing();
    }

    public float getAlpha(float alphaMult){
        float alpha = alphaMult*getExtraAlphaMult();

        Object fader = ReflectionUtilis.getPrivateVariableFromSuperClass(
                "fader",
                this
        );

        if(fader instanceof Fader){
            alpha *= ((Fader) fader).getBrightness();
        }

        return Math.max(0,Math.min(1,alpha));
    }

    public static class CampaignStarData {

        String slotId;
        float size;

        public CampaignStarData(String slotId, float size) {
            this.slotId = slotId;
            this.size = size;
        }
    }
}