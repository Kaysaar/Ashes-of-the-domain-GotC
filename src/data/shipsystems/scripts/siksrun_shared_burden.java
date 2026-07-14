package data.shipsystems.scripts;

import java.util.ArrayList;

import org.lazywizard.lazylib.combat.AIUtils;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class siksrun_shared_burden extends BaseShipSystemScript {

    private static final int RANGE = 800;
    private static final float SLOW = 0.1f; //in percent lost
    private static final String stat_id = "sikrsun_burden";

    private int slowed_enemy = 0;

    private ArrayList<ShipAPI> targets = new ArrayList<ShipAPI>();

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

        for(ShipAPI target : AIUtils.getNearbyEnemies(ship, RANGE)){
            if(target.getHullSize() != HullSize.FIGHTER){
                target.getMutableStats().getMaxSpeed().modifyMult(stat_id, 1 - SLOW);
                targets.add(target);
                slowed_enemy++;
            }
        }
        float speed_bonus = slowed_enemy * SLOW < 2 ? slowed_enemy * SLOW : 2;
        ship.getMutableStats().getMaxSpeed().modifyMult(id, speed_bonus);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {

        ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

        for(ShipAPI target : targets){
            target.getMutableStats().getMaxSpeed().unmodify(stat_id);
            targets.remove(target);
        }

        ship.getMutableStats().getMaxSpeed().unmodify(id);
        
	}
    
}
