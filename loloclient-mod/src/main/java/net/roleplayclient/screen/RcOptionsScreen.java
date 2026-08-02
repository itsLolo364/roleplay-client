package net.roleplayclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import net.roleplayclient.gui.GlassButton;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.gui.RcShell;

import java.util.ArrayList;
import java.util.List;

import static net.roleplayclient.gui.GlassUi.*;

/**
 * Hub Opzioni — header + griglia card responsiva (1/2 colonne).
 */
public class RcOptionsScreen extends Screen {
    private final Screen parent;
    private final GameOptions options;

    private final List<GlassButton> grid = new ArrayList<>();
    private GlassButton btnMods;
    private GlassButton btnDone;
    private RcShell shell;

    public RcOptionsScreen(Screen parent, GameOptions options) {
        super(Text.literal("Opzioni"));
        this.parent = parent;
        this.options = options;
    }

    @Override
    protected void init() {
        shell = RcShell.headerOnly(this.width, this.height);
        layoutButtons();
    }

    private void layoutButtons() {
        grid.clear();
        String[] labels = {
                "Personalizzazione skin", "Musica e suoni",
                "Impostazioni video", "Controlli",
                "Lingua", "Impostazioni chat",
                "Pacchetti risorse", "Accessibilità"
        };

        int pad = 18;
        int top = shell.headerH + 16;
        int availW = this.width - pad * 2;
        int cols = availW >= 520 ? 2 : 1;
        int gap = 10;
        int bw = (availW - gap * (cols - 1)) / cols;
        int bh = shell.compact ? 32 : 36;

        int y = top;
        for (int i = 0; i < labels.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = pad + col * (bw + gap);
            int by = y + row * (bh + gap);
            grid.add(new GlassButton(x, by, bw, bh, labels[i], GlassButton.Style.SECONDARY));
        }

        int rows = (labels.length + cols - 1) / cols;
        int after = top + rows * (bh + gap) + 8;
        int maxBottom = this.height - 24 - bh * 2 - gap;
        if (after > maxBottom) after = Math.max(top, maxBottom);

        int mw = Math.min(280, availW);
        btnMods = new GlassButton(this.width / 2 - mw / 2, after, mw, bh, "Moduli Roleplay Client", GlassButton.Style.PRIMARY);
        btnDone = new GlassButton(this.width / 2 - mw / 2, after + bh + gap, mw, bh, "Fatto", GlassButton.Style.SECONDARY);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (shell == null || shell.width != this.width || shell.height != this.height) {
            shell = RcShell.headerOnly(this.width, this.height);
            layoutButtons();
        }

        GlassUi.background(ctx);
        shell.drawHeader(ctx, this.textRenderer, mx, my, "Roleplay Client", "Opzioni", true);

        String tip = "Scegli una categoria. I sotto-menu usano lo stesso tema Liquid Glass.";
        GlassUi.drawWrapped(ctx, this.textRenderer, tip, 18, shell.headerH + 4, this.width - 36, MUTED);

        for (GlassButton b : grid) b.render(ctx, this.textRenderer, mx, my);
        btnMods.render(ctx, this.textRenderer, mx, my);
        btnDone.render(ctx, this.textRenderer, mx, my);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (shell != null && shell.hitClose(mx, my)) {
            close();
            return true;
        }

        for (int i = 0; i < grid.size(); i++) {
            if (grid.get(i).contains(mx, my)) {
                openCategory(i);
                return true;
            }
        }
        if (btnMods.contains(mx, my)) {
            this.client.setScreen(new ModMenuScreen(this));
            return true;
        }
        if (btnDone.contains(mx, my)) {
            close();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void openCategory(int i) {
        switch (i) {
            case 0 -> this.client.setScreen(new SkinOptionsScreen(this, options));
            case 1 -> this.client.setScreen(new SoundOptionsScreen(this, options));
            case 2 -> this.client.setScreen(new VideoOptionsScreen(this, this.client, options));
            case 3 -> this.client.setScreen(new ControlsOptionsScreen(this, options));
            case 4 -> this.client.setScreen(new LanguageOptionsScreen(this, options, this.client.getLanguageManager()));
            case 5 -> this.client.setScreen(new ChatOptionsScreen(this, options));
            case 6 -> {
                java.nio.file.Path packs = net.fabricmc.loader.api.FabricLoader.getInstance()
                        .getGameDir().resolve("resourcepacks");
                final Screen self = this;
                this.client.setScreen(new net.minecraft.client.gui.screen.pack.PackScreen(
                        this.client.getResourcePackManager(),
                        manager -> {
                            this.client.options.refreshResourcePacks(manager);
                            this.client.setScreen(self);
                        },
                        packs,
                        Text.translatable("resourcePack.title")));
            }
            case 7 -> this.client.setScreen(new AccessibilityOptionsScreen(this, options));
            default -> {
            }
        }
    }

    @Override
    public void close() {
        this.options.write();
        if (parent != null && this.client != null) this.client.setScreen(parent);
        else super.close();
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
