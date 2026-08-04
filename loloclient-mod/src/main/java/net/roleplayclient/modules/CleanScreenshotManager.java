package net.roleplayclient.modules;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.roleplayclient.RoleplayClientMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clean Screenshot: sostituisce F2. Invece dello screenshot vanilla (con HUD,
 * chat, mirino) cattura solo il mondo renderizzato, senza elementi UI.
 * Il mixin su MinecraftClient.handleInputEvents consuma la pressione di F2;
 * la cattura avviene alla fine del render del mondo (prima dell'HUD).
 */
public final class CleanScreenshotManager {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final long PENDING_TTL_MS = 2000;
    private static boolean pending = false;
    private static long pendingAt = 0;

    private CleanScreenshotManager() {
    }

    /**
     * Chiamato dal mixin in handleInputEvents. Se il modulo è attivo e F2 è
     * stato premuto, consuma la pressione (wasPressed) così lo screenshot
     * vanilla non parte, e programma la cattura pulita al prossimo frame.
     */
    public static boolean tryConsumeF2() {
        if (!RoleplayClientMod.config().isEnabled("cleanscreenshot")) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null || !client.options.screenshotKey.wasPressed()) return false;
        pending = true;
        pendingAt = System.currentTimeMillis();
        return true;
    }

    /** Chiamato alla fine del render del mondo (WorldRenderEvents.LAST). */
    public static void onWorldRender() {
        if (!pending) return;
        if (System.currentTimeMillis() - pendingAt > PENDING_TTL_MS) {
            pending = false;
            return;
        }
        pending = false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), CleanScreenshotManager::handleImage);
    }

    private static void handleImage(NativeImage img) {
        try (img) {
            Path dir = FabricLoader.getInstance().getGameDir().resolve("screenshots");
            Files.createDirectories(dir);
            Path file = dir.resolve("Clean_" + LocalDateTime.now().format(TS) + ".png");
            img.writeTo(file);
            RoleplayClientMod.showToast("Clean screenshot: " + file.getFileName());
        } catch (Exception e) {
            RoleplayClientMod.showToast("Screenshot non salvato: " + e.getMessage());
        }
    }
}
