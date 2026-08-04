package net.roleplayclient.modules;
import net.roleplayclient.RoleplayClientMod;

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

    /** True se "name" compare in "text" come parola intera (case-insensitive). */
    private static boolean containsWord(String text, String name) {
        return java.util.regex.Pattern
                .compile("(?<![A-Za-z0-9_])" + java.util.regex.Pattern.quote(name) + "(?![A-Za-z0-9_])",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .find();
    }

    public static void onChat(Text message, SignedMessage signedMessage, GameProfile sender,
                              MessageType.Parameters params, Instant receptionTimestamp) {
        if (sender == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!RoleplayClientMod.config().isEnabled("menzioni")) return;

        // I propri messaggi tornano indietro con sender == te stesso: non sono menzioni.
        if (sender.getId() != null && sender.getId().equals(client.player.getUuid())) return;

        String ownName = client.player.getGameProfile().getName();
        String text = message.getString();
        if (ownName.isEmpty() || text.isEmpty()) return;
        if (containsWord(text, ownName)) {
            lastChatMessage = text;
            mention = true;
            RoleplayClientMod.showToast("Sei stato menzionato da " + sender.getName());
        }
    }

    /**
     * Consuma l'ultima menzione se corrisponde al messaggio appena mostrato in chat.
     * La riga in ChatHud è decorata ("&lt;Nome&gt; testo") mentre l'evento CHAT porta il
     * contenuto puro: il confronto è per contenimento.
     */
    public static boolean consume(String messageText) {
        if (mention && messageText != null && !lastChatMessage.isEmpty()
                && (messageText.equals(lastChatMessage) || messageText.contains(lastChatMessage))) {
            mention = false;
            lastChatMessage = "";
            return true;
        }
        return false;
    }
}
