package net.roleplayclient;

import net.minecraft.client.MinecraftClient;

/**
 * Modalità cinema: nasconde l'HUD per una visuale cinematografica.
 */
public class CinemaManager {
    private static boolean active = false;

    private CinemaManager() {
    }

    public static void toggle() {
        MinecraftClient client = MinecraftClient.getInstance();
        active = !active;
        client.options.hudHidden = active;
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
            if (client != null && client.options != null) client.options.hudHidden = false;
        }
    }
}
