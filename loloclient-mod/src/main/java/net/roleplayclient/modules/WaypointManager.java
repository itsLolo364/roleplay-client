package net.roleplayclient.modules;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Waypoint: posizioni salvate nel mondo. Per ogni waypoint nella
 * dimensione corrente viene calcolata distanza e direzione.
 */
public class WaypointManager {

    public record Dist(String name, double distance, String dir) {
    }

    private static final List<Dist> cache = new ArrayList<>();

    private WaypointManager() {
    }

    public static List<Dist> nearest(int n) {
        return cache.size() > n ? List.copyOf(cache.subList(0, n)) : List.copyOf(cache);
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (!RoleplayClientMod.config().isEnabled("waypoint")) return;

        String worldKey = client.world.getRegistryKey().getValue().toString();
        float yaw = client.player.getYaw();
        cache.clear();
        for (RoleplayConfig.Waypoint w : RoleplayClientMod.config().waypoints()) {
            if (!w.world().equalsIgnoreCase(worldKey)) continue;
            double dx = w.x() - client.player.getX();
            double dy = w.y() - client.player.getY();
            double dz = w.z() - client.player.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            cache.add(new Dist(w.name(), dist, cardinal(yaw, dx, dz)));
        }
        cache.sort(Comparator.comparingDouble(Dist::distance));
    }

    /**
     * Direzione relativa all'orientamento della testa (yaw): la freccia indica
     * dove si trova il waypoint rispetto a dove sta guardando il giocatore.
     * Le 4 frecce diagonali (\u2196\u2197\u2198\u2199) non sono nel font vanilla:
     * vengono aggiunte dal font personalizzato "roleplay-client:arrows".
     */
    private static String cardinal(float yaw, double dx, double dz) {
        if (dx == 0 && dz == 0) return "\u2191";
        double r = Math.toRadians(yaw);
        double forward = dz * Math.cos(r) - dx * Math.sin(r);
        double right = -dx * Math.cos(r) - dz * Math.sin(r);
        double angle = Math.toDegrees(Math.atan2(right, forward));
        int idx = ((int) Math.round(angle / 45.0)) & 7;
        String[] dirs = { "\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196" };
        return dirs[idx];
    }
}
