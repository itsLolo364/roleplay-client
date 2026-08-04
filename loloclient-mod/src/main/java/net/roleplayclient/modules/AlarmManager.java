package net.roleplayclient.modules;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.RoleplayConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sveglie a orario reale: quando l'ora coincide con quella impostata
 * viene mostrato un avviso e suona una campana.
 * Fix #12: chiave = time+message (non indice) → immune a riordini/eliminazioni.
 * Fix #11: il tempo è già in HH:mm dopo la normalizzazione in AlarmsScreen.
 */
public class AlarmManager {
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> fired = new HashSet<>();
    private static String lastMinute = "";

    private AlarmManager() {
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        if (!RoleplayClientMod.config().isEnabled("sveglie")) return;

        String minute = LocalTime.now().format(HM);
        if (!minute.equals(lastMinute)) {
            fired.clear();
            lastMinute = minute;
        }

        List<RoleplayConfig.Alarm> alarms = RoleplayClientMod.config().alarms();
        for (RoleplayConfig.Alarm a : alarms) {
            if (!a.enabled()) continue;
            // Fix #12: chiave stabile = orario + messaggio, immune a riordini
            String key = a.time() + "\0" + a.message();
            if (a.time().equals(minute) && !fired.contains(key)) {
                fired.add(key);
                RoleplayClientMod.showToast("Sveglia: " + a.message());
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.0f));
            }
        }
    }
}
