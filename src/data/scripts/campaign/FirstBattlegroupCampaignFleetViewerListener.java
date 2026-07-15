package data.scripts.campaign;

import ashlib.data.scripts.CustomCampaignViewerListener;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.CampaignFleetMemberView;
import com.fs.starfarer.campaign.fleet.FleetMember;

public class FirstBattlegroupCampaignFleetViewerListener implements CustomCampaignViewerListener {
    @Override
    public CampaignFleetMemberView generateCampaignFleetViewer(CampaignFleet campaignFleet, FleetMember fleetMember) {
        if(FirstBattlegroupCampaingFleetViewMember.hasStarData(fleetMember)){
            return new FirstBattlegroupCampaingFleetViewMember(campaignFleet, fleetMember);
        }
        return null;
    }
}
