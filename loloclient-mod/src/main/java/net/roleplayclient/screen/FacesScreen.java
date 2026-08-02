package net.roleplayclient.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;

import java.util.List;

/**
 * Gestione dei volti conosciuti: aggiungi, modifica, rimuovi.
 * Il tasto "Aggiungi" (o il clic su una riga) apre l'editor
 * con toggle AtlantisRP e lista di server facoltativa.
 */
public class FacesScreen extends RoleplayListScreen {
    private final List<RoleplayConfig.Face> faces;

    public FacesScreen(Screen parent) {
        super(parent, "Volti conosciuti");
        this.faces = RoleplayClientMod.config().faces();
    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Aggiungi"), b -> openEditor(null))
                .dimensions(this.width - 220, this.height - 44, 100, 20)
                .build());
    }

    private void openEditor(RoleplayConfig.Face f) {
        if (this.client != null) this.client.setScreen(new FaceEditScreen(this, faces, f));
    }

    @Override
    protected int rowCount() {
        return faces.size();
    }

    @Override
    protected String rowLabel(int i) {
        RoleplayConfig.Face f = faces.get(i);
        StringBuilder sb = new StringBuilder();
        if (f.atlantis()) sb.append("[A] ");
        sb.append(f.name());
        if (f.rpName() != null && !f.rpName().isEmpty()) sb.append(" (").append(f.rpName()).append(")");
        if (f.description() != null && !f.description().isEmpty()) sb.append(" - ").append(f.description());
        String s = sb.toString();
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    @Override
    protected String rowSecondary(int i) {
        RoleplayConfig.Face f = faces.get(i);
        if (f.atlantis()) return "Atlantis";
        if (f.servers() == null || f.servers().isEmpty()) return "Ovunque";
        return "Server";
    }

    @Override
    protected boolean rowDeletable(int i) {
        return true;
    }

    @Override
    protected void onRowSelect(int i) {
        openEditor(faces.get(i));
    }

    @Override
    protected void onRowDelete(int i) {
        faces.remove(i);
        RoleplayClientMod.config().save();
    }
}
