package net.roleplayclient.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;
import net.roleplayclient.modules.FacesManager;
import net.roleplayclient.modules.MentionsManager;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Aggiunge alla chat il badge "i" (con nota al passaggio del mouse) per i
 * messaggi dei volti conosciuti e il segno "<-" quando il proprio nome
 * viene menzionato.
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1)
    private Text roleplayclient_chatMarkers(Text message) {
        String s = message.getString();
        RoleplayConfig.Face face = null;
        if (RoleplayClientMod.config().isEnabled("volti")) {
            // Prima il percorso affidabile (marker dal sender reale), poi il
            // fallback euristico sul testo della riga.
            face = FacesManager.consumeChatFace(s);
            if (face == null) {
                FacesManager.ChatMatch m = FacesManager.matchChatFace(s);
                if (m != null) face = m.face();
            }
        }
        boolean mention = RoleplayClientMod.config().isEnabled("menzioni") && MentionsManager.consume(s);
        if (face == null && !mention) return message;

        var out = face != null ? FacesManager.prependBadge(message, face) : message.copy();
        if (mention) {
            out.append(Text.literal(" <-").formatted(Formatting.YELLOW));
        }
        return out;
    }
}
