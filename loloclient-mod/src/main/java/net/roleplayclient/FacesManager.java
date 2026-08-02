package net.roleplayclient;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gestione dei "volti conosciuti": badge "i" in chat e in tab,
 * avviso quando una faccia nota risulta online.
 * I volti sono salvati per nick di Minecraft, con un eventuale nome lore (RP)
 * usato per il riconoscimento in chat sui server RP (dove non compare il nick).
 */
public class FacesManager {
    private static final Map<String, Long> lastSeen = new HashMap<>();
    private static final Set<String> announced = new HashSet<>();
    /** Dopo quanto un giocatore non più in tab è considerato offline. */
    private static final long ONLINE_TTL_MS = 30_000;
    /** Le entry più vecchie di così vengono eliminate (la mappa era unbounded). */
    private static final long LAST_SEEN_PRUNE_MS = 5 * 60_000;

    /** Simbolo usato come badge per i volti conosciuti (la ℹ info; il variation selector U+FE0F non ha glifo). */
    public static final String BADGE = "\u2139";

    /** Messaggi di chat in attesa di badge, associati al volto riconosciuto dal sender. */
    private record ChatMarker(String text, RoleplayConfig.Face face, long ts) {
    }

    /** Oltre questa età un marker non può più corrispondere alla riga in arrivo. */
    private static final long CHAT_MARKER_TTL_MS = 10_000;

    /** Volto riconosciuto in una riga di chat, con la posizione (visiva) di fine nome. */
    public record ChatMatch(RoleplayConfig.Face face, String name, int end) {
    }

    private static final Deque<ChatMarker> pendingChat = new ArrayDeque<>();

    private FacesManager() {
    }

    /** Pattern precompilato: cleanCodes gira per ogni giocatore in tab a ogni tick. */
    private static final java.util.regex.Pattern COLOR_CODES =
            java.util.regex.Pattern.compile("[\u00a7&][0-9a-fA-Fk-orxK-ORX]");

    /** Rimuove i codici colore (sia "&" che "\u00a7", incluse le 16 cifre e k/l/m/n/o/r/x). */
    private static String cleanCodes(String s) {
        if (s == null) return "";
        return COLOR_CODES.matcher(s).replaceAll("");
    }

    private static java.util.List<RoleplayConfig.Face> faces() {
        return RoleplayClientMod.config().faces();
    }

    /** Nomi candidati di un volto: nome lore (RP) prima, poi il nick di Minecraft. */
    private static java.util.List<String> faceNames(RoleplayConfig.Face f) {
        java.util.List<String> names = new java.util.ArrayList<>(2);
        if (f.rpName() != null && !f.rpName().isEmpty()) names.add(f.rpName().trim());
        names.add(f.name().trim());
        return names;
    }

    /**
     * Nomi candidati di un volto per il riconoscimento in chat.
     * AtlantiRP: SOLO il nome lore (in chat compare solo quello);
     * se manca, ripiega sul nick. Generico: nome lore (se c'e') poi nick.
     */
    private static java.util.List<String> chatNames(RoleplayConfig.Face f) {
        java.util.List<String> names = new java.util.ArrayList<>(2);
        if (f.rpName() != null && !f.rpName().isEmpty()) names.add(f.rpName().trim());
        if (f.atlantis()) return names;
        names.add(f.name().trim());
        return names;
    }

