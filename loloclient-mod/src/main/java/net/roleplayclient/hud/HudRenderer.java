package net.roleplayclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.roleplayclient.ClickCounter;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;
import net.roleplayclient.WaypointManager;
import net.roleplayclient.gui.GlassUi;

import static net.roleplayclient.gui.GlassUi.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Rendering dei moduli HUD (effetto immediato: ogni frame controlla la config).
 * Ogni modulo ha una posizione libera, trascinabile dall'Editor HUD.
 */
public class HudRenderer {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static long lastFpsTime = System.currentTimeMillis();
    private static int frames = 0;
    private static int fps = 0;
    private static long lastHudCacheMs = 0;
    private static final Map<String, String> hudCache = new HashMap<>();

    private record Toast(String text, long expiresAt) {
    }

    private static final Deque<Toast> toasts = new ArrayDeque<>();

    public static void showToast(String text) {
        toasts.addLast(new Toast(text, System.currentTimeMillis() + 3500));
        while (toasts.size() > 5) toasts.pollFirst();
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        frames++;
        long now = System.currentTimeMillis();
        if (now - lastFpsTime >= 500) {
            fps = (int) Math.round(frames * 1000.0 / (now - lastFpsTime));
            frames = 0;
            lastFpsTime = now;
        }

        var cfg = RoleplayClientMod.config();
        var player = client.player;
        String dim = client.world.getRegistryKey().getValue().getPath();

        text(context, cfg, "fps", () -> "FPS: " + fps);
        text(context, cfg, "cps", () -> "CPS: " + ClickCounter.getLeft() + " | " + ClickCounter.getRight());
        text(context, cfg, "coords", () -> "XYZ: " + player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ() + " (" + dim + ")");
        text(context, cfg, "clock", () -> LocalTime.now().format(TIME));
        text(context, cfg, "ping", () -> pingText(client));
        text(context, cfg, "oramondo", () -> "Ora: " + worldTime(client));

        if (cfg.isEnabled("armor")) drawArmor(context, client, cfg);
        if (cfg.isEnabled("waypoint")) drawWaypoints(context, cfg);

        renderToasts(context, client);
    }

    private static void text(DrawContext ctx, RoleplayConfig cfg, String id, Supplier<String> value) {
        if (!cfg.isEnabled(id)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        if (now - lastHudCacheMs >= 100) {
            lastHudCacheMs = now;
            hudCache.clear();
        }
        String cached = hudCache.get(id);
        if (cached == null) {
            cached = value.get();
            hudCache.put(id, cached);
        }
        var pos = cfg.getPosition(id);
        int x = Math.round(pos.x() * client.getWindow().getScaledWidth());
        int y = Math.round(pos.y() * client.getWindow().getScaledHeight());
        ctx.drawTextWithShadow(client.textRenderer, cached, x, y, GlassUi.TEXT);
    }

    private static String pingText(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return "Ping: ?";
        PlayerListEntry e = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        if (e == null) return "Ping: ?";
        return "Ping: " + e.getLatency() + " ms";
    }

    private static String worldTime(MinecraftClient client) {
        long t = client.world.getTimeOfDay() % 24000L;
        int hour = (int) ((t / 1000L + 6L) % 24L);
        int minute = (int) ((t % 1000L) * 60L / 1000L);
        return String.format("%02d:%02d", hour, minute);
    }

    private static void drawArmor(DrawContext ctx, MinecraftClient client, RoleplayConfig cfg) {
        var pos = cfg.getPosition("armor");
        int x = Math.round(pos.x() * client.getWindow().getScaledWidth());
        int y = Math.round(pos.y() * client.getWindow().getScaledHeight());

        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        int i = 0;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;
            int sy = y + i * 18;
            ctx.drawItem(stack, x, sy);
            ctx.drawStackOverlay(client.textRenderer, stack, x, sy);
            i++;
        }
    }

    private static void drawWaypoints(DrawContext ctx, RoleplayConfig cfg) {
        MinecraftClient client = MinecraftClient.getInstance();
        var pos = cfg.getPosition("waypoint");
        int x = Math.round(pos.x() * client.getWindow().getScaledWidth());
        int y = Math.round(pos.y() * client.getWindow().getScaledHeight());

        int i = 0;
        Identifier arrowsFont = Identifier.of("roleplay-client", "arrows");
        for (WaypointManager.Dist d : WaypointManager.nearest(3)) {
            String line = d.dir() + " " + d.name() + " - " + String.format("%.0fm", d.distance());
            ctx.drawTextWithShadow(client.textRenderer,
                    Text.literal(line).setStyle(Style.EMPTY.withFont(arrowsFont)),
                    x, y + i * 10, BLUE);
            i++;
        }
        if (i == 0) {
            ctx.drawTextWithShadow(client.textRenderer, "Nessun waypoint nella dimensione", x, y, 0x66FFFFFF);
        }
    }

    private static void renderToasts(DrawContext ctx, MinecraftClient client) {
        long now = System.currentTimeMillis();
        while (!toasts.isEmpty() && toasts.peekFirst().expiresAt() <= now) toasts.pollFirst();
        if (toasts.isEmpty()) return;

        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        int y = h - 24;
        for (Toast t : toasts) {
            int tw = client.textRenderer.getWidth(t.text());
            int th = 16;
            int tx = w - tw - 20;
            int ty = y - 5;
            GlassUi.chip(ctx, tx, ty, tw + 24, th, GlassUi.GLASS_1);
            GlassUi.disc(ctx, tx + 3, ty + 3, th - 6, AMBER);
            ctx.drawText(client.textRenderer, t.text(), tx + 18, ty + 4, GlassUi.TEXT, false);
            y -= 20;
        }
    }
}
