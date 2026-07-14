package data.scripts.effects.bendingeffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.effects.Shader;
import data.scripts.effects.ShaderUniformManager;
import org.apache.log4j.Level;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.util.vector.Vector2f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class BendingEffectHandler
        extends BaseCombatLayeredRenderingPlugin {

    private int screenTextureID = -1;

    private Shader shader;

    private final List<BendingInstance> instances =
            new ArrayList<>();

    /*
     * Reused lists prevent new ArrayList allocations every frame.
     */
    private final List<BendingInstance> visibleInstances =
            new ArrayList<>();

    private final List<BendingInstance> renderedInstances =
            new ArrayList<>();

    private int windowWidth;
    private int windowHeight;

    public BendingEffectHandler() {
        this.layer = CombatEngineLayers.UNDER_SHIPS_LAYER;
        initResources();
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(
                CombatEngineLayers.UNDER_SHIPS_LAYER
        );
    }

    private void initResources() {
        updateWindowDimensions();
        createScreenTexture();

        shader = Shader.fromFile(
                "data/vfx/shaders/main.vert",
                "data/vfx/shaders/bending.frag"
        );
    }

    private void updateWindowDimensions() {
        windowWidth = Math.max(
                1,
                Math.round(
                        Global.getSettings()
                                .getScreenWidthPixels()
                )
        );

        windowHeight = Math.max(
                1,
                Math.round(
                        Global.getSettings()
                                .getScreenHeightPixels()
                )
        );
    }

    private void createScreenTexture() {
        if (screenTextureID != -1) {
            GL11.glDeleteTextures(screenTextureID);
            screenTextureID = -1;
        }

        clearOpenGLErrors();

        screenTextureID = GL11.glGenTextures();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                screenTextureID
        );

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                windowWidth,
                windowHeight,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_LINEAR
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MAG_FILTER,
                GL11.GL_LINEAR
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_S,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T,
                GL12.GL_CLAMP_TO_EDGE
        );

        int error = GL11.glGetError();

        if (error != GL11.GL_NO_ERROR) {
            Global.getLogger(
                    BendingEffectHandler.class
            ).log(
                    Level.ERROR,
                    new OpenGLException(
                            "Could not create bending framebuffer "
                                    + "texture. OpenGL error: "
                                    + error
                    )
            );
        }

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                0
        );
    }

    private void ensureCorrectTextureSize() {
        int currentWidth = Math.max(
                1,
                Math.round(
                        Global.getSettings()
                                .getScreenWidthPixels()
                )
        );

        int currentHeight = Math.max(
                1,
                Math.round(
                        Global.getSettings()
                                .getScreenHeightPixels()
                )
        );

        if (currentWidth == windowWidth
                && currentHeight == windowHeight) {
            return;
        }

        windowWidth = currentWidth;
        windowHeight = currentHeight;

        createScreenTexture();
    }

    private void clearOpenGLErrors() {
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {
            // Clear all previously queued OpenGL errors.
        }
    }

    @Override
    public void advance(float amount) {
        instances.removeIf(
                instance ->
                        instance == null
                                || instance.shouldRemove()
        );
    }

    @Override
    public void render(
            CombatEngineLayers layer,
            ViewportAPI viewport
    ) {
        if (instances.isEmpty()
                || viewport == null
                || shader == null
                || screenTextureID == -1) {
            return;
        }

        ensureCorrectTextureSize();
        collectVisibleInstances(viewport);

        /*
         * Avoid even copying the framebuffer when every effect is
         * outside the viewport.
         */
        if (visibleInstances.isEmpty()) {
            return;
        }

        GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT
                        | GL11.GL_TEXTURE_BIT
                        | GL11.GL_COLOR_BUFFER_BIT
                        | GL11.GL_CURRENT_BIT
        );

        GL11.glPushMatrix();

        try {
            /*
             * Base capture used by every visible instance that does not
             * need to see a previously rendered bending effect.
             */
            updateTexture();

            GL11.glEnable(GL11.GL_TEXTURE_2D);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(
                    GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE_MINUS_SRC_ALPHA
            );

            GL11.glDisable(GL11.GL_ALPHA_TEST);

            GL11.glColor4f(
                    1f,
                    1f,
                    1f,
                    1f
            );

            shader.bind();

            ShaderUniformManager manager =
                    shader.getUniformManager();

            manager
                    .setTexture(
                            "textureSampler",
                            screenTextureID,
                            GL13.GL_TEXTURE0
                    )
                    .setVector2(
                            "texelSize",
                            new Vector2f(
                                    1f / windowWidth,
                                    1f / windowHeight
                            )
                    );

            renderedInstances.clear();

            for (BendingInstance instance
                    : visibleInstances) {

                /*
                 * The initial framebuffer capture is already sufficient
                 * for the first visible instance.
                 */
                if (!renderedInstances.isEmpty()
                        && requiresNewBackBuffer(instance)) {
                    updateTexture();
                }

                instance.render(
                        viewport,
                        manager,
                        windowWidth,
                        windowHeight
                );

                renderedInstances.add(instance);
            }
        } finally {
            if (shader != null) {
                shader.unbind();
            }

            GL13.glActiveTexture(
                    GL13.GL_TEXTURE0
            );

            GL11.glBindTexture(
                    GL11.GL_TEXTURE_2D,
                    0
            );

            GL11.glPopMatrix();
            GL11.glPopAttrib();

            renderedInstances.clear();
        }
    }

    private void collectVisibleInstances(
            ViewportAPI viewport
    ) {
        visibleInstances.clear();

        for (BendingInstance instance : instances) {
            if (instance == null
                    || instance.shouldRemove()) {
                continue;
            }

            if (instance.isVisible(
                    viewport,
                    windowWidth,
                    windowHeight
            )) {
                visibleInstances.add(instance);
            }
        }
    }

    /**
     * A new framebuffer capture is needed when explicitly requested or
     * when this effect overlaps an effect already rendered this frame.
     */
    private boolean requiresNewBackBuffer(
            BendingInstance instance
    ) {
        if (instance.needsBackBufferUpdate()) {
            return true;
        }

        for (BendingInstance renderedInstance
                : renderedInstances) {

            if (instance.overlaps(renderedInstance)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Captures the current framebuffer into the texture sampled by the
     * bending shader.
     */
    private void updateTexture() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                screenTextureID
        );

        GL11.glCopyTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                0,
                0,
                windowWidth,
                windowHeight
        );
    }

    public BendingEffectHandler addInstance(
            BendingInstance instance
    ) {
        if (instance != null) {
            instances.add(instance);
        }

        return this;
    }

    public BendingEffectHandler addInstances(
            List<BendingInstance> newInstances
    ) {
        if (newInstances == null
                || newInstances.isEmpty()) {
            return this;
        }

        for (BendingInstance instance
                : newInstances) {

            if (instance != null) {
                instances.add(instance);
            }
        }

        return this;
    }

    public void dispose() {
        for (BendingInstance instance : instances) {
            if (instance != null) {
                instance.dispose();
            }
        }

        instances.clear();
        visibleInstances.clear();
        renderedInstances.clear();

        if (screenTextureID != -1) {
            GL11.glDeleteTextures(screenTextureID);
            screenTextureID = -1;
        }

        if (shader != null) {
            shader.dispose();
            shader = null;
        }
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