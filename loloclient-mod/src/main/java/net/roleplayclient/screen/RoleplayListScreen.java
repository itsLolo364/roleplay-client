package net.roleplayclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.gui.RcShell;

import static net.roleplayclient.gui.GlassUi.*;

/**
 * Base liste RP — chrome Liquid Glass + scissor + bottom bar adattiva.
 */
public abstract class RoleplayListScreen extends Screen {
    protected static final int ITEM_H = 36;

    protected final Screen parent;
    protected final String title;
    protected int scroll = 0;
    protected int selectedRowIdx = -1;
    protected int headerH = 56;
    protected int bottomBarH = 64;

    protected RoleplayListScreen(Screen parent, String title) {
        super(Text.literal(title));
        this.parent = parent;
        this.title = title;
    }

    protected abstract int rowCount();

    protected abstract String rowLabel(int i);

    protected abstract boolean rowDeletable(int i);

    protected abstract void onRowSelect(int i);

    protected abstract void onRowDelete(int i);

    protected String rowToggleLabel(int i) {
        return "";
    }

    protected void onRowToggle(int i) {
    }

    protected boolean rowToggleOn(int i) {
        return rowToggleLabel(i).startsWith("ON");
    }

    protected String rowSecondary(int i) {
        return "";
    }

    protected void onRowSecondary(int i) {
    }

    protected int listTop() {
        return headerH + 12;
    }

    protected int listBottom() {
        return this.height - bottomBarH;
    }

    @Override
    protected void init() {
        RcShell shell = RcShell.headerOnly(this.width, this.height);
        headerH = shell.headerH;
        bottomBarH = shell.compact ? 52 : 64;
        if (this.height < 360) bottomBarH = Math.min(bottomBarH, 48);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public void close() {
        if (parent != null && this.client != null) this.client.setScreen(parent);
        else super.close();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        GlassUi.background(ctx);
        GlassUi.panel(ctx, 0, 0, this.width, headerH);
        GlassUi.eyebrow(ctx, this.textRenderer, "Roleplay Client", 18, 14);
        ctx.drawText(this.textRenderer, this.title, 18, 30, TEXT, true);

        boolean closeHover = mx > this.width - 48 && my < headerH;
        GlassUi.chip(ctx, this.width - 44, (headerH - 28) / 2, 28, 28, closeHover ? CARD_HOVER : GLASS_1);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("x"), this.width - 30, (headerH - 8) / 2, MUTED);

        super.render(ctx, mx, my, delta);

        int top = listTop();
        int bottom = listBottom();
        int listH = Math.max(40, bottom - top);
        GlassUi.panel(ctx, 10, top - 6, this.width - 20, listH + 12);

        int maxScroll = Math.max(0, rowCount() * ITEM_H - listH);
        if (scroll > maxScroll) scroll = maxScroll;

        RcShell.beginScissor(ctx, 16, top, this.width - 32, listH);
        int y = top - scroll;
        for (int i = 0; i < rowCount(); i++) {
            if (y + ITEM_H >= top && y <= bottom) {
                boolean hover = mx > 16 && mx < this.width - 16 && my > y && my < y + ITEM_H
                        && my >= top && my < bottom;
                boolean sel = i == selectedRowIdx;
                GlassUi.navRow(ctx, 16, y + 2, this.width - 32, ITEM_H - 4, sel, hover);
                ctx.drawText(this.textRenderer, rowLabel(i), 28, y + 13, sel ? AMBER : TEXT, false);

                String sec = rowSecondary(i);
                int right = this.width - 28;
                if (rowDeletable(i)) {
                    boolean delHov = mx > right - 22 && mx < right && my > y && my < y + ITEM_H;
                    GlassUi.chip(ctx, right - 22, y + 6, 20, ITEM_H - 12, delHov ? CARD_HOVER : GLASS_0);
                    ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("x"), right - 12, y + 13, MUTED);
                    right -= 28;
                }
                if (!rowToggleLabel(i).isEmpty()) {
                    int tx = right - TOGGLE_W;
                    int ty = y + (ITEM_H - TOGGLE_H) / 2;
                    GlassUi.toggle(ctx, tx, ty, rowToggleOn(i));
                    right = tx - 8;
                }
                if (!sec.isEmpty()) {
                    boolean secHover = mx > right - 84 && mx < right && my > y && my < y + ITEM_H;
                    GlassUi.chip(ctx, right - 84, y + 6, 80, ITEM_H - 12, secHover ? CARD_HOVER : GLASS_0);
                    ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(sec), right - 44, y + 13, AMBER);
                }
            }
            y += ITEM_H;
        }
        RcShell.endScissor(ctx);

        if (rowCount() == 0) {
            ctx.drawText(this.textRenderer, "Lista vuota", 28, top + 16, MUTED, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        if (mx > this.width - 48 && my < headerH) {
            close();
            return true;
        }
        int top = listTop();
        int bottom = listBottom();
        if (my >= top && my < bottom) {
            int idx = (int) ((my - top + scroll) / ITEM_H);
            if (idx >= 0 && idx < rowCount()) {
                double ry = top + idx * ITEM_H - scroll;
                int right = this.width - 28;
                if (rowDeletable(idx) && mx > right - 22 && mx < right) {
                    onRowDelete(idx);
                    return true;
                }
                if (rowDeletable(idx)) right -= 28;
                if (!rowToggleLabel(idx).isEmpty()) {
                    int tx = right - TOGGLE_W;
                    int ty = (int) ry + (ITEM_H - TOGGLE_H) / 2;
                    if (GlassUi.hitToggle(mx, my, tx, ty)) {
                        onRowToggle(idx);
                        return true;
                    }
                    right = tx - 8;
                }
                if (!rowSecondary(idx).isEmpty() && mx > right - 84 && mx < right) {
                    onRowSecondary(idx);
                    return true;
                }
                onRowSelect(idx);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listH = listBottom() - listTop();
        int maxScroll = Math.max(0, rowCount() * ITEM_H - listH);
        scroll = Math.max(0, Math.min(scroll + (int) (-verticalAmount * 14), maxScroll));
        return true;
    }
}
