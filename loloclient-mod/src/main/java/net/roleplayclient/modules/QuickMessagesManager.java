package net.roleplayclient.modules;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.roleplayclient.screen.QuickMessagesScreen;

import java.util.HashSet;
import java.util.List;

public class QuickMessagesManager {

    private static final int MAX_CHAT_LENGTH = 256;
    private static final long SEND_COOLDOWN_MS = 500;
    private static long lastSendMs = 0;
    // #24 fix: track per-key press state with InputUtil (GLFW-independent, menu-safe)
    private static final java.util.Map<String, Boolean> lastDown = new java.util.HashMap<>();
    private static boolean wasScreenOpen = false;

    private QuickMessagesManager() {
    }

    public static void openList() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new QuickMessagesScreen(client.currentScreen));
    }

    public static boolean send(String message) {
        return send(message, false);
    }

    /**
     * Invio di un messaggio rapido. Se il messaggio è marcato come minaccia,
     * parte anche il tempo di reazione della controparte (RP-Timers).
     */
    public static boolean send(String message, boolean threat) {
        if (!sendRaw(message)) return false;
        if (threat) TimersManager.startReaction();
        return true;
    }

    private static boolean sendRaw(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || message == null || message.isEmpty()) return false;
        var handler = client.getNetworkHandler();
        if (handler == null) return false;
        long now = System.currentTimeMillis();
        if (now - lastSendMs < SEND_COOLDOWN_MS) return false;
        lastSendMs = now;
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
     * Fix #24: conflict detection. Se il tasto è già assegnato a un altro
     * keybinding (vanilla o mod), l'invio viene bloccato: altrimenti premere
     * il tasto legato (es. W = avanti) manderebbe il messaggio mentre si cammina.
     */
    private static boolean isBoundElsewhere(InputUtil.Key key) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) return false;
        for (KeyBinding kb : client.options.allKeys) {
            if (kb.getBoundKeyTranslationKey().equals(key.getTranslationKey())) return true;
        }
        return false;
    }

    public static void tickKeys() {
        MinecraftClient client = MinecraftClient.getInstance();
        RoleplayConfig cfg = RoleplayClientMod.config();
        if (!cfg.isEnabled("rpmessages")) return;
        if (client.player == null) return;

        boolean screenOpen = client.currentScreen != null;
        List<String> msgs = cfg.quickMessages();
        List<String> keys = cfg.quickMessageKeys();

        long handle = client.getWindow().getHandle();

        for (int i = 0; i < msgs.size() && i < keys.size(); i++) {
            String translationKey = keys.get(i);
            if (translationKey == null || translationKey.isEmpty()) continue;

            // Fix #24: use InputUtil to check key state properly, no raw GLFW polling
            boolean down;
            InputUtil.Key key;
            try {
                key = InputUtil.fromTranslationKey(translationKey);
                down = InputUtil.isKeyPressed(handle, key.getCode());
            } catch (Exception e) {
                continue;
            }

            boolean was = lastDown.getOrDefault(translationKey, false);
            lastDown.put(translationKey, down);

            if (screenOpen || wasScreenOpen) continue;
            if (isBoundElsewhere(key)) {
                lastDown.put(translationKey, false);
                continue;
            }
            if (down && !was) {
                List<Boolean> threats = cfg.quickThreats();
                boolean threat = i < threats.size() && Boolean.TRUE.equals(threats.get(i));
                if (send(msgs.get(i), threat)) {
                    RoleplayClientMod.showToast("Inviato: " + msgs.get(i));
                }
            }
        }
        wasScreenOpen = screenOpen;

        // Fix #24: ripulisci lo stato dei tasti non più assegnati
        // (es. messaggio rapido eliminato) così la mappa non cresce all'infinito.
        lastDown.keySet().removeIf(k -> !new HashSet<>(keys).contains(k));
    }
}
