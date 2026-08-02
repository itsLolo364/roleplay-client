package net.roleplayclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

import static net.roleplayclient.gui.GlassUi.*;

/**
 * Layout shell stile launcher: header + sidebar + content + pannello destro opzionale.
 * Breakpoint responsivi per windowed / GUI scale alto.
 */
public final class RcShell {
    public static final int PAD = 16;
    public static final int NAV_ITEM_H = 40;
    public static final int STAT_H = 56;

    public final int width;
    public final int height;
    public final int headerH;
    public final int sideW;
    public final int rightW;
    public final int footerH;
    public final boolean compact;
    public final boolean showRight;
    public final boolean showSidebar;

    public final int contentX;
    public final int contentY;
    public final int contentW;
    public final int contentH;

    public final int sideX;
    public final int sideY;
    public final int sideH;

    public final int rightX;
    public final int rightY;
    public final int rightH;

    private RcShell(int width, int height, boolean wantRight, boolean wantSidebar) {
        this.width = width;
        this.height = height;
        this.compact = width < 720 || height < 420;
        this.showSidebar = wantSidebar && width >= 420;
        this.showRight = wantRight && width >= 780 && height >= 420;

        this.headerH = compact ? 52 : 64;
        this.footerH = 0;
        this.sideW = showSidebar ? (compact ? 140 : Math.min(220, Math.max(150, width / 5))) : 0;
        this.rightW = showRight ? Math.min(260, Math.max(180, width / 4)) : 0;

        this.sideX = 0;
        this.sideY = headerH;
        this.sideH = height - headerH;

        this.rightX = width - rightW;
        this.rightY = headerH;
        this.rightH = height - headerH;

        int gap = compact ? 10 : 16;
        this.contentX = sideW + gap;
        this.contentY = headerH + gap;
        this.contentW = Math.max(120, width - sideW - rightW - gap * (showRight ? 3 : 2));
        this.contentH = Math.max(80, height - headerH - gap * 2);
    }

    public static RcShell of(int width, int height) {
        return new RcShell(width, height, true, true);
    }

    public static RcShell of(int width, int height, boolean wantRight, boolean wantSidebar) {
        return new RcShell(width, height, wantRight, wantSidebar);
    }

    /** Shell senza pannello destro (Options, liste, ecc.). */
    public static RcShell content(int width, int height) {
        return new RcShell(width, height, false, true);
    }

    /** Solo header + content full-bleed (liste strette). */
    public static RcShell headerOnly(int width, int height) {
        return new RcShell(width, height, false, false);
    }

    public int btnW(int preferred) {
        return Math.min(preferred, Math.max(120, contentW - 8));
    }

    public int btnH() {
        return compact ? 30 : 36;
    }

    public int gap() {
        return compact ? 6 : 10;
    }

    public void drawChrome(DrawContext ctx, TextRenderer tr, int mx, int my,
                           String eyebrow, String title, boolean closeButton) {
        GlassUi.background(ctx);
        drawHeader(ctx, tr, mx, my, eyebrow, title, closeButton);
        if (showSidebar) {
            GlassUi.panel(ctx, sideX, sideY, sideW, sideH);
        }
        if (showRight) {
            GlassUi.panel(ctx, rightX + 8, rightY + 12, rightW - 16, rightH - 24);
        }
    }

