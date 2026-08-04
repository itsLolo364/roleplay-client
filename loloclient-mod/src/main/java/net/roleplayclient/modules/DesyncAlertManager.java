package net.roleplayclient.modules;

import net.minecraft.client.MinecraftClient;
import net.roleplayclient.RoleplayClientMod;

/**
 * Allerta desync: quando il server smette di rispondere ai keep-alive oltre
 * la soglia configurata, mostra un avviso persistente sull'HUD.
 * L'orario dell'ultimo keep-alive viene aggiornato su un thread di rete
 * (volatile), il controllo avviene nel tick del client.
 */
public final class DesyncAlertManager {
    private static volatile long lastKeepAliveMs = 0;

    private DesyncAlertManager() {
    }

    /** Chiamato dal mixin su onKeepAlive (thread di rete). */
    public static void onKeepAlive() {
        lastKeepAliveMs = System.currentTimeMillis();
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            lastKeepAliveMs = 0;
            return;
        }
    }

    public static boolean isAlerting() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || !RoleplayClientMod.config().isEnabled("desyncalert")) return false;
        if (lastKeepAliveMs == 0) return false;
        int threshold = RoleplayClientMod.config().getInt("desyncalert", "thresholdSeconds", 5);
        return System.currentTimeMillis() - lastKeepAliveMs > threshold * 1000L;
    }

    public static long sinceKeepAliveMs() {
        return lastKeepAliveMs == 0 ? -1 : System.currentTimeMillis() - lastKeepAliveMs;
    }
}
