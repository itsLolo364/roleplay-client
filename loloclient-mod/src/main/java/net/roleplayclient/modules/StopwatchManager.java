package net.roleplayclient.modules;

/**
 * Cronometro RP: un tasto alterna avvio/pausa; una doppia pressione ravvicinata
 * mentre è in pausa azzera. La durata è mostrata sull'HUD dal ModuleHudRenderer.
 */
public final class StopwatchManager {
    private static final long RESET_WINDOW_MS = 600;

    private static long startMs = -1;
    private static long accumulatedMs = 0;
    private static long lastToggleMs = 0;

    private StopwatchManager() {
    }

    public static void toggle() {
        long now = System.currentTimeMillis();
        if (startMs >= 0) {
            // In esecuzione: metti in pausa e memorizza il momento per il reset.
            accumulatedMs += now - startMs;
            startMs = -1;
            lastToggleMs = now;
        } else {
            if (now - lastToggleMs < RESET_WINDOW_MS) {
                accumulatedMs = 0;
                lastToggleMs = 0;
            } else {
                startMs = now;
            }
        }
    }

    public static void reset() {
        startMs = -1;
        accumulatedMs = 0;
        lastToggleMs = 0;
    }

    public static boolean running() {
        return startMs >= 0;
    }

    public static long elapsedMs() {
        long base = accumulatedMs;
        if (startMs >= 0) base += System.currentTimeMillis() - startMs;
        return base;
    }

    public static String format(long ms) {
        long total = ms / 1000;
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", total / 3600, (total / 60) % 60, total % 60);
    }
}