    /** Indirizzo (hostname) del server su cui si e' connessi, minuscolo; "" se non determinabile. */
    public static String currentServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null && info.address != null) return info.address.toLowerCase();
        return "";
    }

    /**
     * True se il volto vale sul server corrente.
     * AtlantiRP: solo indirizzi che contengono "coralmc.it" o "play.atlantisrp.it".
     * Generico: se ha una lista di server, deve corrisponderne uno; lista vuota = ovunque.
     */
    public static boolean faceApplies(RoleplayConfig.Face f) {
        if (f == null) return false;
        if (f.atlantis()) {
            String s = currentServer();
            return s.contains("coralmc.it") || s.contains("play.atlantisrp.it");
        }
        List<String> servers = f.servers();
        if (servers == null || servers.isEmpty()) return true;
        String cur = currentServer();
        if (cur.isEmpty()) return false;
        for (String sv : servers) {
            if (sv == null) continue;
            String t = sv.trim().toLowerCase();
            if (!t.isEmpty() && cur.contains(t)) return true;
        }
        return false;
    }

    /** Cerca il volto (case-insensitive) il cui nick o nome RP corrisponda esattamente a "name". */
    public static RoleplayConfig.Face findFace(String name) {
        if (name == null) return null;
        for (RoleplayConfig.Face f : faces()) {
            if (!faceApplies(f)) continue;
            if (f.name().equalsIgnoreCase(name)) return f;
            if (f.rpName() != null && !f.rpName().isEmpty() && f.rpName().equalsIgnoreCase(name)) return f;
        }
        return null;
    }

    /** Cerca il volto il cui nick o nome RP compare come parola nel display name mostrato in tab. */
    public static RoleplayConfig.Face findTabFace(String displayName) {
        String s = cleanCodes(displayName);
        if (s.isEmpty()) return null;
        RoleplayConfig.Face best = null;
        int bestLen = 0;
        for (RoleplayConfig.Face f : faces()) {
            if (!faceApplies(f)) continue;
            for (String n : faceNames(f)) {
                if (n.isEmpty() || n.length() > s.length()) continue;
                if (n.length() > bestLen && isContainedWord(s, n)) {
                    best = f;
                    bestLen = n.length();
                }
            }
        }
        return best;
    }

    /** True se "needle" compare in "hay" delimitato da non-lettere/cifre (case-insensitive). */
    private static boolean isContainedWord(String hay, String needle) {
        String lh = hay.toLowerCase();
        String ln = needle.toLowerCase();
        int from = 0;
        int i;
        while ((i = lh.indexOf(ln, from)) >= 0) {
            boolean beforeOk = i == 0 || !Character.isLetterOrDigit(lh.charAt(i - 1));
            int afterIdx = i + ln.length();
            boolean afterOk = afterIdx >= lh.length() || !Character.isLetterOrDigit(lh.charAt(afterIdx));
            if (beforeOk && afterOk) return true;
            from = i + 1;
        }
        return false;
    }

    /**
     * Cerca il volto a cui appartiene una riga di chat e la posizione in cui
     * finisce il nome (per inserire la "i" subito dopo). Usa i confini di parola:
     * in chat il nome puo' essere seguito da spazio, ":", ">", "]" ecc.
     */
    public static ChatMatch matchChatFace(String line) {
        if (line == null) return null;
        String s = cleanCodes(line);
        if (s.isEmpty()) return null;
        String ls = s.toLowerCase();
        ChatMatch best = null;
        for (RoleplayConfig.Face f : faces()) {
            if (!faceApplies(f)) continue;
            for (String n : chatNames(f)) {
                if (n.isEmpty() || n.length() > s.length()) continue;
                String ln = n.toLowerCase();
                int from = 0;
                int i;
                while ((i = ls.indexOf(ln, from)) >= 0) {
                    int end = i + n.length();
                    boolean beforeOk = i == 0 || !Character.isLetterOrDigit(ls.charAt(i - 1));
                    boolean afterOk = end >= ls.length() || !Character.isLetterOrDigit(ls.charAt(end));
                    if (beforeOk && afterOk) {
                        if (best == null || n.length() > best.name.length()) {
                            best = new ChatMatch(f, n, end);
                        }
                        break;
                    }
                    from = i + 1;
                }
            }
        }
        return best;
    }

    /** Testo del hover sulla "i": solo la descrizione, poi la riga dorata col tasto Volti. */
    public static String hoverText(RoleplayConfig.Face f) {
        StringBuilder sb = new StringBuilder();
        if (f.description() != null && !f.description().isEmpty()) sb.append(f.description());
        sb.append("\n\u00a76Questo è un volto conosciuto. Impostane nuovi tramite ")
                .append(RoleplayClientMod.VOLTI_KEY.getBoundKeyLocalizedText().getString());
        return sb.toString();
    }

    public static boolean isKnown(String name) {
        return findFace(name) != null;
    }

    /** Restituisce il nome come salvato in config, oppure null. */
    public static String getKnown(String name) {
        RoleplayConfig.Face f = findFace(name);
        return f != null ? f.name() : null;
    }

    /** Nome mostrato in tab per un giocatore (il display name del server, se presente). */
    public static String displayName(PlayerListEntry entry) {
        Text t = entry.getDisplayName();
        return t != null ? t.getString() : entry.getProfile().getName();
    }

    /**
     * Chiamato a ogni messaggio di chat. Se il mittente è un giocatore noto
     * (nick reale dal profilo, indipendente da come il server lo mostra nella riga),
     * registra l'associazione testo->volto per il badge in ChatHud.
     */
    public static void onChat(Text message, SignedMessage signedMessage, GameProfile sender,
                              MessageType.Parameters params, Instant receptionTimestamp) {
        if (sender == null || message == null) return;
        if (!RoleplayClientMod.config().isEnabled("volti")) return;
        RoleplayConfig.Face face = findFace(sender.getName());
        if (face == null) return;
        pruneStaleMarkers();
        pendingChat.addLast(new ChatMarker(message.getString(), face, System.currentTimeMillis()));
        while (pendingChat.size() > 100) pendingChat.removeFirst();
    }

    private static void pruneStaleMarkers() {
        long cutoff = System.currentTimeMillis() - CHAT_MARKER_TTL_MS;
        while (!pendingChat.isEmpty() && pendingChat.peekFirst().ts() < cutoff) {
            pendingChat.removeFirst();
        }
    }

    /**
     * Consuma l'eventuale volto registrato per questo testo via sender (il badge va messo una sola volta).
     * L'evento CHAT porta il contenuto NON decorato mentre ChatHud riceve la riga
     * decorata ("&lt;Nome&gt; testo"): il confronto è quindi per contenimento, non
     * per uguaglianza (che non corrispondeva praticamente mai).
     */
    public static RoleplayConfig.Face consumeChatFace(String text) {
        if (text == null || pendingChat.isEmpty()) return null;
        pruneStaleMarkers();
        var it = pendingChat.descendingIterator();
        while (it.hasNext()) {
            ChatMarker m = it.next();
            if (!m.text().isEmpty() && (text.equals(m.text()) || text.contains(m.text()))) {
                it.remove();
                return m.face();
            }
        }
        return null;
    }

    private static Text badgeText(RoleplayConfig.Face f) {
        return Text.literal(" " + BADGE).styled(st -> st
                .withColor(net.minecraft.util.Formatting.AQUA)
                .withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(Text.literal(hoverText(f)))));
    }

    /** Mette la "i" del volto all'inizio del messaggio, davanti a tutto il testo. */
    public static MutableText prependBadge(Text message, RoleplayConfig.Face face) {
        MutableText out = Text.literal("").setStyle(message.getStyle());
        out.append(badgeText(face));
        out.append(message.copy());
        return out;
    }

    public static boolean isOnline(String name) {
        // La sola presenza della chiave non basta: le entry restano dopo la
        // disconnessione, quindi va confrontato il timestamp.
        Long ts = lastSeen.get(name.toLowerCase());
        return ts != null && System.currentTimeMillis() - ts < ONLINE_TTL_MS;
    }

    /** Controlla la lista giocatori: quando una faccia nota risulta presente mostra un avviso. */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || client.world == null) return;
        if (!RoleplayClientMod.config().isEnabled("volti")) return;

        long now = System.currentTimeMillis();
        Set<String> onlineNow = new HashSet<>();
        for (PlayerListEntry e : client.getNetworkHandler().getListedPlayerListEntries()) {
            String name = displayName(e);
            String lower = name.toLowerCase();
            lastSeen.put(lower, now);
            RoleplayConfig.Face face = findTabFace(name);
            if (face != null && !announced.contains(lower)) {
                announced.add(lower);
                RoleplayClientMod.showToast("Volto conosciuto online: " + face.name());
            }
            onlineNow.add(lower);
        }
        announced.retainAll(onlineNow);
        // Pruning: senza, su server grandi con prefissi/rank che cambiano la
        // mappa cresce senza limite per tutta la sessione.
        lastSeen.values().removeIf(ts -> now - ts > LAST_SEEN_PRUNE_MS);
    }
}
