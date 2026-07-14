package data.scripts.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.effects.bendingeffect.BendingEffectHandler;

import java.util.List;

public class EffectsInitializer implements EveryFrameCombatPlugin {

    public static BendingEffectHandler BendingEffectHandler = null;

    @Override
    public void init(CombatEngineAPI engine) {
        if (BendingEffectHandler != null) {
            BendingEffectHandler.dispose();
            BendingEffectHandler = null;
        }

        BendingEffectHandler = new BendingEffectHandler();
        Global.getCombatEngine().addLayeredRenderingPlugin(BendingEffectHandler);
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}

    @Override
    public void advance(float amount, List<InputEventAPI> events) {}

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}

    @Override
    public void renderInUICoords(ViewportAPI viewport) {}

}
