package dev.harlik;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.harlik.api.RenderContext;
import dev.harlik.api.builder.Builder;
import dev.harlik.api.font.FontManager;
import dev.harlik.api.scale.Scale;
import dev.harlik.api.scissors.Scissors;
import dev.harlik.impl.GlState;
import dev.harlik.impl.batch.BatchManager;
import dev.harlik.impl.shader.ShaderManager;
import java.awt.Color;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.lwjgl.opengl.GL33.*;

public class SpellRenderer implements ClientModInitializer {

	public static final String MOD_ID = "spell-renderer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ShaderManager.INSTANCE.init();
        FontManager.INSTANCE.init();

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            SpellRenderer.on(RenderContext.GUI, () -> {
                Builder.rect()
                        .pos(10, 10, 2)
                        .size(128, 128)
                        .radius(16, 0.6f)
                        .color(Color.GRAY)
                        .submit();
                Builder.rect()
                        .pos(100, 100)
                        .size(96, 64)
                        .radius(12)
                        .color(Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK)
                        .submit();
                Builder.image(ResourceLocation.fromNamespaceAndPath(SpellRenderer.MOD_ID, "icon.png"))
                        .pos(180, 10)
                        .size(128, 128)
                        .radius(24, 0.6f)
                        .submit();
                Builder.glass()
                        .pos(260, 10)
                        .size(128, 128)
                        .radius(16)
                        .submit();
                Builder.text("Spell-renderer")
                        .pos(10, 200)
                        .size(48)
                        .color(Color.WHITE)
                        .submit();
                Builder.icon("backward")
                        .font("icons")
                        .size(32)
                        .pos(10, 256)
                        .submit();
                Builder.circle()
                        .pos(500, 50).radius(32).color(Color.BLACK).submit();
            });
        }
	}

    public static void bindGUIFramebuffer() { // Framebuffer 0 - right after mojang's swap, after evrth is drawn
        var window = Minecraft.getInstance().getWindow();
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
        GlStateManager._viewport(0, 0, window.getWidth(), window.getHeight());
    }

	public static void bindHUDFramebuffer() { // Framebuffer 3 - before mojang swaps the buffers, while evrth is drawing
		RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
		GlTexture colorTexture = (GlTexture) target.getColorTexture();
		GlDevice device = (GlDevice) RenderSystem.getDevice();
		int fbo = colorTexture.getFbo(device.directStateAccess(), target.getDepthTexture());
		GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, fbo);
		GlStateManager._viewport(0, 0, target.getColorTexture().getWidth(0), target.getColorTexture().getHeight(0));
	}

	public static void renderFrame(RenderContext context) {
		Scale.INSTANCE.updateMatrix();
        Scissors.clear();

		try (var ignored = GlState.capture()) {
            if (context.equals(RenderContext.GUI)) {
                bindGUIFramebuffer();
            } else {
                bindHUDFramebuffer();
            }

			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glDisable(GL_DEPTH_TEST);
			glDisable(GL_CULL_FACE);

			context.fire();
			BatchManager.INSTANCE.flushBatches(Scale.INSTANCE.getMatrix());
		}
	}

    public static void on(RenderContext context, Runnable listener) {
        context.register(listener);
    }
}