package dev.harlik.api;

import dev.harlik.SpellRenderer;
import java.util.ArrayList;
import java.util.List;

public enum RenderContext {
    GUI,
    HUD,
    WORLD;

    private final List<Runnable> listeners = new ArrayList<>();

    public void register(Runnable listener) {
        listeners.add(listener);
    }

    public void fire() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Throwable t) {
                SpellRenderer.LOGGER.error("Listener in {} threw", this, t);
            }
        }
    }
}
