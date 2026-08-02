package net.roleplayclient;

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
 */
public class AlarmManager {
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<Integer> fired = new HashSet<>();
    private static String lastMinute = "";

    private AlarmManager() {
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (!RoleplayClientMod.config().isEnabled("sveglie")) return;

        String minute = LocalTime.now().format(HM);
        if (!minute.equals(lastMinute)) {
            fired.clear();
            lastMinute = minute;
        }

        List<RoleplayConfig.Alarm> alarms = RoleplayClientMod.config().alarms();
        for (int i = 0; i < alarms.size(); i++) {
            RoleplayConfig.Alarm a = alarms.get(i);
            if (!a.enabled()) continue;
            if (a.time().equals(minute) && !fired.contains(i)) {
                fired.add(i);
                RoleplayClientMod.showToast("Sveglia: " + a.message());
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.0f));
            }
        }
    }
}
