package net.roleplayclient.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Gestione delle sveglie a orario reale: ora (HH:mm), messaggio, on/off.
 */
public class AlarmsScreen extends RoleplayListScreen {
    private static final Pattern HHMM = Pattern.compile("([01]?[0-9]|2[0-3]):[0-5][0-9]");

    private final List<RoleplayConfig.Alarm> alarms;
    private TextFieldWidget timeField;
    private TextFieldWidget msgField;
    private ButtonWidget addBtn;

    public AlarmsScreen(Screen parent) {
        super(parent, "Sveglie");
        this.alarms = RoleplayClientMod.config().alarms();
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height - 44;
        timeField = new TextFieldWidget(this.textRenderer, 12, y, 60, 20, Text.literal(""));
        timeField.setMaxLength(5);
        timeField.setPlaceholder(Text.literal("HH:mm"));
        this.addDrawableChild(timeField);

        msgField = new TextFieldWidget(this.textRenderer, 80, y, this.width - 300, 20, Text.literal(""));
        msgField.setMaxLength(60);
        msgField.setPlaceholder(Text.literal("Messaggio"));
        this.addDrawableChild(msgField);

        addBtn = ButtonWidget.builder(Text.literal("Aggiungi"), b -> addOrUpdate())
                .dimensions(this.width - 210, y, 90, 20)
                .build();
        this.addDrawableChild(addBtn);
    }

    private void addOrUpdate() {
        String time = timeField.getText().trim();
        if (!HHMM.matcher(time).matches()) {
            RoleplayClientMod.showToast("Formato ora non valido (usare HH:mm)");
            return;
        }
        String msg = msgField.getText().trim();
        if (msg.isEmpty()) msg = "Sveglia!";
        if (selectedRowIdx >= 0 && selectedRowIdx < alarms.size()) {
            RoleplayConfig.Alarm a = alarms.get(selectedRowIdx);
            alarms.set(selectedRowIdx, new RoleplayConfig.Alarm(time, msg, a.enabled()));
            selectedRowIdx = -1;
            addBtn.setMessage(Text.literal("Aggiungi"));
        } else {
            alarms.add(new RoleplayConfig.Alarm(time, msg, true));
        }
        timeField.setText("");
        msgField.setText("");
        RoleplayClientMod.config().save();
    }

    @Override
    protected int rowCount() {
        return alarms.size();
    }

    @Override
    protected String rowLabel(int i) {
        RoleplayConfig.Alarm a = alarms.get(i);
        return a.time() + "  -  " + a.message();
    }

    @Override
    protected String rowToggleLabel(int i) {
        return alarms.get(i).enabled() ? "ON" : "OFF";
    }

    @Override
    protected void onRowToggle(int i) {
        RoleplayConfig.Alarm a = alarms.get(i);
        alarms.set(i, new RoleplayConfig.Alarm(a.time(), a.message(), !a.enabled()));
        RoleplayClientMod.config().save();
    }

    @Override
    protected boolean rowDeletable(int i) {
        return true;
    }

    @Override
    protected void onRowSelect(int i) {
        selectedRowIdx = i;
        RoleplayConfig.Alarm a = alarms.get(i);
        timeField.setText(a.time());
        msgField.setText(a.message());
        timeField.setFocused(true);
        addBtn.setMessage(Text.literal("Aggiorna"));
    }

    @Override
    protected void onRowDelete(int i) {
        alarms.remove(i);
        if (selectedRowIdx == i) selectedRowIdx = -1;
        RoleplayClientMod.config().save();
    }
}
