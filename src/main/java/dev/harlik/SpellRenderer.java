package dev.harlik;

import dev.harlik.api.RenderContext;
import dev.harlik.api.builder.Builder;
import dev.harlik.api.builder.shapes.ShapeGlass;
import dev.harlik.api.font.FontManager;
import dev.harlik.api.scale.Scale;
import dev.harlik.api.scissors.Region;
import dev.harlik.api.scissors.Scissors;
import dev.harlik.impl.batch.BatchManager;
import dev.harlik.impl.drag.DragManager;
import dev.harlik.impl.gpu.SpellFrame;
import dev.harlik.impl.gpu.SpellPipelines;
import java.awt.Color;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpellRenderer implements ClientModInitializer {

	public static final String MOD_ID = "spell-renderer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> SpellPipelines.precompileAll());
        FontManager.INSTANCE.init();
	}

    /** Dev only: a gradient backdrop with text, the glass overlapping its bottom edge, and a label. */
    private static void glassTest(float x, String label, ShapeGlass glass) {
        var backdrop = Builder.rect()
                .pos(x, 300)
                .size(130, 90)
                .radius(8)
                .color(Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN);
        backdrop.submit();
        Builder.text("Aa")
                .pos(12, 16, 1)
                .size(48)
                .color(Color.BLACK)
                .parent(backdrop)
                .submit();
        glass.pos(15, 45, 2)
                .size(100, 70)
                .radius(14)
                .parent(backdrop)
                .submit();
        Builder.text(label)
                .pos(0, 122, 1)
                .size(16)
                .color(Color.WHITE)
                .parent(backdrop)
                .submit();
    }

    /** Dev only: snaps the dragged panel to the screen center and to the left/top edges of other panels. */
    private static Region snapTest(String id, Region proposed) {
        float x = proposed.x(), y = proposed.y();
        float centerX = Scale.INSTANCE.getViewportWidth() / 2, centerY = Scale.INSTANCE.getViewportHeight() / 2;

        if (Math.abs(x + proposed.w() / 2 - centerX) < 8) x = centerX - proposed.w() / 2;
        if (Math.abs(y + proposed.h() / 2 - centerY) < 8) y = centerY - proposed.h() / 2;

        for (var other : DragManager.INSTANCE.getAllBounds().entrySet()) {
            if (other.getKey().equals(id)) continue;
            Region b = other.getValue();
            if (Math.abs(x - b.x()) < 8) x = b.x();
            if (Math.abs(y - b.y()) < 8) y = b.y();
        }
        return new Region(x, y, proposed.w(), proposed.h());
    }

    /** Dev only: dims the screen under the dragged panel and draws a guide for every line it snapped to.
     *  Drawn in the drag's own context, so a GUI dim never covers a HUD panel. */
    private static void dragOverlayTest(RenderContext context) {
        Region active = DragManager.INSTANCE.getActiveBounds();
        if (active == null || DragManager.INSTANCE.getActiveContext() != context) return;

        float w = Scale.INSTANCE.getViewportWidth(), h = Scale.INSTANCE.getViewportHeight();
        Builder.rect()
                .pos(0, 0, 4) // below the panels (z 5), above everything else
                .size(w, h)
                .color(new Color(0, 0, 0, 120))
                .submit();

        if (Math.abs(active.x() + active.w() / 2 - w / 2) < 0.5f) guideTest(w / 2, 0, 1, h);
        if (Math.abs(active.y() + active.h() / 2 - h / 2) < 0.5f) guideTest(0, h / 2, w, 1);

        for (var other : DragManager.INSTANCE.getAllBounds().entrySet()) {
            if (other.getKey().equals(DragManager.INSTANCE.getActiveId())) continue;
            Region b = other.getValue();
            if (Math.abs(active.x() - b.x()) < 0.5f) guideTest(b.x(), 0, 1, h);
            if (Math.abs(active.y() - b.y()) < 0.5f) guideTest(0, b.y(), w, 1);
        }
    }

    private static void guideTest(float x, float y, float w, float h) {
        Builder.rect()
                .pos(x, y, 10)
                .size(w, h)
                .color(Color.CYAN)
                .submit();
    }

	public static void renderFrame(RenderContext context) {
		Scale.INSTANCE.updateMatrix();
        Scissors.clear();
        DragManager.INSTANCE.beginFrame(context);
        DragManager.INSTANCE.update();

        context.fire();
        try (SpellFrame frame = SpellFrame.begin(context)) {
            BatchManager.INSTANCE.flushBatches(frame, Scale.INSTANCE.getMatrix());
        }
	}

    public static void on(RenderContext context, Runnable listener) {
        context.register(listener);
    }
}