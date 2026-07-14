package data.shipsystems.scripts;

import java.util.List;

import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class sikrsun_grav_pool extends BaseShipSystemScript {

	private static int RANGE = 700;
	private static Vector2f grav_strength = new Vector2f(50,50);

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {


		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

		if(state == State.ACTIVE){
			List<ShipAPI> targets = AIUtils.getNearbyEnemies(ship, RANGE);
			for(ShipAPI target : targets){
				if(target.getHullSize() != HullSize.FIGHTER) continue;
				Vector2f.add(target.getVelocity(), VectorUtils.rotate(grav_strength, VectorUtils.getAngle(target.getLocation(), ship.getLocation())), target.getVelocity());
				// if(target.getVelocity().length() > target.getMaxSpeed()){
				// 	target.getVelocity().scale(target.getMaxSpeed() / target.getVelocity().length());
				// }
					//target.getMutableStats().getTimeMult().modifyMult(id, 0.6f);
			}
			List<MissileAPI> missiles = AIUtils.getNearbyEnemyMissiles(ship, RANGE);
			for(MissileAPI missile : missiles){
				Vector2f.add(missile.getVelocity(), VectorUtils.rotate(grav_strength, VectorUtils.getAngle(missile.getLocation(), ship.getLocation())), missile.getVelocity());
				// if(missile.getVelocity().length() > missile.getMaxSpeed() * 2){
				// 	missile.getVelocity().scale(missile.getMaxSpeed() * 2 / missile.getVelocity().length());
				// }
			}
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Gravity Pool : ON", false);
		}
		return null;
	}
}