package net.roleplayclient.modules;
import net.roleplayclient.RoleplayClientMod;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SceneLogManager {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Executor LOG_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final List<String> buffer = new ArrayList<>();
    private static boolean dirty = false;

    private SceneLogManager() {
    }

    public static void onChat(Text message, SignedMessage signedMessage, GameProfile sender,
                              MessageType.Parameters params, Instant receptionTimestamp) {
        if (sender == null) return;
        if (!RoleplayClientMod.config().isEnabled("logscene")) return;
        String text = message.getString();
        if (!text.startsWith("*")) return;
        String line = LocalDateTime.now().format(TS) + " | " + sender.getName() + ": " + text + System.lineSeparator();
        boolean shouldFlush;
        synchronized (buffer) {
            buffer.add(line);
            shouldFlush = !dirty;
            dirty = true;
        }
        if (shouldFlush) {
            LOG_EXECUTOR.execute(() -> {
                List<String> toWrite;
                synchronized (buffer) {
                    if (buffer.isEmpty()) return;
                    toWrite = new ArrayList<>(buffer);
                    buffer.clear();
                    dirty = false;
                }
                try {
                    Path f = FabricLoader.getInstance().getGameDir().resolve("roleplay-client-scene-log.txt");
                    Files.createDirectories(f.getParent());
                    Files.write(f, toWrite, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (Exception e) {
                    System.err.println("[RoleplayClient] Errore scene log: " + e.getMessage());
                }
            });
        }
    }
}
