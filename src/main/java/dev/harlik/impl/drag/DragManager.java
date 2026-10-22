package dev.harlik.impl.drag;

import dev.harlik.api.RenderContext;
import dev.harlik.api.scale.Scale;
import dev.harlik.api.scissors.Region;
import dev.harlik.api.scissors.Scissors;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Tracks drag offsets for shapes marked with {@code .draggable(id)}.
 * Shapes are rebuilt every frame, so all state is keyed by the drag id.
 * Offsets are kept in memory only; use {@link #offsetX}/{@link #setOffset} to persist them yourself.
 */
public class DragManager {

    public static final DragManager INSTANCE = new DragManager();

    /** Distance the cursor must travel after a press before the drag starts. */
    private static final float THRESHOLD = 3f;

    private enum State { IDLE, ARMED, DRAGGING }

    private record Handle(Region region, float z) {}

    private static final class Registration {
        final List<Handle> handles = new ArrayList<>();
        boolean hasSelf;
        float baseX, baseY, selfW, selfH;
    }

    private final Map<String, float[]> offsets = new HashMap<>();
    private final Map<RenderContext, Map<String, Registration>> building = new EnumMap<>(RenderContext.class);
    private final Map<RenderContext, Map<String, Registration>> committed = new EnumMap<>(RenderContext.class);
    private final Map<RenderContext, Long> committedAt = new EnumMap<>(RenderContext.class);
    private long frame;
    private RenderContext currentContext;

    private State state = State.IDLE;
    @Getter
    private String activeId;
    private RenderContext activeContext;
    private float pressX, pressY, grabDx, grabDy;

    /** Starts a render pass: last pass's registrations become hit-testable, new ones start collecting. */
    public void beginFrame(RenderContext context) {
        if (context == RenderContext.GUI) frame++;
        currentContext = context;
        committed.put(context, building.getOrDefault(context, new HashMap<>()));
        committedAt.put(context, frame);
        building.put(context, new HashMap<>());
    }

    /** Registers a shape's box under a drag id. Called from {@code Shape.submit()}. */
    public void register(String id, Region bounds, float z, boolean isTagged) {
        if (currentContext == null) return;
        Registration reg = building.get(currentContext).computeIfAbsent(id, k -> new Registration());

        Region clip = Scissors.current();
        Region handle = clip != null ? Region.intersect(bounds, clip) : bounds;
        if (handle.w() > 0 && handle.h() > 0) reg.handles.add(new Handle(handle, z));

        if (isTagged) {
            reg.hasSelf = true;
            reg.baseX = bounds.x() - offsetX(id);
            reg.baseY = bounds.y() - offsetY(id);
            reg.selfW = bounds.w();
            reg.selfH = bounds.h();
        }
    }

    /** Handles a mouse button event. Returns true if the event should be consumed. */
    public boolean onPress(int button, int action) {
        if (button != GLFW_MOUSE_BUTTON_LEFT) return false;

        if (action == GLFW_RELEASE) {
            boolean consumed = state != State.IDLE;
            stop();
            return consumed;
        }
        if (action != GLFW_PRESS) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.mouseHandler.isMouseGrabbed()) return false;

        float mx = mouseX(), my = mouseY();
        RenderContext context = RenderContext.GUI;
        String hit = hitTest(context, mx, my);
        if (hit == null && mc.screen instanceof ChatScreen && isFresh(RenderContext.HUD)) {
            context = RenderContext.HUD;
            hit = hitTest(context, mx, my);
        }
        if (hit == null) return false;

        state = State.ARMED;
        activeId = hit;
        activeContext = context;
        pressX = mx;
        pressY = my;
        grabDx = mx - offsetX(hit);
        grabDy = my - offsetY(hit);
        return true;
    }

    /** Moves the active drag to follow the cursor. Called every render pass. */
    public void update() {
        if (state == State.IDLE) return;

        Minecraft mc = Minecraft.getInstance();
        if (glfwGetMouseButton(mc.getWindow().getWindow(), GLFW_MOUSE_BUTTON_LEFT) != GLFW_PRESS) {
            stop();
            return;
        }

        Registration reg = committed.get(activeContext).get(activeId);
        if (reg == null) {
            stop();
            return;
        }

        float mx = mouseX(), my = mouseY();
        if (state == State.ARMED) {
            if (Math.hypot(mx - pressX, my - pressY) <= THRESHOLD) return;
            state = State.DRAGGING;
        }

        float ox = mx - grabDx, oy = my - grabDy;
        if (reg.hasSelf) {
            ox = clamp(ox, -reg.baseX, Scale.INSTANCE.getViewportWidth() - reg.selfW - reg.baseX);
            oy = clamp(oy, -reg.baseY, Scale.INSTANCE.getViewportHeight() - reg.selfH - reg.baseY);
        }
        setOffset(activeId, ox, oy);
    }

    public float offsetX(String id) {
        float[] offset = offsets.get(id);
        return offset != null ? offset[0] : 0;
    }

    public float offsetY(String id) {
        float[] offset = offsets.get(id);
        return offset != null ? offset[1] : 0;
    }

    public void setOffset(String id, float x, float y) {
        offsets.put(id, new float[]{x, y});
    }

    public void reset(String id) {
        offsets.remove(id);
    }

    public boolean isDragging() {
        return state == State.DRAGGING;
    }

    private String hitTest(RenderContext context, float mx, float my) {
        Map<String, Registration> regs = committed.get(context);
        if (regs == null) return null;

        String best = null;
        float bestZ = Float.NEGATIVE_INFINITY;
        for (var entry : regs.entrySet()) {
            for (Handle handle : entry.getValue().handles) {
                if (handle.z() > bestZ && handle.region().inBounds(mx, my)) {
                    best = entry.getKey();
                    bestZ = handle.z();
                }
            }
        }
        return best;
    }

    /** HUD isn't rendered without a level, so its registrations can go stale. */
    private boolean isFresh(RenderContext context) {
        Long at = committedAt.get(context);
        return at != null && at >= frame - 1;
    }

    private void stop() {
        state = State.IDLE;
        activeId = null;
        activeContext = null;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    // MouseHandler reports window coordinates; shape space is the framebuffer divided by Scale.
    private static float mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return (float) (mc.mouseHandler.xpos() / mc.getWindow().getScreenWidth() * Scale.INSTANCE.getViewportWidth());
    }

    private static float mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return (float) (mc.mouseHandler.ypos() / mc.getWindow().getScreenHeight() * Scale.INSTANCE.getViewportHeight());
    }
}
