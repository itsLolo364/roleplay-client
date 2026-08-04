package net.roleplayclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.roleplayclient.RoleplayClientMod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * RP Timers: timer per scene roleplay (voce fissa) e timer di reazione.
 * Il timer di reazione parte quando viene inviato un messaggio rapido
 * marcato come "minaccia": regola G2.8 del regolamento AtlantisRP (tempo
 * minimo di reazione alla controparte). Durata configurabile (default 1.5s).
 */
public final class TimersManager {
    /** timer attivo: label, momento di scadenza, se è un timer di reazione. */
    public record Timer(String label, long expiresAt, boolean reaction) {
    }

    private static final List<Timer> timers = new ArrayList<>();
    private static final List<Timer> expiredReactions = new ArrayList<>();

    private TimersManager() {
    }

    public static void start(String label, long seconds) {
        timers.add(new Timer(label, System.currentTimeMillis() + seconds * 1000, false));
    }

    /** Avvia il tempo di reazione per la controparte (durata dalla config). */
    public static void startReaction() {
        if (!RoleplayClientMod.config().isEnabled("rptimers")) return;
        float secs = RoleplayClientMod.config().getFloat("rptimers", "reactionSeconds", 1.5f);
        timers.add(new Timer("Tempo di reazione", System.currentTimeMillis() + (long) (secs * 1000), true));
        RoleplayClientMod.showToast("Tempo di reazione: " + secs + "s");
    }

    public static void tick() {
        if (timers.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<Timer> it = timers.iterator();
        while (it.hasNext()) {
            Timer t = it.next();
            if (t.expiresAt() <= now) {
                it.remove();
                if (t.reaction()) expiredReactions.add(t);
            }
        }
        if (!expiredReactions.isEmpty()) {
            for (Timer t : expiredReactions) {
                playPling();
                RoleplayClientMod.showToast("Tempo di reazione scaduto");
            }
            expiredReactions.clear();
        }
    }

    private static void playPling() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f));
    }

    public static List<Timer> active() {
        long now = System.currentTimeMillis();
        List<Timer> out = new ArrayList<>();
        for (Timer t : timers) {
            if (t.expiresAt() > now) out.add(t);
        }
        return out;
    }

    public static long reactionRemainingMs() {
        long now = System.currentTimeMillis();
        for (Timer t : timers) {
            if (t.reaction() && t.expiresAt() > now) return t.expiresAt() - now;
        }
        return -1;
    }

    public static boolean reactionActive() {
        return reactionRemainingMs() >= 0;
    }

    /** Formato mm:ss per il countdown. */
    public static String formatRemaining(long ms) {
        long total = Math.max(0, ms / 1000);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", total / 60, total % 60);
    }

    /** Formato "0.0s" per il countdown di reazione (sotto il secondo). */
    public static String formatReaction(long ms) {
        if (ms >= 1000) return String.format(java.util.Locale.ROOT, "%.1fs", ms / 1000.0);
        return String.format(java.util.Locale.ROOT, "%.1fs", ms / 1000.0);
    }
}
