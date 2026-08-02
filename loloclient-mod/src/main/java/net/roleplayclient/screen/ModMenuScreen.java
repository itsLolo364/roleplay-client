package net.roleplayclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.roleplayclient.Packages;
import net.roleplayclient.Packages.Pkg;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;
import net.roleplayclient.gui.GlassButton;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.gui.RcShell;

import java.util.ArrayList;
import java.util.List;

import static net.roleplayclient.gui.GlassUi.*;

/**
 * Menu moduli — sidebar + dettaglio stile launcher, layout responsivo.
 */
public class ModMenuScreen extends Screen {
    private static final int SEARCH_H = 36;
    private static final int ITEM_H = 58;
    private static final int FOOTER_H = 40;
    private static final int PAD = 12;

    private final Screen parent;

    private TextFieldWidget search;
    private Pkg selectedModule;
    private int scroll = 0;
    private List<Pkg> cachedItems = List.of();
    private String lastQuery = "\0";

    private GlassButton btnPrimary;
    private GlassButton btnHud;
    private GlassButton btnConfig;
    private GlassButton btnKeys;

    private int sideW = 280;
    private int headerH = 64;

    public ModMenuScreen(Screen parent) {
        super(Text.literal("Roleplay Client - Moduli"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        RcShell shell = RcShell.content(this.width, this.height);
        headerH = shell.headerH;
        sideW = shell.showSidebar ? Math.min(320, Math.max(200, this.width / 3)) : Math.min(280, this.width);

        int searchY = headerH + 10;
        search = new TextFieldWidget(this.textRenderer, PAD + 10, searchY + 8, sideW - PAD * 2 - 20, 18, Text.literal(""));
        search.setMaxLength(40);
        search.setDrawsBackground(false);
        search.setEditableColor(TEXT);
        this.addDrawableChild(search);
        rebuildActionButtons();
    }

    private void rebuildActionButtons() {
        btnPrimary = btnHud = btnConfig = btnKeys = null;
        if (selectedModule == null) return;

        int x = sideW + 28;
        int detailW = this.width - sideW - 32;
        if (detailW < 160) {
            x = PAD;
            detailW = this.width - PAD * 2;
        }
        int by = this.height - FOOTER_H - 56;
        int bw = Math.min(200, detailW - 16);
        btnPrimary = new GlassButton(x, by, bw, 36,
                RoleplayClientMod.config().isEnabled(selectedModule.id()) ? "Disattiva" : "Attiva",
                GlassButton.Style.PRIMARY);

        int ey = by - 44;
        int ex = x;
        int small = Math.min(140, Math.max(100, (detailW - 24) / 3));
        if (selectedModule.hud()) {
            btnHud = new GlassButton(ex, ey, small, 32, "Editor HUD", GlassButton.Style.PRIMARY);
            ex += small + 8;
        }
        if (selectedModule.configurable()) {
            btnConfig = new GlassButton(ex, ey, small, 32, "Configura", GlassButton.Style.SECONDARY);
            ex += small + 8;
        }
        btnKeys = new GlassButton(ex, ey, Math.min(120, small), 32, "Tasti", GlassButton.Style.SECONDARY);
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

    private int listTop() {
        return headerH + 10 + SEARCH_H + 8;
    }

    private int listHeight() {
        return Math.max(40, this.height - listTop() - FOOTER_H - 8);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        GlassUi.background(ctx);
        drawHeader(ctx, mouseX, mouseY);
        drawSidebar(ctx, mouseX, mouseY);
        drawDetails(ctx, mouseX, mouseY);
        drawFooter(ctx);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawHeader(DrawContext ctx, int mx, int my) {
        GlassUi.panel(ctx, 0, 0, this.width, headerH);
        int ls = 36, ly = (headerH - ls) / 2;
        ctx.drawTexturedQuad(GlassUi.logo(), 18, ly, 18 + ls, ly + ls, 0f, 0f, 1f, 1f);
        ctx.drawText(this.textRenderer, "Roleplay ", 64, 18, TEXT, true);
        int tw = this.textRenderer.getWidth("Roleplay ");
        ctx.drawText(this.textRenderer, "Client", 64 + tw, 18, AMBER, true);
        GlassUi.eyebrow(ctx, this.textRenderer, "Moduli e pacchetti", 64, 36);

        boolean closeHover = mx > this.width - 48 && my < headerH;
        GlassUi.chip(ctx, this.width - 44, (headerH - 28) / 2, 28, 28, closeHover ? CARD_HOVER : GLASS_1);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("x"), this.width - 30, (headerH - 8) / 2, MUTED);
    }

    private void drawFooter(DrawContext ctx) {
        GlassUi.panel(ctx, 0, this.height - FOOTER_H, this.width, FOOTER_H);
        GlassUi.chip(ctx, 14, this.height - FOOTER_H + 8, Math.min(220, this.width - 28), 24, GLASS_1);
        ctx.drawText(this.textRenderer, "ESC  ·  Effetto immediato", 24, this.height - FOOTER_H + 15, MUTED, false);
    }

    private List<Pkg> filteredModules() {
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        if (q.equals(lastQuery)) return cachedItems;
        lastQuery = q;
        if (q.isEmpty()) {
            cachedItems = new ArrayList<>(Packages.all().values());
        } else {
            List<Pkg> out = new ArrayList<>();
            for (Pkg p : Packages.all().values()) {
                if (p.name().toLowerCase().contains(q) || p.desc().toLowerCase().contains(q)) out.add(p);
            }
            cachedItems = out;
        }
        return cachedItems;
    }

    private void drawSidebar(DrawContext ctx, int mx, int my) {
        int listTop = listTop();
        int listH = listHeight();
        GlassUi.panel(ctx, 0, headerH - 4, sideW, this.height - headerH - FOOTER_H + 8);

        int sy = headerH + 10;
        GlassUi.chip(ctx, PAD, sy, sideW - PAD * 2, SEARCH_H, GLASS_0);
        if (search != null) {
            search.setX(PAD + 10);
            search.setY(sy + 10);
            search.setWidth(sideW - PAD * 2 - 20);
            if (search.getText().isEmpty() && !search.isFocused()) {
                ctx.drawText(this.textRenderer, "Cerca un modulo...", PAD + 12, sy + 13, DIM, false);
            }
        }

        List<Pkg> items = filteredModules();
        int maxScroll = Math.max(0, items.size() * ITEM_H - listH);
        if (scroll > maxScroll) scroll = maxScroll;

        RoleplayConfig cfg = RoleplayClientMod.config();
        RcShell.beginScissor(ctx, PAD, listTop, sideW - PAD * 2, listH);
        int y = listTop - scroll;
        for (Pkg p : items) {
            if (y + ITEM_H >= listTop && y <= listTop + listH) {
                boolean on = cfg.isEnabled(p.id());
                boolean selected = p == selectedModule;
                boolean hover = mx > PAD && mx < sideW - PAD && my > y && my < y + ITEM_H && my >= listTop && my <= listTop + listH;

                int rowX = PAD;
                int rowW = sideW - PAD * 2;
                GlassUi.navRow(ctx, rowX, y + 2, rowW, ITEM_H - 4, selected, hover);

                int iy = y + (ITEM_H - 28) / 2;
                ctx.drawTexturedQuad(GlassUi.pkgIcon(p.id()), rowX + 10, iy, rowX + 38, iy + 28, 0f, 0f, 1f, 1f);

                int titleColor = selected ? AMBER : TEXT;
                ctx.drawText(this.textRenderer, p.name(), rowX + 46, y + 12, titleColor, false);
                String sub = p.hud() ? "PACCHETTO HUD" : "PACCHETTO RP";
                ctx.drawText(this.textRenderer, sub, rowX + 46, y + 26, MUTED, false);

                int tx = rowX + rowW - TOGGLE_W - 10;
                int ty = y + (ITEM_H - TOGGLE_H) / 2;
                GlassUi.toggle(ctx, tx, ty, on);
            }
            y += ITEM_H;
        }
        RcShell.endScissor(ctx);

        if (items.isEmpty()) {
            ctx.drawText(this.textRenderer, "Nessun pacchetto trovato", PAD + 8, listTop + 16, MUTED, false);
        }
    }

    private void drawDetails(DrawContext ctx, int mx, int my) {
        int x = sideW + 16;
        int w = this.width - x - 16;
        if (w < 120) return;
        GlassUi.panel(ctx, x, headerH - 4, w, this.height - headerH - FOOTER_H + 8);

        if (selectedModule == null) {
            GlassUi.eyebrow(ctx, this.textRenderer, "Roleplay Client", x + 24, headerH + 28);
            ctx.drawText(this.textRenderer, "Seleziona un pacchetto", x + 24, headerH + 48, TEXT, true);
            ctx.drawText(this.textRenderer, "dalla lista a sinistra.", x + 24, headerH + 64, MUTED, false);
            return;
        }

        Pkg p = selectedModule;
        RoleplayConfig cfg = RoleplayClientMod.config();
        boolean on = cfg.isEnabled(p.id());

        ctx.drawTexturedQuad(GlassUi.pkgIcon(p.id()), x + 24, headerH + 20, x + 24 + 48, headerH + 20 + 48, 0f, 0f, 1f, 1f);
        GlassUi.eyebrow(ctx, this.textRenderer, p.hud() ? "Pacchetto HUD" : "Pacchetto RP", x + 86, headerH + 24);
        ctx.drawText(this.textRenderer, p.name(), x + 86, headerH + 40, TEXT, true);
        ctx.drawText(this.textRenderer, on ? "Attivo — effetto immediato" : "Disattivato",
                x + 86, headerH + 56, on ? AMBER : MUTED, false);

        int ty = headerH + 88;
        ty = GlassUi.drawWrapped(ctx, this.textRenderer, p.desc(), x + 24, ty, w - 48, MUTED);
        if (p.keyBind()) {
            ctx.drawText(this.textRenderer, "Il tasto si cambia da \"Tasti\".", x + 24, ty + 8, DIM, false);
        }

        if (btnPrimary == null || !btnPrimary.label.equals(on ? "Disattiva" : "Attiva")) {
            rebuildActionButtons();
        }
        if (btnHud != null) btnHud.render(ctx, this.textRenderer, mx, my);
        if (btnConfig != null) btnConfig.render(ctx, this.textRenderer, mx, my);
        if (btnKeys != null) btnKeys.render(ctx, this.textRenderer, mx, my);
        if (btnPrimary != null) btnPrimary.render(ctx, this.textRenderer, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        if (search != null) search.mouseClicked(mx, my, button);

        if (mx > this.width - 48 && my < headerH) {
            close();
            return true;
        }

        if (handleListClick(mx, my)) return true;

        if (btnPrimary != null && btnPrimary.contains(mx, my) && selectedModule != null) {
            toggleModule(selectedModule);
            return true;
        }
        if (btnHud != null && btnHud.contains(mx, my)) {
            this.client.setScreen(new HudEditorScreen(this));
            return true;
        }
        if (btnConfig != null && btnConfig.contains(mx, my) && selectedModule != null) {
            this.client.setScreen(screenFor(selectedModule));
            return true;
        }
        if (btnKeys != null && btnKeys.contains(mx, my)) {
            this.client.setScreen(new KeybindsScreen(this));
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private boolean handleListClick(double mx, double my) {
        int listTop = listTop();
        int listH = listHeight();
        if (mx < PAD || mx > sideW - PAD || my < listTop || my > listTop + listH) return false;

        List<Pkg> items = filteredModules();
        int idx = (int) ((my - listTop + scroll) / ITEM_H);
        if (idx < 0 || idx >= items.size()) return true;

        Pkg p = items.get(idx);
        double rowY = listTop + idx * ITEM_H - scroll;
        int rowX = PAD;
        int rowW = sideW - PAD * 2;
        int tx = rowX + rowW - TOGGLE_W - 10;
        int ty = (int) rowY + (ITEM_H - TOGGLE_H) / 2;

        if (GlassUi.hitToggle(mx, my, tx, ty)) {
            toggleModule(p);
            selectedModule = p;
            return true;
        }
        selectedModule = p;
        rebuildActionButtons();
        return true;
    }

    private Screen screenFor(Pkg p) {
        return switch (p.id()) {
            case "volti" -> new FacesScreen(this);
            case "rpmessages" -> new QuickMessagesScreen(this);
            case "sveglie" -> new AlarmsScreen(this);
            case "waypoint" -> new WaypointsScreen(this);
            default -> this;
        };
    }

    private void toggleModule(Pkg p) {
        RoleplayConfig cfg = RoleplayClientMod.config();
        boolean on = !cfg.isEnabled(p.id());
        cfg.setEnabled(p.id(), on);
        RoleplayClientMod.showToast(p.name() + (on ? " attivato" : " disattivato"));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < sideW) {
            int items = filteredModules().size();
            int maxScroll = Math.max(0, items * ITEM_H - listHeight());
            scroll = Math.max(0, Math.min(scroll + (int) (-verticalAmount * 14), maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
