package net.roleplayclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.roleplayclient.modules.ChatSearchManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Ricerca chat: disegna l'overlay dei risultati dopo il render della chat.
 */
@Mixin(ChatHud.class)
public class ChatHudSearchMixin {

    @Shadow
    @Final
    private List<ChatHudLine.Visible> visibleMessages;

    @Inject(method = "render", at = @At("TAIL"))
    private void roleplayclient_searchOverlay(DrawContext ctx, int i, int j, int k, boolean bl, CallbackInfo ci) {
        ChatSearchManager.renderOverlay(ctx, this.visibleMessages);
    }
}
