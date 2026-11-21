package dev.harlik.mixin;

import dev.harlik.impl.HudLayer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow @Final private GuiRenderState guiRenderState;

    @Inject(method = "render", at = @At("HEAD"))
    private void spell$resetScreenStratum(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        HudLayer.screenStratum = -1;
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void spell$markScreenStratum(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        HudLayer.screenStratum = ((GuiRenderStateAccessor) guiRenderState).spell$getStrata().size();
    }
}
