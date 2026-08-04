package net.roleplayclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.modules.QuickMessagesManager;

import java.util.List;

/**
 * Messaggi rapidi: la lista si apre con il tasto "+"; cliccare su una voce
 * la invia in chat. Ogni messaggio puo' avere un tasto assegnato: premendolo
 * si invia subito quel messaggio (es. K -> /sit).
 */
public class QuickMessagesScreen extends RoleplayListScreen {
    private final List<String> messages;
    private final List<String> keys;
    private final List<Boolean> threats;
    private TextFieldWidget field;
    private int capturingIdx = -1;

    public QuickMessagesScreen(Screen parent) {
        super(parent, "Messaggi rapidi");
        this.messages = RoleplayClientMod.config().quickMessages();
        this.keys = RoleplayClientMod.config().quickMessageKeys();
        this.threats = RoleplayClientMod.config().quickThreats();
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height - 44;
        field = new TextFieldWidget(this.textRenderer, 12, y, Math.max(60, this.width - 240), 20, Text.literal(""));
        field.setMaxLength(200);
        field.setPlaceholder(Text.literal("Testo o comando (es. /me ...)"));
        this.addDrawableChild(field);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Aggiungi"), b -> addMessage())
                .dimensions(Math.max(12, this.width - 220), y, 100, 20)
                .build());
    }

    private void addMessage() {
        String v = field.getText().trim();
        if (v.isEmpty()) return;
        messages.add(v);
        keys.add("");
        threats.add(false);
        field.setText("");
        RoleplayClientMod.config().save();
    }

    @Override
    protected int rowCount() {
        return messages.size();
    }

    @Override
    protected String rowLabel(int i) {
        String m = messages.get(i);
        return m.length() > 40 ? m.substring(0, 40) + "..." : m;
    }

    @Override
    protected String rowSecondary(int i) {
        if (capturingIdx == i) return "Premi tasto...";
        String k = keys.get(i);
        if (k == null || k.isEmpty()) return "Tasto: -";
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(k);
            return "Tasto: " + key.getLocalizedText().getString();
        } catch (Exception e) {
            return "Tasto: ?";
        }
    }

    @Override
    protected void onRowSecondary(int i) {
        capturingIdx = i;
    }

    @Override
    protected boolean rowDeletable(int i) {
        return true;
    }

    @Override
    protected String rowToggleLabel(int i) {
        return threats.get(i) ? "ON" : "OFF";
    }

    @Override
    protected boolean rowToggleOn(int i) {
        return threats.get(i);
    }

    @Override
    protected void onRowToggle(int i) {
        threats.set(i, !threats.get(i));
        RoleplayClientMod.config().save();
    }

    @Override
    protected void onRowSelect(int i) {
        if (QuickMessagesManager.send(messages.get(i), threats.get(i))) {
            RoleplayClientMod.showToast("Inviato: " + messages.get(i));
        }
    }

    @Override
    protected void onRowDelete(int i) {
        messages.remove(i);
        keys.remove(i);
        threats.remove(i);
        if (capturingIdx == i) capturingIdx = -1;
        RoleplayClientMod.config().save();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        String hint = capturingIdx >= 0
                ? "Premi il tasto da assegnare - ESC annulla - CANC/Backspace per togliere"
                : "Pulsante \"Tasto\" per assegnare un tasto - toggle ON = il messaggio è una minaccia (tempo di reazione)";
        ctx.drawText(this.textRenderer, hint, 12, this.height - 52, GlassUi.MUTED, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingIdx >= 0) {
            if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
                capturingIdx = -1;
                return true;
            }
            if (keyCode == InputUtil.GLFW_KEY_DELETE || keyCode == InputUtil.GLFW_KEY_BACKSPACE) {
                keys.set(capturingIdx, "");
                capturingIdx = -1;
                RoleplayClientMod.config().save();
                return true;
            }
            InputUtil.Key key = InputUtil.fromKeyCode(keyCode, scanCode);
            if (key != InputUtil.UNKNOWN_KEY) {
                keys.set(capturingIdx, key.getTranslationKey());
                capturingIdx = -1;
                RoleplayClientMod.config().save();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (capturingIdx >= 0) {
            // Il tasto sinistro non è mai assegnabile: verrebbe premuto di continuo
            // in gioco e invierebbe il messaggio in chat a ogni click (rischio ban).
            if (button == 0) {
                capturingIdx = -1;
                return true;
            }
            InputUtil.Key key = InputUtil.Type.MOUSE.createFromCode(button);
            keys.set(capturingIdx, key.getTranslationKey());
            capturingIdx = -1;
            RoleplayClientMod.config().save();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
