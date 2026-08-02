package net.roleplayclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.roleplayclient.FacesManager;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tab.bettertab.tabList.TabEntry;

import java.util.List;

/**
 * Badge "i" accanto al nome dei volti conosciuti nella tab di Better Tab.
 * Better Tab sostituisce completamente la renderizzazione vanilla della tab,
 * quindi serve questo hook: il badge viene disegnato subito dopo il nome
 * dell'entry e mostra la descrizione al passaggio del mouse.
 */
@Mixin(TabEntry.class)
public class BetterTabTabEntryMixin {
    @Shadow public Text name;
    @Shadow public int textWidth;
    @Shadow private int textStartX;
    @Shadow private List<OrderedText> lines;
    @Shadow private boolean lineEntry;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;III)V", at = @At("TAIL"))
    private void roleplayclient_tabBadge(DrawContext context, int x, int y, int width, CallbackInfo ci) {
        if (!RoleplayClientMod.config().isEnabled("volti")) return;
        if (this.lineEntry || this.name == null) return;

        RoleplayConfig.Face face = FacesManager.findTabFace(this.name.getString());
        if (face == null) return;

        int lastLine = this.lines.size() > 1 ? this.lines.size() - 1 : 0;
        int ix = x + this.textStartX + this.textWidth + 3;
        int iy = y + 2 + lastLine;

        MinecraftClient client = MinecraftClient.getInstance();
        Text badge = Text.literal(FacesManager.BADGE);
        context.drawTextWithShadow(client.textRenderer, badge, ix, iy, 0xFF247CE2);
        int bw = client.textRenderer.getWidth(badge);

        int mx = (int) (client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth());
        int my = (int) (client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight());
        if (mx >= ix && mx <= ix + bw && my >= iy - 8 && my <= iy + 2) {
            context.drawHoverEvent(client.textRenderer, Style.EMPTY.withHoverEvent(
                    new HoverEvent.ShowText(Text.literal(FacesManager.hoverText(face)))), mx, my);
        }
    }
}
