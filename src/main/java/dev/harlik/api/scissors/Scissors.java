package dev.harlik.api.scissors;

import dev.harlik.SpellRenderer;
import java.util.ArrayDeque;
import java.util.Deque;

public final class Scissors {

    private static final Deque<Region> STACK = new ArrayDeque<>();

    public static void push(float x, float y, float w, float h) {
        Region submitted = new Region(x, y, w, h);
        Region top = STACK.peek();
        STACK.push(top == null ? submitted : Region.intersect(top, submitted));
    }

    public static void pop() {
        if (STACK.isEmpty()) {
            SpellRenderer.LOGGER.error("No active scissors to pop.");
            return;
        }
        STACK.pop();
    }

    public static void clear() {
        STACK.clear();
    }

    public static Region current() {
        return STACK.peek();
    }

    public static int depth() {
        return STACK.size();
    }

    public static void truncateTo(int target) {
        while (STACK.size() > target) STACK.pop();
    }

}
