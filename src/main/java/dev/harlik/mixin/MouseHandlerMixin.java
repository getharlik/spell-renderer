package dev.harlik.mixin;

import dev.harlik.impl.drag.DragManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void spell$onPress(long window, int button, int action, int mods, CallbackInfo ci) {
        if (DragManager.INSTANCE.onPress(button, action)) ci.cancel();
    }
}
