package dev.harlik.mixin;

import java.util.List;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderState.class)
public interface GuiRenderStateAccessor {

    @Accessor("strata")
    List<?> spell$getStrata();

    /** Integer.MAX_VALUE when nothing asked for the menu blur this frame. */
    @Accessor("firstStratumAfterBlur")
    int spell$getFirstStratumAfterBlur();

    @Accessor("firstStratumAfterBlur")
    void spell$setFirstStratumAfterBlur(int stratum);
}
