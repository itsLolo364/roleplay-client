package net.roleplayclient.modules;

import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.roleplayclient.RoleplayClientMod;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Clip Ready: indica se Medals (clipping software) è raggiungibile e pronto a
 * registrare. Il poll di health avviene in background ogni N secondi.
 * Flash breve "CLIP!" quando un volto conosciuto parla in chat o quando arriva
 * un suono (momento da registrare). Beep quando Medals torna online.
 */
public final class ClipReadyManager {
    private static final ExecutorService POLLER = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "roleplay-client-clip-poll");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean healthy = false;
    private static volatile boolean checked = false;
    private static long lastPollMs = 0;
    private static long clipFlashUntil = 0;
    private static boolean lastHealthy = false;

    private ClipReadyManager() {
    }

    public static void tick() {
        var cfg = RoleplayClientMod.config();
        if (cfg == null || !cfg.isEnabled("clipready")) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        int pollSeconds = cfg.getInt("clipready", "pollSeconds", 5);
        long now = System.currentTimeMillis();
        if (lastPollMs == 0 || now - lastPollMs >= pollSeconds * 1000L) {
            lastPollMs = now;
            String url = cfg.getString("clipready", "healthUrl", "http://localhost:12665/api/v1/health");
            int timeout = Math.min(3000, 1000 + pollSeconds * 200);
            POLLER.execute(() -> poll(url, timeout));
        }
    }

    private static void poll(String url, int timeout) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            try (InputStream is = conn.getInputStream()) {
                is.readAllBytes();
            }
            healthy = code >= 200 && code < 300;
        } catch (Exception e) {
            healthy = false;
        }
        checked = true;
        MinecraftClient client = MinecraftClient.getInstance();
        if (healthy && !lastHealthy && client.player != null) {
            client.execute(() -> {
                RoleplayClientMod.showToast("Medals: clip pronta");
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f));
            });
        }
        lastHealthy = healthy;
    }

    /** Chiamato dall'evento chat: un volto conosciuto ha parlato -> flash clip. */
    public static void onChat(GameProfile sender) {
        if (sender == null) return;
        var cfg = RoleplayClientMod.config();
        if (cfg == null || !cfg.isEnabled("clipready")) return;
        if (FacesManager.findFace(sender.getName()) != null) {
            clipFlashUntil = System.currentTimeMillis() + 3000;
        }
    }

    /** Chiamato dal mixin su onPlaySound: un suono di gioco -> flash clip breve. */
    public static void onPlaySound() {
        var cfg = RoleplayClientMod.config();
        if (cfg == null || !cfg.isEnabled("clipready")) return;
        clipFlashUntil = System.currentTimeMillis() + 1500;
    }

    public static boolean isHealthy() {
        return checked && healthy;
    }

    public static boolean isUnknown() {
        return !checked;
    }

    public static boolean isClipFlashing() {
        return System.currentTimeMillis() < clipFlashUntil;
    }
}
