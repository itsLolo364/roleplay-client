package net.roleplayclient.screen;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.gui.GlassButton;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.gui.RcShell;
import net.roleplayclient.gui.SkinPreview;

import java.util.List;

import static net.roleplayclient.gui.GlassUi.*;

/**
 * Menu principale — Home launcher: sidebar secondaria, CTA centrali, skin a destra.
 * Niente Singleplayer / Realms.
 */
public class RcTitleScreen extends Screen {
    /** Solo voci secondarie (le azioni principali stanno al centro). */
    private static final List<RcShell.NavItem> NAV = List.of(
            new RcShell.NavItem("⌂  Home", "home"),
            new RcShell.NavItem("⚙  Opzioni", "options"),
            new RcShell.NavItem("✕  Esci", "quit")
    );

    private GlassButton btnPlay;
    private GlassButton btnMods;
    private GlassButton btnOptions;
    private GlassButton btnQuit;
    private RcShell shell;

    public RcTitleScreen() {
        super(Text.literal("Roleplay Client"));
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
        int statsH = shell.statGridHeight(shell.contentW, 4);
        int y = shell.contentY + (shell.compact ? 70 : 96) + statsH + 16;

        int maxY = shell.contentY + shell.contentH - bh * 2 - gap - 8;
        if (y > maxY) y = Math.max(shell.contentY + 8, maxY);

        // Centro: azioni principali
        btnPlay = new GlassButton(x, y, bw, bh + 2, "▶  Gioca", GlassButton.Style.PRIMARY);
        y += bh + 2 + gap;
        btnMods = new GlassButton(x, y, bw, bh, "◈  Moduli RC", GlassButton.Style.SECONDARY);

        // Fallback se sidebar nascosta
        btnOptions = null;
        btnQuit = null;
        if (!shell.showSidebar) {
            y += bh + gap;
            int half = (bw - 8) / 2;
            btnOptions = new GlassButton(x, y, half, bh, "⚙  Opzioni", GlassButton.Style.SECONDARY);
            btnQuit = new GlassButton(x + half + 8, y, half, bh, "✕  Esci", GlassButton.Style.GHOST);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (shell == null || shell.width != this.width || shell.height != this.height) {
            shell = RcShell.of(this.width, this.height);
            layoutButtons();
        }

        shell.drawChrome(ctx, this.textRenderer, mx, my, null, "Roleplay Client", false);
        shell.drawSidebarNav(ctx, this.textRenderer, mx, my, NAV, 0);

        if (shell.showSidebar) {
            String ver = "v" + modVersion();
            GlassUi.chip(ctx, shell.sideX + 12, shell.height - 40, shell.sideW - 24, 26, GLASS_1);
            ctx.drawText(this.textRenderer, ver + " · aggiornato", shell.sideX + 22, shell.height - 32, MUTED, false);
        }

        int cx = shell.contentX;
        int cy = shell.contentY;
        String name = RcShell.sessionName();
        GlassUi.eyebrow(ctx, this.textRenderer, "Roleplay Client", cx, cy);
        String welcome = name != null ? "Bentornato, " + name + "." : "Bentornato.";
        ctx.drawText(this.textRenderer, welcome, cx, cy + 16, TEXT, true);

        String desc = "Entra in multiplayer e gestisci i moduli RP. Stile allineato al launcher.";
        int afterDesc = GlassUi.drawWrapped(ctx, this.textRenderer, desc, cx, cy + 36, shell.contentW, MUTED);

        RcShell.Stat[] stats = {
                new RcShell.Stat("Minecraft", "1.21.8"),
                new RcShell.Stat("Fabric", fabricVersion()),
                new RcShell.Stat("Client", modVersion()),
                new RcShell.Stat("Account", name != null ? name : "—")
        };
        shell.drawStatGrid(ctx, this.textRenderer, cx, afterDesc + 12, shell.contentW, stats);

        btnPlay.render(ctx, this.textRenderer, mx, my);
        btnMods.render(ctx, this.textRenderer, mx, my);
        if (btnOptions != null) btnOptions.render(ctx, this.textRenderer, mx, my);
        if (btnQuit != null) btnQuit.render(ctx, this.textRenderer, mx, my);

        if (shell.showRight) {
            drawRightPanel(ctx, mx, my);
        }

        super.render(ctx, mx, my, delta);
    }

    private void drawRightPanel(DrawContext ctx, int mx, int my) {
        int px = shell.rightX + 20;
        int py = shell.rightY + 36;
        int pw = shell.rightW - 40;
        int ph = Math.min(300, shell.rightH - 72);

        GlassUi.blueGlow(ctx, px + 8, py + ph / 2 + 20, pw - 16, 48);
        GlassUi.accentGlow(ctx, px + 24, py + ph / 2 + 36, pw - 48, 20);

        boolean drawn = SkinPreview.draw(ctx, px, py, pw, ph, mx, my);
        if (!drawn) {
            GlassUi.chip(ctx, px, py + 20, pw, ph - 40, GLASS_0);
            int ls = 48;
            int lx = px + (pw - ls) / 2;
            int ly = py + (ph - ls) / 2 - 10;
            ctx.drawTexturedQuad(GlassUi.logo(), lx, ly, lx + ls, ly + ls, 0f, 0f, 1f, 1f);
        }

        String tip = drawn ? "La tua skin"
                : (SkinPreview.isLoading() ? "Carico skin…" : "Skin non disponibile");
        ctx.drawText(this.textRenderer, tip, px + (pw - this.textRenderer.getWidth(tip)) / 2,
                shell.rightY + shell.rightH - 36, DIM, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int nav = shell != null ? shell.hitSidebarNav(mx, my, NAV.size()) : -1;
        if (nav >= 0) return activateNav(nav);

        if (btnPlay.contains(mx, my)) {
            this.client.setScreen(new MultiplayerScreen(this));
            return true;
        }
        if (btnMods.contains(mx, my)) {
            this.client.setScreen(new ModMenuScreen(this));
            return true;
        }
        if (btnOptions != null && btnOptions.contains(mx, my)) {
            this.client.setScreen(new RcOptionsScreen(this, this.client.options));
            return true;
        }
        if (btnQuit != null && btnQuit.contains(mx, my)) {
            this.client.scheduleStop();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean activateNav(int idx) {
        return switch (NAV.get(idx).id()) {
            case "home" -> true;
            case "options" -> {
                this.client.setScreen(new RcOptionsScreen(this, this.client.options));
                yield true;
            }
            case "quit" -> {
                this.client.scheduleStop();
                yield true;
            }
            default -> false;
        };
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer(RoleplayClientMod.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("1.0.0");
    }

    private static String fabricVersion() {
        return FabricLoader.getInstance().getModContainer("fabricloader")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("—");
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
