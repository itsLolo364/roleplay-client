package net.roleplayclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.roleplayclient.modules.FacesManager;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Badge "i" accanto al nome dei volti conosciuti nella tab giocatori (lista premendo Tab).
 */
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "renderLatencyIcon(Lnet/minecraft/client/gui/DrawContext;IIILnet/minecraft/client/network/PlayerListEntry;)V",
            at = @At("HEAD"))
    private void roleplayclient_tabBadge(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (!RoleplayClientMod.config().isEnabled("volti")) return;
        String display = FacesManager.displayName(entry);
        RoleplayConfig.Face face = FacesManager.findTabFace(display);
        if (face == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int skinOffset = 0;
        if (client.getNetworkHandler() != null) {
            PlayerListEntry self = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (self != null && self.getSkinTextures() != null) skinOffset = 9;
        }
        int nameX = x + skinOffset;
        int nameW = client.textRenderer.getWidth(Text.literal(display));
        int ix = nameX + nameW + 3;
        Text badge = Text.literal(FacesManager.BADGE);
        context.drawTextWithShadow(client.textRenderer, badge, ix, y, 0xFF247CE2);
        int bw = client.textRenderer.getWidth(badge);

        int mx = (int) (client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth());
        int my = (int) (client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight());
        if (mx >= ix && mx <= ix + bw && my >= y && my <= y + 16) {
            context.drawHoverEvent(client.textRenderer, Style.EMPTY.withHoverEvent(
                    new HoverEvent.ShowText(Text.literal(FacesManager.hoverText(face)))), mx, my);
        }
    }
}
