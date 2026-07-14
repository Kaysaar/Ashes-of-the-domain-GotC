package data.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class sikrsun_grav_pool_ai implements ShipSystemAIScript{

    private static final int RANGE = 700;
    private static final int MIN_MISSILES = 2;
    private static final int MIN_FIGHTERS = 2;

    private ShipAPI ship;
    private ShipSystemAPI system;
    private boolean system_on;
    private boolean flux;
    private float missiles = 0;
    private float fighters = 0;
    private final IntervalUtil interval = new IntervalUtil (1f,1.5f);

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system; 
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        interval.advance(amount);
        if(interval.intervalElapsed()){
            if(flux){
                if(ship.getFluxLevel() <= 0.55f) flux = false;
                return;
            }
            if(!system.isActive() && system_on)system_on = false;
            if(!system.isActive() && AIUtils.canUseSystemThisFrame(ship)){
                if(ship.getFluxLevel() <= 0.8f){
                    missiles = 0;
                    fighters = 0;
                    for(CombatEntityAPI enemy_missile : AIUtils.getNearbyEnemyMissiles(ship, RANGE+100)){
                        if(enemy_missile != null){
                            if(MathUtils.getDistance(enemy_missile,ship) < RANGE){
                                missiles++;
                            }
                        }
                    }
                    for(ShipAPI enemy_ship : AIUtils.getNearbyEnemies(ship, RANGE+100)){
                        if(enemy_ship != null && !enemy_ship.isHulk() && enemy_ship.isFighter()){
                            if(MathUtils.getDistance(enemy_ship,ship) < RANGE){
                                fighters++;
                            }
                        }
                    }
                    if(missiles >= MIN_MISSILES || fighters >= MIN_FIGHTERS){
                        if(!system_on){
                            ship.useSystem();
                            system_on = true;
                        }
                        return;
                    }
                }else{
                    flux = true;
                }
                if(system_on){
                    ship.useSystem();
                    system_on = false;
                }
            }
        }
    }
}