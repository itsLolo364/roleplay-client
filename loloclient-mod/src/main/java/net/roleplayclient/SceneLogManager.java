package net.roleplayclient;

import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Diario delle scene: salva su un file di testo le frasi RP che iniziano
 * con "*" ricevute in chat, con data/ora e nome del giocatore.
 * Il file è roleplay-client-scene-log.txt nella cartella del gioco.
 */
public class SceneLogManager {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SceneLogManager() {
    }

    public static void onChat(Text message, SignedMessage signedMessage, GameProfile sender,
                              MessageType.Parameters params, Instant receptionTimestamp) {
        if (sender == null) return;
        if (!RoleplayClientMod.config().isEnabled("logscene")) return;
        String text = message.getString();
        if (!text.startsWith("*")) return;
        try {
            Path f = FabricLoader.getInstance().getGameDir().resolve("roleplay-client-scene-log.txt");
            Files.createDirectories(f.getParent());
            String line = LocalDateTime.now().format(TS) + " | " + sender.getName() + ": " + text + System.lineSeparator();
            Files.writeString(f, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Errore scene log: " + e.getMessage());
        }
    }
}
