package net.roleplayclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.roleplayclient.Packages;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.gui.RcShell;
import net.roleplayclient.settings.Setting;
import net.roleplayclient.settings.SettingDefs;

import java.util.ArrayList;
import java.util.List;

import static net.roleplayclient.gui.GlassUi.*;

/**
 * Schermata generica delle impostazioni di un modulo: una riga per opzione.
 * Bool = toggle, numeriche = stepper +/- con campo editabile, stringhe =
 * campo di testo. I valori finiscono nella sezione "settings" della config.
 */
public class ModuleSettingsScreen extends Screen {
    private static final int ROW_H = 44;

    private final Screen parent;
    private final String moduleId;
    private final String title;
    private final List<Setting> settings;
    private final List<TextFieldWidget> fields = new ArrayList<>();
    private final List<Integer> fieldSettingIdx = new ArrayList<>();
    private int headerH = 56;
    private int bottomBarH = 52;

    public ModuleSettingsScreen(Screen parent, String moduleId) {
        super(Text.literal("Impostazioni"));
        this.parent = parent;
        this.moduleId = moduleId;
        var pkg = Packages.get(moduleId);
        this.title = (pkg != null ? pkg.name() : moduleId) + " — Impostazioni";
        this.settings = SettingDefs.forModule(moduleId);
    }

    @Override
    protected void init() {
        RcShell shell = RcShell.headerOnly(this.width, this.height);
        headerH = shell.headerH;
        bottomBarH = shell.compact ? 48 : 52;

        for (TextFieldWidget f : fields) this.remove(f);
        fields.clear();
        fieldSettingIdx.clear();

        for (int i = 0; i < settings.size(); i++) {
            Setting s = settings.get(i);
            if (!s.isNumeric() && s.type() != Setting.Type.STRING) continue;
            TextFieldWidget field = new TextFieldWidget(this.textRenderer, 0, 0, s.type() == Setting.Type.STRING ? Math.max(120, this.width - 90) : 110, 20, Text.literal(""));
            field.setMaxLength(s.type() == Setting.Type.STRING ? 300 : 12);
            field.setText(displayValue(s));
            final TextFieldWidget f = field;
            final int idx = i;
            field.setChangedListener(v -> commitField(idx, f));
            this.addDrawableChild(field);
            fields.add(field);
            fieldSettingIdx.add(i);
        }
    }

    private String displayValue(Setting s) {
        var cfg = RoleplayClientMod.config();
        return switch (s.type()) {
            case BOOL -> cfg.getBool(moduleId, s.key(), (Boolean) s.def()) ? "true" : "false";
            case INT -> String.valueOf(cfg.getInt(moduleId, s.key(), ((Number) s.def()).intValue()));
            case FLOAT -> fmt(cfg.getFloat(moduleId, s.key(), ((Number) s.def()).floatValue()));
            case STRING -> cfg.getString(moduleId, s.key(), (String) s.def());
        };
    }

