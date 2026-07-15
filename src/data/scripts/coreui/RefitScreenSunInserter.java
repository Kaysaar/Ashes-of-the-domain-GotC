package data.scripts.coreui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.combat.CombatViewport;
import com.fs.starfarer.combat.entities.Ship;
import data.scripts.reflection.ReflectionUtilis;

import data.scripts.weapons.sikrsun_star_render;

public class RefitScreenSunInserter implements EveryFrameScript {
    @Override
    public boolean isDone() {
        return false;
    }
    SunRedneringUI render;
    RefitScreenZoomIntercepter intercepter;
    @Override
    public boolean runWhilePaused() {
        return true;
    }
    ShipAPI lastSaved;
    public static float lastSavedZoom = 1f;
    public static boolean haveSavedStatusOfZoom = false;
    @Override
    public void advance(float amount) {
        if(CoreUITabId.REFIT.equals(Global.getSector().getCampaignUI().getCurrentCoreTab())){
            UIPanelAPI current = ReflectionUtilis.getCurrentTab();
            if(current!=null){
                UIPanelAPI refitPanel = (UIPanelAPI) ReflectionUtilis.invokeMethodWithAutoProjection("getRefitPanel",current);
                FleetMemberAPI member = (FleetMemberAPI) ReflectionUtilis.invokeMethodWithAutoProjection("getMember",refitPanel);
                    UIPanelAPI shipDisplay = (UIPanelAPI) ReflectionUtilis.invokeMethodWithAutoProjection("getShipDisplay",refitPanel);
                    ShipAPI ship = (ShipAPI) ReflectionUtilis.invokeMethodWithAutoProjection("getShip",shipDisplay);
                    if(ship!=null&&intercepter==null){
                        RefitScreenSunInserter.lastSavedZoom = 1f;
                        intercepter = new RefitScreenZoomIntercepter(1,1,ship,shipDisplay);
                        refitPanel.addComponent(intercepter.getMainPanel()).inTL(0,0);
                    }
                    WeaponAPI weapon = ship.getAllWeapons().stream().filter(x->x.getSlot().getId().equals("SUN")).findFirst().orElse(null);
                    if(!ship.equals(lastSaved)){
                        lastSaved = ship;
                        if(render!=null){
                            refitPanel.removeComponent(render.getMainPanel());
                            render = null;
                        }
                    }
                    if(weapon!=null){
                        if(render==null){
                            render = new SunRedneringUI(0,0,lastSaved,weapon,shipDisplay);
                            haveSavedStatusOfZoom = true;
                            refitPanel.addComponent(render.getMainPanel()).inTL(0,0);
                        }



                    }

                else{
                    if(render!=null){
                        refitPanel.removeComponent(render.getMainPanel());
                    }

                    render= null;
                }
            }
        }
        else{
            intercepter = null;
            haveSavedStatusOfZoom  = false;
            lastSaved = null;
            render= null;
        }
    }
}
