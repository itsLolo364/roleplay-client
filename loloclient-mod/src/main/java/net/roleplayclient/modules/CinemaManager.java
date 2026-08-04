package net.roleplayclient.modules;
import net.roleplayclient.RoleplayClientMod;

import net.minecraft.client.MinecraftClient;

/**
 * Modalità cinema: nasconde l'HUD per una visuale cinematografica.
 */
public class CinemaManager {
    private static boolean active = false;
    private static boolean prevF1Hidden = false;

    private CinemaManager() {
    }

    public static void toggle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;
        if (!active) {
            // attivazione: salva stato F1 attuale
            prevF1Hidden = client.options.hudHidden;
            active = true;
            client.options.hudHidden = true;
        } else {
            // disattivazione: ripristina stato F1 precedente (#40)
            active = false;
            client.options.hudHidden = prevF1Hidden;
        }
        RoleplayClientMod.showToast(active
                ? "Modalità cinema: HUD nascosto (premi il tasto per tornare)"
                : "Modalità cinema disattivata");
    }

    public static boolean isActive() {
        return active;
    }

    /** Se il pacchetto cinema viene disattivato, ripristina l'HUD. */
    public static void tick() {
        if (active && !RoleplayClientMod.config().isEnabled("cinema")) {
            active = false;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.options != null) {
                client.options.hudHidden = prevF1Hidden;
            }
        }
    }
}
