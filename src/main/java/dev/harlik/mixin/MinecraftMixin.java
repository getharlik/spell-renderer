package dev.harlik.mixin;

import dev.harlik.SpellRenderer;
import dev.harlik.api.RenderContext;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    /** Right before the finished frame is copied to the window, on top of everything vanilla drew. */
    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"))
    private void spell$guiRender(boolean bl, CallbackInfo ci) {
        SpellRenderer.renderFrame(RenderContext.GUI);
    }
}
