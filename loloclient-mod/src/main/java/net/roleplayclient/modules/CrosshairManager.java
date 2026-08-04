package net.roleplayclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.roleplayclient.RoleplayClientMod;

/**
 * Crosshair RP: disegna un mirino personalizzato al centro dello schermo,
 * con colore e dimensione configurabili. Se "hide" è attivo non disegna nulla.
 * Viene invocato dal mixin su InGameHud.renderCrosshair (che viene cancellato).
 */
public final class CrosshairManager {

    private CrosshairManager() {
    }

    public static void render(DrawContext ctx, MinecraftClient client) {
        var cfg = RoleplayClientMod.config();
        if (cfg.getBool("crosshair", "hide", false)) return;

        int size = cfg.getInt("crosshair", "size", 6);
        int color = parseColor(cfg.getString("crosshair", "color", "#FFFFFF"), 0xFFFFFFFF);

        int cw = client.getWindow().getScaledWidth() / 2;
        int ch = client.getWindow().getScaledHeight() / 2;
        int gap = 2;

        ctx.fill(cw - 1, ch - gap - size, cw + 1, ch - gap, color);
        ctx.fill(cw - 1, ch + gap, cw + 1, ch + gap + size, color);
        ctx.fill(cw - gap - size, ch - 1, cw - gap, ch + 1, color);
        ctx.fill(cw + gap, ch - 1, cw + gap + size, ch + 1, color);
        ctx.fill(cw - 1, ch - 1, cw + 1, ch + 1, color);
    }

    /** Parsa "#RRGGBB" o "#AARRGGBB" (il default ARGB è a piena opacità). */
    public static int parseColor(String s, int def) {
        if (s == null) return def;
        try {
            String t = s.trim();
            if (t.startsWith("#")) t = t.substring(1);
            if (t.length() == 6) return 0xFF000000 | Integer.parseInt(t, 16);
            if (t.length() == 8) return Integer.parseUnsignedInt(t, 16);
            return Integer.parseUnsignedInt(t, 16);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
