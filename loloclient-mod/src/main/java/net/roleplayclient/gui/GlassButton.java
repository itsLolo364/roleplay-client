package net.roleplayclient.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Bottone pill stile launcher: primary (GIOCA), secondary, ghost.
 * Mutable bounds for responsive re-layout.
 */
public final class GlassButton {
    public enum Style { PRIMARY, SECONDARY, GHOST }

    public int x, y, w, h;
    public String label;
    public final Style style;
    public boolean enabled = true;

    public GlassButton(int x, int y, int w, int h, String label, Style style) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.label = label;
        this.style = style;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean contains(double mx, double my) {
        return enabled && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void render(DrawContext ctx, TextRenderer tr, int mx, int my) {
        boolean hover = contains(mx, my);
        int dy = hover && enabled ? -1 : 0;
        int rx = x;
        int ry = y + dy;
        switch (style) {
            case PRIMARY -> {
                if (enabled) GlassUi.accentGlow(ctx, rx, ry, w, h);
                GlassUi.chip(ctx, rx, ry, w, h, hover && enabled ? GlassUi.AMBER_2 : GlassUi.AMBER);
                // specular top
                if (enabled) ctx.fill(rx + 6, ry + 1, rx + w - 6, ry + 2, 0x55FFFFFF);
                drawLabel(ctx, tr, enabled ? GlassUi.ON_ACCENT : GlassUi.DIM, ry);
            }
            case SECONDARY -> {
                GlassUi.chip(ctx, rx, ry, w, h, hover && enabled ? GlassUi.CARD_HOVER : GlassUi.GLASS_1);
                if (hover && enabled) {
                    ctx.fill(rx, ry, rx + w, ry + 1, GlassUi.AMBER_LINE);
                    ctx.fill(rx, ry + h - 1, rx + w, ry + h, GlassUi.AMBER_LINE);
                    ctx.fill(rx, ry, rx + 1, ry + h, GlassUi.AMBER_LINE);
                    ctx.fill(rx + w - 1, ry, rx + w, ry + h, GlassUi.AMBER_LINE);
                } else {
                    GlassUi.border(ctx, rx, ry, w, h);
                }
                drawLabel(ctx, tr, enabled ? GlassUi.TEXT : GlassUi.DIM, ry);
            }
            case GHOST -> {
                if (hover && enabled) GlassUi.chip(ctx, rx, ry, w, h, GlassUi.GLASS_0);
                drawLabel(ctx, tr, enabled ? GlassUi.MUTED : GlassUi.DIM, ry);
            }
        }
    }

    private void drawLabel(DrawContext ctx, TextRenderer tr, int color, int ry) {
        int tw = tr.getWidth(label);
        ctx.drawText(tr, Text.literal(label), x + (w - tw) / 2, ry + (h - 8) / 2, color, false);
    }
}
