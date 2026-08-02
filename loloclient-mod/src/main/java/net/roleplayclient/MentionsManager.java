package net.roleplayclient;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;

import java.time.Instant;

/**
 * Avviso quando il proprio nome viene menzionato in chat.
 */
public class MentionsManager {
    private static String lastChatMessage = "";
    private static boolean mention = false;

    private MentionsManager() {
    }

    public static void onChat(Text message, SignedMessage signedMessage, GameProfile sender,
                              MessageType.Parameters params, Instant receptionTimestamp) {
        if (sender == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!RoleplayClientMod.config().isEnabled("menzioni")) return;

        String ownName = client.player.getGameProfile().getName();
        String text = message.getString();
        if (ownName.isEmpty() || text.isEmpty()) return;
        if (text.toLowerCase().contains(ownName.toLowerCase())) {
            lastChatMessage = text;
            mention = true;
            RoleplayClientMod.showToast("Sei stato menzionato da " + sender.getName());
        }
    }

    /** Consuma l'ultima menzione se corrisponde al messaggio appena mostrato in chat. */
    public static boolean consume(String messageText) {
        if (mention && messageText != null && messageText.equals(lastChatMessage)) {
            mention = false;
            return true;
        }
        return false;
    }
}
