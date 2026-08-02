package net.roleplayclient;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Conteggio click al secondo (finestra mobile di 1 secondo).
 */
public class ClickCounter {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    public static void registerLeft() {
        register(LEFT, System.currentTimeMillis());
    }

    public static void registerRight() {
        register(RIGHT, System.currentTimeMillis());
    }

    public static int getLeft() {
        return count(LEFT);
    }

    public static int getRight() {
        return count(RIGHT);
    }

    private static void register(Deque<Long> d, long now) {
        d.addLast(now);
        prune(d, now);
    }

    private static int count(Deque<Long> d) {
        long now = System.currentTimeMillis();
        prune(d, now);
        return d.size();
    }

    private static void prune(Deque<Long> d, long now) {
        while (!d.isEmpty() && now - d.peekFirst() > 1000) d.pollFirst();
    }
}
