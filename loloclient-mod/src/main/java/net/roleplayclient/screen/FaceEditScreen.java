package net.roleplayclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;
import net.roleplayclient.gui.GlassUi;

import static net.roleplayclient.gui.GlassUi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor di un singolo volto conosciuto.
 * <ul>
 *   <li>Toggle "AtlantisRP": richiede nick, nome lore e descrizione; vale solo su coralmc.it / play.atlantisrp.it.</li>
 *   <li>Generico: chiede solo il nick ed eventuali server/IP (caselle aggiungibili); vuoto = vale ovunque.</li>
 * </ul>
 */
public class FaceEditScreen extends Screen {
    private static final int HEADER_H = 56;

    private final Screen parent;
    private final List<RoleplayConfig.Face> faces;
    private final RoleplayConfig.Face existing;

    private boolean atlantis;
    private String nick = "";
    private String lore = "";
    private String desc = "";
    private final List<String> serverValues = new ArrayList<>();

    private CheckboxWidget modeToggle;
    private TextFieldWidget nickField;
    private TextFieldWidget loreField;
    private TextFieldWidget descField;
    private final List<TextFieldWidget> serverFields = new ArrayList<>();
    private int hintY;
    private String hintText = "";

    public FaceEditScreen(Screen parent, List<RoleplayConfig.Face> faces, RoleplayConfig.Face existing) {
        super(Text.literal(existing == null ? "Nuovo volto conosciuto" : "Modifica volto conosciuto"));
        this.parent = parent;
        this.faces = faces;
        this.existing = existing;
        if (existing != null) {
            this.atlantis = existing.atlantis();
            this.nick = existing.name() == null ? "" : existing.name();
            this.lore = existing.rpName() == null ? "" : existing.rpName();
            this.desc = existing.description() == null ? "" : existing.description();
            if (existing.servers() != null) this.serverValues.addAll(existing.servers());
        }
    }

    @Override
    protected void init() {
        rebuild();
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

    private void capture() {
        if (nickField != null) nick = nickField.getText();
        if (loreField != null) lore = loreField.getText();
        if (descField != null) desc = descField.getText();
        serverValues.clear();
        for (TextFieldWidget sf : serverFields) serverValues.add(sf.getText());
    }

    private void addServer() {
        capture();
        serverValues.add("");
        rebuild();
    }

    private void removeServer(int idx) {
        capture();
        if (idx >= 0 && idx < serverValues.size()) serverValues.remove(idx);
        rebuild();
    }

    private void rebuild() {
        this.clearChildren();
        serverFields.clear();

        int x = this.width / 2 - 130;
        int y = HEADER_H + 20;

        modeToggle = CheckboxWidget.builder(Text.literal("Volto specifico AtlantisRP"), this.textRenderer)
                .pos(x, y)
                .checked(atlantis)
                .callback((cb, checked) -> {
                    capture();
                    atlantis = checked;
                    rebuild();
                })
                .build();
        this.addDrawableChild(modeToggle);
        y += 26;

        nickField = new TextFieldWidget(this.textRenderer, x, y, 260, 20, Text.literal(""));
        nickField.setMaxLength(40);
        nickField.setPlaceholder(Text.literal("Nick Minecraft"));
        nickField.setText(nick);
        this.addDrawableChild(nickField);
        y += 24;

        if (atlantis) {
            loreField = new TextFieldWidget(this.textRenderer, x, y, 260, 20, Text.literal(""));
            loreField.setMaxLength(40);
            loreField.setPlaceholder(Text.literal("Nome lore (RP)"));
            loreField.setText(lore);
            this.addDrawableChild(loreField);
            y += 24;

            descField = new TextFieldWidget(this.textRenderer, x, y, 260, 20, Text.literal(""));
            descField.setMaxLength(120);
            descField.setPlaceholder(Text.literal("Descrizione (hover sulla i)"));
            descField.setText(desc);
            this.addDrawableChild(descField);
            y += 26;

            hintText = "Vale solo su server coralmc.it / play.atlantisrp.it";
            hintY = y;
            y += 14;
        } else {
            if (serverValues.isEmpty()) serverValues.add("");
            for (int i = 0; i < serverValues.size(); i++) {
                TextFieldWidget sf = new TextFieldWidget(this.textRenderer, x, y, 210, 20, Text.literal(""));
                sf.setMaxLength(100);
                sf.setPlaceholder(Text.literal("Server/IP (es. play.example.it)"));
                sf.setText(serverValues.get(i));
                this.addDrawableChild(sf);
                serverFields.add(sf);
                final int idx = i;
                this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), b -> removeServer(idx))
                        .dimensions(x + 216, y, 44, 20)
                        .build());
                y += 24;
            }
            this.addDrawableChild(ButtonWidget.builder(Text.literal("+ Aggiungi server"), b -> addServer())
                    .dimensions(x, y, 260, 20)
                    .build());
            y += 26;

            hintText = "Vuoto = vale su ogni server";
            hintY = y;
            y += 14;
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal(existing == null ? "Aggiungi" : "Salva"), b -> save())
                .dimensions(x, y, 126, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Annulla"), b -> close())
                .dimensions(x + 134, y, 126, 20)
                .build());
    }

    private void save() {
        capture();
        String n = nick.trim();
        if (n.isEmpty()) return;
        List<String> servers = new ArrayList<>();
        for (String s : serverValues) {
            String t = s.trim();
            if (!t.isEmpty()) servers.add(t);
        }
        RoleplayConfig.Face f = new RoleplayConfig.Face(n, desc.trim(), lore.trim(), atlantis, servers);
        if (existing != null) {
            int idx = faces.indexOf(existing);
            if (idx >= 0) faces.set(idx, f);
        } else {
            faces.add(f);
        }
        RoleplayClientMod.config().save();
        close();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        GlassUi.background(ctx);
        GlassUi.panel(ctx, 0, 0, this.width, HEADER_H);
        GlassUi.eyebrow(ctx, this.textRenderer, "Roleplay Client", 18, 14);
        ctx.drawText(this.textRenderer, this.getTitle().getString(), 18, 30, TEXT, true);
        boolean closeHover = mx > this.width - 48 && my < HEADER_H;
        GlassUi.chip(ctx, this.width - 44, (HEADER_H - 28) / 2, 28, 28, closeHover ? CARD_HOVER : GLASS_1);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("x"), this.width - 30, (HEADER_H - 8) / 2, MUTED);

        super.render(ctx, mx, my, delta);

        if (!hintText.isEmpty()) {
            ctx.drawText(this.textRenderer, hintText, this.width / 2 - 130, hintY, MUTED, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx > this.width - 40 && my < HEADER_H) {
            close();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
