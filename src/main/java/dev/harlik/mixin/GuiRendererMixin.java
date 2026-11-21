package dev.harlik.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.harlik.SpellRenderer;
import dev.harlik.api.RenderContext;
import dev.harlik.impl.HudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the HUD context inside vanilla's GUI draw, between the vanilla HUD and the open screen.
 * See {@link HudLayer}.
 */
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @Shadow @Final private GuiRenderState renderState;

    @Unique private boolean spell$hudRendered;
    @Unique private boolean spell$splitWithoutBlur;

    /**
     * Screens that don't blur (inventories, chat, or any screen with menu blur set to 0) leave the GUI unsplit,
     * which would put the HUD context on top of them. Splits at the screen's first stratum instead,
     * the same place vanilla splits for a blurring screen; the blur itself is skipped below.
     */
    @Inject(method = "prepare", at = @At("HEAD"))
    private void spell$splitBeforeScreen(CallbackInfo ci) {
        var state = (GuiRenderStateAccessor) renderState;
        spell$splitWithoutBlur = false;
        if (state.spell$getFirstStratumAfterBlur() == Integer.MAX_VALUE
                && HudLayer.screenStratum >= 0
                && Minecraft.getInstance().level != null) {
            state.spell$setFirstStratumAfterBlur(HudLayer.screenStratum);
            spell$splitWithoutBlur = true;
        }
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void spell$beginDraw(CallbackInfo ci) {
        spell$hudRendered = false;
    }

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"))
    private void spell$hudBeforeScreen(CallbackInfo ci) {
        spell$renderHud();
    }

    @WrapWithCondition(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"))
    private boolean spell$skipBlurForOwnSplit(GameRenderer gameRenderer) {
        return !spell$splitWithoutBlur;
    }

    /** draw() returns early when there is nothing after the split, or nothing at all (F1). */
    @Inject(method = "draw", at = @At("RETURN"))
    private void spell$hudFallback(CallbackInfo ci) {
        spell$renderHud();
    }

    @Unique
    private void spell$renderHud() {
        if (spell$hudRendered || Minecraft.getInstance().level == null) return;
        spell$hudRendered = true;
        SpellRenderer.renderFrame(RenderContext.HUD);
    }
}
