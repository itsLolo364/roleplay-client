package net.roleplayclient.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.gui.RcShell;

import static net.roleplayclient.gui.GlassUi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tasti Roleplay Client — chrome Liquid Glass stile launcher.
 */
public class KeybindsScreen extends Screen {
    private static final int ROW_H = 48;

    private final Screen parent;
    private final List<KeyBinding> binds = new ArrayList<>();
    private int assigning = -1;
    private int scroll = 0;
    private int headerH = 56;
    private boolean needsSave = false;

    public KeybindsScreen(Screen parent) {
        super(Text.literal("Tasti di Roleplay Client"));
        this.parent = parent;
        binds.add(RoleplayClientMod.ZOOM_KEY);
        binds.add(RoleplayClientMod.CINEMA_KEY);
        binds.add(RoleplayClientMod.QUICK_KEY);
        binds.add(RoleplayClientMod.VOLTI_KEY);
        binds.add(RoleplayClientMod.STOPWATCH_KEY);
    }

    @Override
    protected void init() {
        headerH = RcShell.headerOnly(this.width, this.height).headerH;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public void close() {
        if (needsSave) {
            MinecraftClient.getInstance().options.write();
            needsSave = false;
        }
        if (parent != null && this.client != null) this.client.setScreen(parent);
        else super.close();
    }

    private int listTop() {
        return headerH + 12;
    }

    private int listBottom() {
        return this.height - 48;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        GlassUi.background(ctx);
        GlassUi.panel(ctx, 0, 0, this.width, headerH);
        GlassUi.eyebrow(ctx, this.textRenderer, "Roleplay Client", 18, 14);
        ctx.drawText(this.textRenderer, "Tasti", 18, 30, TEXT, true);

        boolean closeHover = mx > this.width - 48 && my < headerH;
        GlassUi.chip(ctx, this.width - 44, (headerH - 28) / 2, 28, 28, closeHover ? CARD_HOVER : GLASS_1);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("x"), this.width - 30, (headerH - 8) / 2, MUTED);

        int top = listTop();
        int bottom = listBottom();
        int listH = Math.max(40, bottom - top);
        GlassUi.panel(ctx, 10, top - 6, this.width - 20, listH + 12);

        int maxScroll = Math.max(0, binds.size() * ROW_H - listH);
        if (scroll > maxScroll) scroll = maxScroll;

        RcShell.beginScissor(ctx, 16, top, this.width - 32, listH);
        int y = top - scroll;
        for (int i = 0; i < binds.size(); i++) {
            if (y + ROW_H >= top && y <= bottom) {
                KeyBinding kb = binds.get(i);
                boolean hover = mx > 16 && mx < this.width - 16 && my > y && my < y + ROW_H
                        && my >= top && my < bottom;
                boolean active = assigning == i;
                GlassUi.navRow(ctx, 16, y + 4, this.width - 32, ROW_H - 8, active, hover);

                ctx.drawText(this.textRenderer, Text.translatable(kb.getTranslationKey()), 28, y + 18, active ? AMBER : TEXT, false);

                String kbText = active ? "Premi un tasto..." : kb.getBoundKeyLocalizedText().getString();
                int chipW = Math.min(140, Math.max(90, this.width / 5));
                GlassUi.chip(ctx, this.width - chipW - 30, y + 10, chipW, ROW_H - 20, active ? AMBER_SOFT : GLASS_0);
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(kbText),
                        this.width - chipW / 2 - 30, y + 18, active ? AMBER : MUTED);
            }
            y += ROW_H;
        }
        RcShell.endScissor(ctx);

        String hint = assigning >= 0
                ? "Premi il tasto da assegnare — ESC annulla"
                : "Clicca una riga per cambiare tasto — ESC chiude";
        ctx.drawText(this.textRenderer, hint, 18, this.height - 30, MUTED, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (assigning >= 0) {
            if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
                assigning = -1;
                return true;
            }
            InputUtil.Key key = InputUtil.fromKeyCode(keyCode, scanCode);
            if (key != InputUtil.UNKNOWN_KEY) {
                assign(key);
            }
            return true;
        }
        if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 && assigning < 0) return super.mouseClicked(mx, my, button);

        if (mx > this.width - 48 && my < headerH) {
            close();
            return true;
        }
        if (assigning >= 0) {
            assign(InputUtil.Type.MOUSE.createFromCode(button));
            return true;
        }
        int top = listTop();
        int bottom = listBottom();
        if (my >= top && my < bottom) {
            int idx = (int) ((my - top + scroll) / ROW_H);
            if (idx >= 0 && idx < binds.size()) {
                assigning = idx;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listH = listBottom() - listTop();
        int maxScroll = Math.max(0, binds.size() * ROW_H - listH);
        scroll = Math.max(0, Math.min(scroll + (int) (-verticalAmount * 14), maxScroll));
        return true;
    }

    private void assign(InputUtil.Key key) {
        KeyBinding original = binds.get(assigning);
        InputUtil.Key originalKey = InputUtil.fromTranslationKey(original.getBoundKeyTranslationKey());
        original.setBoundKey(key);
        KeyBinding.updateKeysByCode();

        String conflictLabel = vanillaConflictLabel(key, original);
        if (conflictLabel != null) {
            RoleplayClientMod.showToast("Conflitto: " + conflictLabel + " — tasto ripristinato");
            original.setBoundKey(originalKey);
            KeyBinding.updateKeysByCode();
            assigning = -1;
            return;
        }
        needsSave = true;
        assigning = -1;
    }

    private static String vanillaConflictLabel(InputUtil.Key key, KeyBinding exclude) {
        for (KeyBinding kb : MinecraftClient.getInstance().options.allKeys) {
            if (kb == exclude) continue;
            if (kb.getBoundKeyTranslationKey().equals(key.getTranslationKey())) {
                return kb.getTranslationKey();
            }
        }
        return null;
    }
}
