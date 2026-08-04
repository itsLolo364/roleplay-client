package net.roleplayclient.modules;

import net.minecraft.client.MinecraftClient;

/**
 * Tempo di sessione: accumula il tempo di gioco (connessi a un mondo) e lo
 * espone all'HUD. Si azzera alla disconnessione.
 */
public final class SessionTimeManager {
    private static long sessionMs = 0;
    private static long lastTick = 0;

    private SessionTimeManager() {
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        if (client.world == null) {
            sessionMs = 0;
            lastTick = 0;
            return;
        }
        if (lastTick == 0) {
            lastTick = now;
            return;
        }
        sessionMs += now - lastTick;
        lastTick = now;
    }

    public static long sessionMs() {
        return sessionMs;
    }

    public static String format(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total / 60) % 60;
        long s = total % 60;
        if (h > 0) return String.format(java.util.Locale.ROOT, "%dh %02dm", h, m);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", m, s);
    }
}
