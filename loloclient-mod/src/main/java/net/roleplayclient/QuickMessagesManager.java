package net.roleplayclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.roleplayclient.screen.QuickMessagesScreen;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Messaggi rapidi RP: frasi o comandi pronti da inviare in chat.
 * Ogni messaggio puo' avere un tasto assegnato: premendolo in gioco
 * si invia subito quel messaggio.
 */
public class QuickMessagesManager {

    private static final Map<String, Boolean> lastDown = new HashMap<>();
    private static boolean wasScreenOpen = false;
    // Limite chat vanilla; cooldown anti-spam tra invii da hotkey.
    private static final int MAX_CHAT_LENGTH = 256;
    private static final long SEND_COOLDOWN_MS = 500;
    private static long lastSendMs = 0;

    private QuickMessagesManager() {
    }

    public static void openList() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new QuickMessagesScreen(client.currentScreen));
    }

    /**
     * Invia il messaggio in chat, interpretando i comandi (testo che inizia con /).
     * @return true se il messaggio è stato inviato (false se in cooldown o non inviabile)
     */
    public static boolean send(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || message == null || message.isEmpty()) return false;
        var handler = client.getNetworkHandler();
        if (handler == null) return false;
        long now = System.currentTimeMillis();
        if (now - lastSendMs < SEND_COOLDOWN_MS) return false;
        lastSendMs = now;
        // Il server rifiuta (o disconnette per) messaggi oltre il limite vanilla:
        // tronca quelli caricati da config modificate a mano.
        if (message.length() > MAX_CHAT_LENGTH) message = message.substring(0, MAX_CHAT_LENGTH);
        if (message.startsWith("/")) {
            handler.sendChatCommand(message.substring(1));
        } else {
            handler.sendChatMessage(message);
        }
        return true;
    }

    public static List<String> list() {
        return RoleplayClientMod.config().quickMessages();
    }

    /**
     * Controlla nel tick i tasti assegnati ai messaggi rapidi: quando uno viene
     * premuto (bordo di pressione) invia il messaggio corrispondente.
     */
    public static void tickKeys() {
        MinecraftClient client = MinecraftClient.getInstance();
        RoleplayConfig cfg = RoleplayClientMod.config();
        if (!cfg.isEnabled("rpmessages")) return;
        if (client.player == null) return;

        boolean screenOpen = client.currentScreen != null;
        List<String> msgs = cfg.quickMessages();
        List<String> keys = cfg.quickMessageKeys();
        for (int i = 0; i < msgs.size() && i < keys.size(); i++) {
            String k = keys.get(i);
            if (k == null || k.isEmpty()) continue;
            InputUtil.Key key;
            try {
                key = InputUtil.fromTranslationKey(k);
            } catch (Exception e) {
                continue;
            }
            String id = i + ":" + k;
            boolean down = isDown(key);
            boolean was = lastDown.getOrDefault(id, false);
            lastDown.put(id, down);

            if (screenOpen) continue;
            // Quando un'eventuale schermata (es. la chat) e' appena stata chiusa,
            // lo stato del tasto resta "giu'" per un tick: non deve far partire
            // l'hotkey mentre si invia un messaggio con Invio.
            if (wasScreenOpen) continue;
            if (down && !was && send(msgs.get(i))) {
                RoleplayClientMod.showToast("Inviato: " + msgs.get(i));
            }
        }
        wasScreenOpen = screenOpen;
    }

    private static boolean isDown(InputUtil.Key key) {
        MinecraftClient client = MinecraftClient.getInstance();
        long handle = client.getWindow().getHandle();
        return switch (key.getCategory()) {
            case KEYSYM -> InputUtil.isKeyPressed(handle, key.getCode());
            case MOUSE -> GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }
}
