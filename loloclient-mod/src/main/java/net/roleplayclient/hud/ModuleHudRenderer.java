package net.roleplayclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.modules.ClipReadyManager;
import net.roleplayclient.modules.DesyncAlertManager;
import net.roleplayclient.modules.SessionTimeManager;
import net.roleplayclient.modules.StopwatchManager;
import net.roleplayclient.modules.TimersManager;

import java.util.List;

/**
 * Rendering degli HUD dei moduli RP nuovi: timer, cronometro, tempo di
 * sessione, allerta desync, indicatore clip e watermark. Tutte le posizioni
 * sono trascinabili dall'editor HUD (sezioni in RoleplayConfig).
 */
public final class ModuleHudRenderer {

    private ModuleHudRenderer() {
    }

    public static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        RoleplayConfig cfg = RoleplayClientMod.config();

        renderTimers(ctx, cfg);
        renderStopwatch(ctx, cfg);
        renderReaction(ctx, cfg);
        renderDesync(ctx, cfg);
        renderSession(ctx, cfg);
        renderClipReady(ctx, cfg);
        renderWatermark(ctx, cfg);
    }

    private static void text(DrawContext ctx, RoleplayConfig cfg, String id, String value, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        var pos = cfg.getPosition(id);
        int x = Math.round(pos.x() * client.getWindow().getScaledWidth());
        int y = Math.round(pos.y() * client.getWindow().getScaledHeight());
        ctx.drawTextWithShadow(client.textRenderer, value, x, y, color);
    }

    private static void centered(DrawContext ctx, RoleplayConfig cfg, String id, String value, int color, boolean shadow) {
        MinecraftClient client = MinecraftClient.getInstance();
        var pos = cfg.getPosition(id);
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int w = client.textRenderer.getWidth(value);
        int x = Math.round(pos.x() * sw) - w / 2;
        int y = Math.round(pos.y() * sh);
        if (shadow) ctx.drawTextWithShadow(client.textRenderer, value, x, y, color);
        else ctx.drawText(client.textRenderer, value, x, y, color, false);
    }

    private static void renderTimers(DrawContext ctx, RoleplayConfig cfg) {
        if (!cfg.isEnabled("rptimers")) return;
        List<TimersManager.Timer> active = TimersManager.active();
        if (active.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        var pos = cfg.getPosition("rptimers");
        int x = Math.round(pos.x() * client.getWindow().getScaledWidth());
        int y = Math.round(pos.y() * client.getWindow().getScaledHeight());
        int i = 0;
        for (TimersManager.Timer t : active) {
            int color = t.reaction() ? GlassUi.AMBER : GlassUi.BLUE;
            String line = t.reaction()
                    ? "Reazione: " + TimersManager.formatReaction(t.expiresAt() - System.currentTimeMillis())
                    : t.label() + ": " + TimersManager.formatRemaining(t.expiresAt() - System.currentTimeMillis());
            ctx.drawTextWithShadow(client.textRenderer, line, x, y + i * 10, color);
            i++;
        }
    }

    private static void renderStopwatch(DrawContext ctx, RoleplayConfig cfg) {
        if (!cfg.isEnabled("rpstopwatch")) return;
        String value = "Crono: " + StopwatchManager.format(StopwatchManager.elapsedMs())
                + (StopwatchManager.running() ? " [IN CORSO]" : "");
        text(ctx, cfg, "rpstopwatch", value, GlassUi.TEAL);
    }

    private static void renderReaction(DrawContext ctx, RoleplayConfig cfg) {
        if (!cfg.isEnabled("rptimers") || !TimersManager.reactionActive()) return;
        String value = "Tempo di reazione: " + TimersManager.formatReaction(TimersManager.reactionRemainingMs());
        centered(ctx, cfg, "reaction", value, GlassUi.AMBER, true);
    }

    private static void renderDesync(DrawContext ctx, RoleplayConfig cfg) {
        if (!cfg.isEnabled("desyncalert") || !DesyncAlertManager.isAlerting()) return;
        long since = DesyncAlertManager.sinceKeepAliveMs();
        String value = "DESYNC! Nessuna risposta da " + (since > 0 ? since / 1000 : "?") + "s";
        text(ctx, cfg, "desyncalert", value, 0xFFF06060);
    }

    private static void renderSession(DrawContext ctx, RoleplayConfig cfg) {
        if (!cfg.isEnabled("sessiontime")) return;
        text(ctx, cfg, "sessiontime", "Sessione: " + SessionTimeManager.format(SessionTimeManager.sessionMs()), GlassUi.BLUE);
    }

    private static void renderClipReady(DrawContext ctx, RoleplayConfig cfg) {
        if (!cfg.isEnabled("clipready")) return;
        String value;
        int color;
        if (ClipReadyManager.isUnknown()) {
            value = "CLIP: ...";
            color = GlassUi.MUTED;
        } else if (ClipReadyManager.isHealthy()) {
            value = "CLIP READY";
            color = GlassUi.AMBER;
        } else {
            value = "CLIP OFFLINE";
            color = GlassUi.MUTED;
        }
        if (ClipReadyManager.isClipFlashing()) {
            value = value + "  *CLIP*";
            color = GlassUi.AMBER;
        }
        text(ctx, cfg, "clipready", value, color);
    }

    private static void renderWatermark(DrawContext ctx, RoleplayConfig cfg) {
        if (!cfg.isEnabled("watermark")) return;
        centered(ctx, cfg, "watermark", "Roleplay Client", GlassUi.MUTED, true);
    }
}
