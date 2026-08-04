package net.roleplayclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.text.Text;
import net.roleplayclient.gui.GlassButton;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.gui.RcShell;
import net.roleplayclient.gui.SkinPreview;

import java.util.List;

import static net.roleplayclient.gui.GlassUi.*;

/**
 * Menu pausa — CTA centrali, resto in sidebar, skin a destra.
 */
public class RcPauseScreen extends Screen {
    private static final List<RcShell.NavItem> NAV = List.of(
            new RcShell.NavItem("★  Progressi", "adv"),
            new RcShell.NavItem("☰  Statistiche", "stats"),
            new RcShell.NavItem("⚙  Opzioni", "options"),
            new RcShell.NavItem("◈  Moduli RC", "mods")
    );

    private GlassButton btnBack;
    private GlassButton btnDisconnect;
    private GlassButton btnOptions;
    private GlassButton btnMods;
    private RcShell shell;
    private final boolean pauseOnly;

    public RcPauseScreen() {
        this(false);
    }

    public RcPauseScreen(boolean pauseOnly) {
        super(Text.literal("Menu di gioco"));
        this.pauseOnly = pauseOnly;
    }

    @Override
    protected void init() {
        shell = RcShell.of(this.width, this.height);
        layoutButtons();
    }

    private void layoutButtons() {
        int bw = shell.btnW(260);
        int bh = shell.btnH();
        int gap = shell.gap();
        int x = shell.contentX;
        int y = shell.contentY + (shell.compact ? 90 : 120);

        int need = bh * 2 + gap + (shell.showSidebar ? 0 : bh + gap);
        int maxY = shell.contentY + shell.contentH - need;
        if (y > maxY) y = Math.max(shell.contentY + 8, maxY);

        btnBack = new GlassButton(x, y, bw, bh + 2, "▶  Torna al gioco", GlassButton.Style.PRIMARY);
        y += bh + 2 + gap;
        btnDisconnect = new GlassButton(x, y, bw, bh, "⎋  Esci dal mondo", GlassButton.Style.GHOST);

        btnOptions = null;
        btnMods = null;
        if (!shell.showSidebar) {
            y += bh + gap;
            int half = (bw - 8) / 2;
            btnOptions = new GlassButton(x, y, half, bh, "⚙  Opzioni", GlassButton.Style.SECONDARY);
            btnMods = new GlassButton(x + half + 8, y, half, bh, "◈  Moduli", GlassButton.Style.SECONDARY);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (shell == null || shell.width != this.width || shell.height != this.height) {
            shell = RcShell.of(this.width, this.height);
            layoutButtons();
        }

        shell.drawChrome(ctx, this.textRenderer, mx, my, "Roleplay Client", "Menu di gioco", false);
        shell.drawSidebarNav(ctx, this.textRenderer, mx, my, NAV, -1);

        int cx = shell.contentX;
        int cy = shell.contentY;
        GlassUi.eyebrow(ctx, this.textRenderer, "In partita", cx, cy);
        ctx.drawText(this.textRenderer, "Partita in pausa", cx, cy + 16, TEXT, true);

        String world = worldLabel();
        String desc = "Riprendi quando sei pronto. Le altre azioni sono nella barra a sinistra.";
        int after = GlassUi.drawWrapped(ctx, this.textRenderer, desc, cx, cy + 36, shell.contentW, MUTED);

        RcShell.Stat[] stats = {
                new RcShell.Stat("Mondo", world),
                new RcShell.Stat("Modalità", this.client != null && this.client.isInSingleplayer() ? "Singleplayer" : "Multiplayer"),
                new RcShell.Stat("Giocatore", RcShell.sessionName() != null ? RcShell.sessionName() : "—")
        };
        shell.drawStatGrid(ctx, this.textRenderer, cx, after + 10, shell.contentW, stats);

        btnBack.render(ctx, this.textRenderer, mx, my);
        btnDisconnect.render(ctx, this.textRenderer, mx, my);
        if (btnOptions != null) btnOptions.render(ctx, this.textRenderer, mx, my);
        if (btnMods != null) btnMods.render(ctx, this.textRenderer, mx, my);

        if (shell.showRight) {
            int px = shell.rightX + 20;
            int py = shell.rightY + 40;
            int pw = shell.rightW - 40;
            int ph = Math.min(280, shell.rightH - 72);
            GlassUi.blueGlow(ctx, px + 10, py + ph / 2, pw - 20, 36);
            if (SkinPreview.draw(ctx, px, py, pw, ph, mx, my)) {
                String tip = "La tua skin";
                ctx.drawText(this.textRenderer, tip, px + (pw - this.textRenderer.getWidth(tip)) / 2,
                        shell.rightY + shell.rightH - 36, DIM, false);
            }
        }

        super.render(ctx, mx, my, delta);
    }

    private String worldLabel() {
        if (this.client == null || this.client.world == null) return "—";
        if (this.client.isInSingleplayer() && this.client.getServer() != null) {
            return truncate(this.client.getServer().getSaveProperties().getLevelName(), 18);
        }
        if (this.client.getCurrentServerEntry() != null) {
            return truncate(this.client.getCurrentServerEntry().name, 18);
        }
        return "Server";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int nav = shell != null ? shell.hitSidebarNav(mx, my, NAV.size()) : -1;
        if (nav >= 0) return activateNav(nav);

        if (btnBack.contains(mx, my)) return resume();
        if (btnDisconnect.contains(mx, my)) return disconnect();
        if (btnOptions != null && btnOptions.contains(mx, my)) {
            this.client.setScreen(new RcOptionsScreen(this, this.client.options));
            return true;
        }
        if (btnMods != null && btnMods.contains(mx, my)) {
            this.client.setScreen(new ModMenuScreen(this));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean activateNav(int idx) {
        return switch (NAV.get(idx).id()) {
            case "adv" -> {
                if (this.client.player != null) {
                    this.client.setScreen(new AdvancementsScreen(
                            this.client.player.networkHandler.getAdvancementHandler(), this));
                }
                yield true;
            }
            case "stats" -> {
                if (this.client.player != null) {
                    this.client.setScreen(new StatsScreen(this, this.client.player.getStatHandler()));
                }
                yield true;
            }
            case "options" -> {
                this.client.setScreen(new RcOptionsScreen(this, this.client.options));
                yield true;
            }
            case "mods" -> {
                this.client.setScreen(new ModMenuScreen(this));
                yield true;
            }
            default -> false;
        };
    }

    private boolean resume() {
        this.client.setScreen(null);
        this.client.mouse.lockCursor();
        return true;
    }

    private boolean disconnect() {
        if (this.client == null || this.client.world == null) return true;
        GameMenuScreen.disconnect(this.client, net.minecraft.client.world.ClientWorld.QUITTING_MULTIPLAYER_TEXT);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return pauseOnly;
    }
}
