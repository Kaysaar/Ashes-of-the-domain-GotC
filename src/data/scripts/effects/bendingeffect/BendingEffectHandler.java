package data.scripts.effects.bendingeffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.effects.Shader;
import data.scripts.effects.ShaderUniformManager;
import org.apache.log4j.Level;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.util.vector.Vector2f;

import javax.management.InstanceNotFoundException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class BendingEffectHandler extends BaseCombatLayeredRenderingPlugin {

    private int screenTextureID = -1;
    private Shader shader = null;
    private float t;
    private List<BendingInstance> instances = new ArrayList<>();
    private int windowWidth;
    private int windowHeight;

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.UNDER_SHIPS_LAYER);
    }

    private void initResources() {
        windowWidth = (int) Global.getSettings().getScreenWidthPixels();
        windowHeight = (int) Global.getSettings().getScreenWidthPixels();
        GL11.glGetError(); // clear the error cache (you need to do this in a loop actually as its a stack, but 1 call is enough)
        screenTextureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenTextureID);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                windowWidth, windowHeight, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        int err = GL11.glGetError();
        if (err != GL11.GL_NO_ERROR) {
            Global.getLogger(BendingInstance.class).log(Level.ERROR, new OpenGLException("Error after trying to create texture\n error: " + err));
        }

        shader = Shader.fromFile("data/vfx/shaders/main.vert", "data/vfx/shaders/bending.frag");
    }

    public BendingEffectHandler() {
        this.layer = CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER;
        initResources();
    }

    @Override
    public void advance(float amount) {
        t += amount;
        this.instances.removeIf(BendingInstance::shouldRemove);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        GL11.glPushMatrix();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        shader.bind();
        ShaderUniformManager manager = shader.getUniformManager();
        manager // as opengl is a state machine, we dont need to constantly update this as it just stay as is
                .setTexture("textureSampler", this.screenTextureID, GL13.GL_TEXTURE0);

        for (BendingInstance instance : instances) {
            updateTexture();
            instance.render(viewport, manager);
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        shader.unbind();

        GL11.glPopMatrix();
    }

    /// must be ran before rendering
    private void updateTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.screenTextureID);

        GL11.glCopyTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0, 0,
                0, 0,
                windowWidth, windowHeight
        );
    }

    public BendingEffectHandler addInstance(BendingInstance ins) {
        this.instances.add(ins);
        return this;
    }
    public BendingEffectHandler addInstances(List<BendingInstance> insts) {
        this.instances.addAll(insts);
        return this;
    }

    public void dispose() {
        for (BendingInstance instance : instances)
            instance.dispose();
        GL11.glDeleteTextures(screenTextureID);
        shader.dispose();
    }

    @Override
    public float getRenderRadius() {
        return Float.MAX_VALUE;
    }

    @Override
    public boolean isExpired() {
        return false;
    }


}