    public void drawHeader(DrawContext ctx, TextRenderer tr, int mx, int my,
                           String eyebrow, String title, boolean closeButton) {
        GlassUi.panel(ctx, 0, 0, width, headerH);
        int ls = compact ? 28 : 36;
        int ly = (headerH - ls) / 2;
        ctx.drawTexturedQuad(GlassUi.logo(), 14, ly, 14 + ls, ly + ls, 0f, 0f, 1f, 1f);

        int tx = 14 + ls + 10;
        if (eyebrow != null && !eyebrow.isEmpty()) {
            GlassUi.eyebrow(ctx, tr, eyebrow, tx, compact ? 10 : 14);
            ctx.drawText(tr, title, tx, compact ? 24 : 30, TEXT, true);
        } else {
            ctx.drawText(tr, "Roleplay ", tx, headerH / 2 - 4, TEXT, true);
            int tw = tr.getWidth("Roleplay ");
            ctx.drawText(tr, "Client", tx + tw, headerH / 2 - 4, AMBER, true);
        }

        String user = sessionName();
        if (user != null && width > 480) {
            int chipW = Math.min(160, tr.getWidth(user) + 28);
            int cx = closeButton ? width - 52 - chipW - 8 : width - chipW - 16;
            int cy = (headerH - 28) / 2;
            GlassUi.chip(ctx, cx, cy, chipW, 28, GLASS_1);
            ctx.drawText(tr, user, cx + 14, cy + 10, MUTED, false);
        }

        if (closeButton) {
            boolean closeHover = mx > width - 48 && my < headerH;
            GlassUi.chip(ctx, width - 44, (headerH - 28) / 2, 28, 28, closeHover ? CARD_HOVER : GLASS_1);
            ctx.drawCenteredTextWithShadow(tr, Text.literal("x"), width - 30, (headerH - 8) / 2, MUTED);
        }
    }

    public boolean hitClose(double mx, double my) {
        return mx > width - 48 && my < headerH;
    }

    public void drawSidebarNav(DrawContext ctx, TextRenderer tr, int mx, int my,
                               List<NavItem> items, int selected) {
        if (!showSidebar) return;
        int y = sideY + 16;
        int x = sideX + 10;
        int w = sideW - 20;
        for (int i = 0; i < items.size(); i++) {
            NavItem item = items.get(i);
            boolean sel = i == selected;
            boolean hover = mx >= x && mx < x + w && my >= y && my < y + NAV_ITEM_H;
            int drawY = hover && !sel ? y - 1 : y;
            GlassUi.navRow(ctx, x, drawY, w, NAV_ITEM_H - 4, sel, hover);
            int color = sel ? AMBER : (hover ? TEXT : MUTED);
            ctx.drawText(tr, item.label(), x + 14, drawY + 12, color, false);
            y += NAV_ITEM_H + 4;
        }
    }

    public int hitSidebarNav(double mx, double my, int count) {
        if (!showSidebar) return -1;
        int y = sideY + 16;
        int x = sideX + 10;
        int w = sideW - 20;
        for (int i = 0; i < count; i++) {
            if (mx >= x && mx < x + w && my >= y && my < y + NAV_ITEM_H) return i;
            y += NAV_ITEM_H + 4;
        }
        return -1;
    }

    public void drawStatGrid(DrawContext ctx, TextRenderer tr, int x, int y, int maxW, Stat[] stats) {
        if (stats == null || stats.length == 0) return;
        int cols = maxW >= 420 ? Math.min(4, stats.length) : (maxW >= 260 ? 2 : 1);
        int gap = 8;
        int cardW = (maxW - gap * (cols - 1)) / cols;
        for (int i = 0; i < stats.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cardW + gap);
            int cy = y + row * (STAT_H + gap);
            GlassUi.statCard(ctx, tr, cx, cy, cardW, STAT_H, stats[i].label(), stats[i].value());
        }
    }

    public int statGridHeight(int maxW, int count) {
        if (count <= 0) return 0;
        int cols = maxW >= 420 ? Math.min(4, count) : (maxW >= 260 ? 2 : 1);
        int rows = (count + cols - 1) / cols;
        return rows * STAT_H + (rows - 1) * 8;
    }

    public static void beginScissor(DrawContext ctx, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        ctx.enableScissor(x, y, x + w, y + h);
    }

    public static void endScissor(DrawContext ctx) {
        ctx.disableScissor();
    }

    public static String sessionName() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c == null || c.getSession() == null) return null;
        String name = c.getSession().getUsername();
        return name == null || name.isBlank() ? null : name;
    }

    public record NavItem(String label, String id) {
    }

    public record Stat(String label, String value) {
    }
}
