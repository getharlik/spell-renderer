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
                Builder.rect().pos(15, 15, 10).radius(8).size(100, 75).color(Color.WHITE).submit();
                Builder.rect().pos(100, 15).radius(12, 0.6f).size(75, 100).color(Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK).submit();
                Builder.image(ResourceLocation.fromNamespaceAndPath(SpellRenderer.MOD_ID, "icon.png"))
                        .pos(200, 15).radius(24, 0.6f).size(128, 128).submit();
                Builder.text("Hello, World!").pos(10, 200).size(48).color(Color.WHITE).submit();
                Builder.glass()
                        .pos(272, 15)
                        .size(200, 120)
                        .radius(24, 0.6f)
                        .submit();
            });
        }
	}

    public static void bindGUIFramebuffer() { // Framebuffer 0 - right before mojang's swap, after evrth is drawn
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