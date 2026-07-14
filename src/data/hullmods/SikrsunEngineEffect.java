package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.effects.EffectsInitializer;
import data.scripts.effects.bendingeffect.BendingInstance;
import data.scripts.effects.bendingeffect.BendingShipEffectData;
import data.scripts.effects.bendingeffect.BendingEffectDataHandler;

import java.util.ArrayList;
import java.util.List;

public class SikrsunEngineEffect extends BaseHullMod {

    @Override
    public void applyEffectsAfterShipAddedToCombatEngine(ShipAPI ship, String id) {
        if (ship == null || ship.getHullSpec() == null) {
            return;
        }

        String hullId = ship.getHullSpec().getHullId();

        BendingShipEffectData shipEffectData =
                BendingEffectDataHandler.getDataFromList(hullId);

        if (shipEffectData == null
                || shipEffectData.effects == null
                || shipEffectData.effects.isEmpty()) {
            return;
        }

        List<BendingInstance> instances =
                new ArrayList<>(shipEffectData.effects.size());

        for (BendingShipEffectData.BendingEffectData effectData
                : shipEffectData.effects) {

            if (effectData == null || effectData.coordinates == null) {
                continue;
            }

            BendingInstance instance = new BendingInstance()
                    .setShip(ship)
                    .setSize(effectData.size)
                    .updateOffset(
                            effectData.coordinates.x,
                            effectData.coordinates.y
                    )
                    .setRadius(effectData.radius)
                    .setStrength(effectData.maxStrength);

            instances.add(instance);
        }

        if (!instances.isEmpty()) {
            EffectsInitializer.BendingEffectHandler.addInstances(instances);
        }
    }
}