    private static String fmt(float v) {
        if (v == Math.round(v)) return String.valueOf((int) v);
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private void commitField(int idx, TextFieldWidget field) {
        if (idx < 0 || idx >= settings.size()) return;
        Setting s = settings.get(idx);
        String raw = field.getText().trim();
        switch (s.type()) {
            case INT -> {
                try {
                    int v = clampI(Integer.parseInt(raw), (int) s.min(), (int) s.max());
                    RoleplayClientMod.config().set(moduleId, s.key(), v);
                } catch (NumberFormatException ignored) {
                }
            }
            case FLOAT -> {
                try {
                    float v = clampF(Float.parseFloat(raw.replace(',', '.')), s.min(), s.max());
                    v = roundToStep(v, s.step());
                    RoleplayClientMod.config().set(moduleId, s.key(), v);
                } catch (NumberFormatException ignored) {
                }
            }
            case STRING -> {
                if (!raw.isEmpty()) RoleplayClientMod.config().set(moduleId, s.key(), raw);
            }
            case BOOL -> {
            }
        }
    }

    private static int clampI(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clampF(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float roundToStep(float v, float step) {
        if (step <= 0) return v;
        return Math.round(v / step) * step;
    }

    private void nudge(int idx, boolean up) {
        if (idx < 0 || idx >= settings.size()) return;
        Setting s = settings.get(idx);
        if (!s.isNumeric()) return;
        var cfg = RoleplayClientMod.config();
        if (s.type() == Setting.Type.INT) {
            int v = cfg.getInt(moduleId, s.key(), ((Number) s.def()).intValue());
            v = clampI(v + (up ? (int) s.step() : -(int) s.step()), (int) s.min(), (int) s.max());
            cfg.set(moduleId, s.key(), v);
        } else {
            float v = cfg.getFloat(moduleId, s.key(), ((Number) s.def()).floatValue());
            v = clampF(v + (up ? s.step() : -s.step()), s.min(), s.max());
            cfg.set(moduleId, s.key(), roundToStep(v, s.step()));
        }
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
        return headerH + 12;
    }

    private int listBottom() {
        return this.height - bottomBarH;
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

        int top = listTop();
        int bottom = listBottom();
        int listH = Math.max(40, bottom - top);
        GlassUi.panel(ctx, 10, top - 6, this.width - 20, listH + 12);

        for (int i = 0; i < settings.size(); i++) {
            int y = top + i * ROW_H;
            if (y + ROW_H < top || y > bottom) continue;
            Setting s = settings.get(i);
            GlassUi.navRow(ctx, 16, y + 2, this.width - 32, ROW_H - 4, false, false);

            String label = s.label();
            int lw = this.textRenderer.getWidth(label);
            int maxLabelW = this.width - 260;
            if (lw > maxLabelW) label = this.textRenderer.trimToWidth(label, maxLabelW) + "...";
            ctx.drawText(this.textRenderer, label, 28, y + (ROW_H - 8) / 2, TEXT, false);

            switch (s.type()) {
                case BOOL -> {
                    int tx = this.width - 28 - TOGGLE_W;
                    int ty = y + (ROW_H - TOGGLE_H) / 2;
                    GlassUi.toggle(ctx, tx, ty, RoleplayClientMod.config().getBool(moduleId, s.key(), (Boolean) s.def()));
                }
                case INT, FLOAT -> {
                    TextFieldWidget field = fieldFor(i);
                    if (field != null) {
                        int fw = 110;
                        int fx = this.width - 28 - fw;
                        int fy = y + (ROW_H - 20) / 2;
                        field.setX(fx);
                        field.setY(fy);
                        field.setWidth(fw);
                        if (!field.isFocused() && !field.getText().equals(displayValue(s))) {
                            field.setText(displayValue(s));
                        }
                        field.setDrawsBackground(true);
                        boolean minusHover = mx > fx - 30 && mx < fx - 6 && my > y && my < y + ROW_H;
                        boolean plusHover = mx > fx + fw + 6 && mx < fx + fw + 30 && my > y && my < y + ROW_H;
                        GlassUi.chip(ctx, fx - 30, y + 6, 24, ROW_H - 12, minusHover ? CARD_HOVER : GLASS_0);
                        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("-"), fx - 18, y + 13, AMBER);
                        GlassUi.chip(ctx, fx + fw + 6, y + 6, 24, ROW_H - 12, plusHover ? CARD_HOVER : GLASS_0);
                        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("+"), fx + fw + 18, y + 13, AMBER);
                    }
                }
                case STRING -> {
                    TextFieldWidget field = fieldFor(i);
                    if (field != null) {
                        int fw = Math.min(280, this.width - 90);
                        int fx = this.width - 28 - fw;
                        int fy = y + (ROW_H - 20) / 2;
                        field.setX(fx);
                        field.setY(fy);
                        field.setWidth(fw);
                        if (!field.isFocused() && !field.getText().equals(displayValue(s))) {
                            field.setText(displayValue(s));
                        }
                    }
                }
            }
        }

        if (settings.isEmpty()) {
            ctx.drawText(this.textRenderer, "Nessuna impostazione disponibile", 28, top + 16, MUTED, false);
        }

        ctx.drawText(this.textRenderer, "I numeri si cambiano con +/- oppure digitando — i valori si salvano subito", 18, this.height - 24, MUTED, false);
        super.render(ctx, mx, my, delta);
    }

    private TextFieldWidget fieldFor(int settingIdx) {
        int pos = fieldSettingIdx.indexOf(settingIdx);
        return pos >= 0 && pos < fields.size() ? fields.get(pos) : null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        if (mx > this.width - 48 && my < headerH) {
            close();
            return true;
        }

        int top = listTop();
        for (int i = 0; i < settings.size(); i++) {
            Setting s = settings.get(i);
            int y = top + i * ROW_H;
            if (my < y || my >= y + ROW_H) continue;
            switch (s.type()) {
                case BOOL -> {
                    int tx = this.width - 28 - TOGGLE_W;
                    int ty = y + (ROW_H - TOGGLE_H) / 2;
                    if (GlassUi.hitToggle(mx, my, tx, ty)) {
                        RoleplayClientMod.config().set(moduleId, s.key(), !RoleplayClientMod.config().getBool(moduleId, s.key(), (Boolean) s.def()));
                        return true;
                    }
                }
                case INT, FLOAT -> {
                    TextFieldWidget field = fieldFor(i);
                    if (field != null) {
                        int fx = this.width - 28 - 110;
                        if (mx > fx - 30 && mx < fx - 6) {
                            nudge(i, false);
                            return true;
                        }
                        if (mx > fx + 116 && mx < fx + 140) {
                            nudge(i, true);
                            return true;
                        }
                    }
                }
                case STRING -> {
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
