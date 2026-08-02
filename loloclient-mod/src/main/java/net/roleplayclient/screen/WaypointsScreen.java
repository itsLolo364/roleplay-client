package net.roleplayclient.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;

import java.util.List;

/**
 * Gestione dei waypoint: aggiungi la posizione attuale, rinomina, rimuovi.
 */
public class WaypointsScreen extends RoleplayListScreen {
    private final List<RoleplayConfig.Waypoint> waypoints;
    private TextFieldWidget field;
    private ButtonWidget addBtn;

    public WaypointsScreen(Screen parent) {
        super(parent, "Waypoint");
        this.waypoints = RoleplayClientMod.config().waypoints();
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height - 44;
        field = new TextFieldWidget(this.textRenderer, 12, y, this.width - 360, 20, Text.literal(""));
        field.setMaxLength(40);
        field.setPlaceholder(Text.literal("Nome waypoint"));
        this.addDrawableChild(field);

        addBtn = ButtonWidget.builder(Text.literal("Aggiungi attuale"), b -> addCurrent())
                .dimensions(this.width - 340, y, 140, 20)
                .build();
        this.addDrawableChild(addBtn);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Aggiorna"), b -> renameSelected())
                .dimensions(this.width - 192, y, 80, 20)
                .build());
    }

    private void addCurrent() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            RoleplayClientMod.showToast("Devi essere in gioco per salvare la posizione");
            return;
        }
        String name = field.getText().trim();
        if (name.isEmpty()) name = "Waypoint " + (waypoints.size() + 1);
        String world = client.world.getRegistryKey().getValue().toString();
        waypoints.add(new RoleplayConfig.Waypoint(name, world, client.player.getX(), client.player.getY(), client.player.getZ()));
        field.setText("");
        RoleplayClientMod.config().save();
    }

    private void renameSelected() {
        String v = field.getText().trim();
        if (v.isEmpty() || selectedRowIdx < 0 || selectedRowIdx >= waypoints.size()) return;
        RoleplayConfig.Waypoint w = waypoints.get(selectedRowIdx);
        waypoints.set(selectedRowIdx, new RoleplayConfig.Waypoint(v, w.world(), w.x(), w.y(), w.z()));
        selectedRowIdx = -1;
        field.setText("");
        addBtn.setMessage(Text.literal("Aggiungi attuale"));
        RoleplayClientMod.config().save();
    }

    @Override
    protected int rowCount() {
        return waypoints.size();
    }

    @Override
    protected String rowLabel(int i) {
        RoleplayConfig.Waypoint w = waypoints.get(i);
        return w.name() + " - " + w.world() + " @ " + Math.round(w.x()) + " " + Math.round(w.y()) + " " + Math.round(w.z());
    }

    @Override
    protected boolean rowDeletable(int i) {
        return true;
    }

    @Override
    protected void onRowSelect(int i) {
        selectedRowIdx = i;
        field.setText(waypoints.get(i).name());
        field.setFocused(true);
        addBtn.setMessage(Text.literal("Aggiungi attuale"));
    }

    @Override
    protected void onRowDelete(int i) {
        waypoints.remove(i);
        if (selectedRowIdx == i) selectedRowIdx = -1;
        RoleplayClientMod.config().save();
    }
}
