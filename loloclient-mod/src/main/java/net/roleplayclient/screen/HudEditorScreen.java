package net.roleplayclient.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.roleplayclient.Packages;
import net.roleplayclient.Packages.Pkg;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;
import net.roleplayclient.gui.GlassUi;

import static net.roleplayclient.gui.GlassUi.*;

/**
 * Editor HUD stile launcher: riquadri glass trascinabili + header glass.
 */
public class HudEditorScreen extends Screen {
    private static final int BOX_W = 140;
    private static final int BOX_H = 28;

    private final Screen parent;
    private String dragging = null;
    private float grabDX = 0;
    private float grabDY = 0;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Editor HUD"));
        this.parent = parent;
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
        super.render(ctx, mx, my, delta);

        GlassUi.panel(ctx, 0, 0, this.width, 52);
        GlassUi.eyebrow(ctx, this.textRenderer, "Roleplay Client", 18, 10);
        ctx.drawText(this.textRenderer, "Editor HUD", 18, 26, TEXT, true);

        String hint = "Trascina i riquadri — ESC salva e chiude";
        int hw = this.textRenderer.getWidth(hint) + 24;
        int hx = Math.min(this.width - hw - 16, this.width - hw - 16);
        if (hx < 160) {
            ctx.drawText(this.textRenderer, hint, 18, 40, MUTED, false);
        } else {
            GlassUi.chip(ctx, this.width - hw - 16, 12, hw, 28, GLASS_1);
            ctx.drawText(this.textRenderer, hint, this.width - hw - 4, 22, MUTED, false);
        }

        RoleplayConfig cfg = RoleplayClientMod.config();
        MinecraftClient client = MinecraftClient.getInstance();
        float sw = client.getWindow().getScaledWidth();
        float sh = client.getWindow().getScaledHeight();

        for (String id : cfg.positions().keySet()) {
            if (!cfg.isEnabled(id)) continue;
            var pos = cfg.getPosition(id);
            int x = Math.round(pos.x() * sw);
            int y = Math.round(pos.y() * sh);
            // keep boxes visible under header
            y = Math.max(56, y);
            x = Math.max(0, Math.min(x, (int) sw - BOX_W));

            boolean hover = mx > x && mx < x + BOX_W && my > y && my < y + BOX_H;
            boolean active = hover || id.equals(dragging);
            GlassUi.navRow(ctx, x, y, BOX_W, BOX_H, active, hover);
            Pkg pkg = Packages.get(id);
            String label = pkg != null ? pkg.name() : id;
            ctx.drawText(this.textRenderer, label, x + 10, y + 10, active ? AMBER : TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        RoleplayConfig cfg = RoleplayClientMod.config();
        MinecraftClient client = MinecraftClient.getInstance();
        float sw = client.getWindow().getScaledWidth();
        float sh = client.getWindow().getScaledHeight();

        for (String id : cfg.positions().keySet()) {
            if (!cfg.isEnabled(id)) continue;
            var pos = cfg.getPosition(id);
            int x = Math.round(pos.x() * sw);
            int y = Math.max(56, Math.round(pos.y() * sh));
            x = Math.max(0, Math.min(x, (int) sw - BOX_W));
            if (mx > x && mx < x + BOX_W && my > y && my < y + BOX_H) {
                dragging = id;
                grabDX = (float) (mx - x);
                grabDY = (float) (my - y);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            float sw = client.getWindow().getScaledWidth();
            float sh = client.getWindow().getScaledHeight();
            float nx = (float) ((mx - grabDX) / sw);
            float ny = (float) ((my - grabDY) / sh);
            RoleplayClientMod.config().movePosition(dragging, nx, ny);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            RoleplayClientMod.config().save();
            dragging = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }
}
