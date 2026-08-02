package net.roleplayclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Elenco delle mod installate nella cartella mods dell'istanza.
 * Read-only: le mod sono sempre attive, la gestione on/off è solo
 * per i pacchetti interni del client (vedi ModMenuScreen, tab Moduli).
 */
public class ModManager {

    public static final class InstalledMod {
        public String fileName;     // nome reale su disco
        public String displayName;
        public String version;
        public String description;
        public String id;
        public boolean protectedMod;

        public InstalledMod() {
        }
    }

    public static Path modsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    public static List<InstalledMod> list() {
        List<InstalledMod> out = new ArrayList<>();
        Path dir = modsDir();
        if (!Files.isDirectory(dir)) return out;

        Path[] files;
        try (var stream = Files.list(dir)) {
            files = stream.toArray(Path[]::new);
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Errore lettura mods: " + e.getMessage());
            return out;
        }

        for (Path p : files) {
            String name = p.getFileName().toString();
            if (!name.endsWith(".jar")) continue;

            InstalledMod m = new InstalledMod();
            m.fileName = name;

            JsonObject meta = readMeta(p);
            if (meta != null && meta.has("id")) {
                m.id = meta.get("id").getAsString();
                m.displayName = meta.has("name") ? meta.get("name").getAsString() : pretty(m.id);
                m.version = meta.has("version") ? meta.get("version").getAsString() : "";
                m.description = meta.has("description") ? meta.get("description").getAsString() : "";
            } else {
                m.id = name.substring(0, name.length() - 4);
                m.displayName = pretty(m.id);
                m.version = "";
                m.description = "";
            }
            m.protectedMod = "roleplay-client".equals(m.id) || "loloclient".equals(m.id) || "fabric-api".equals(m.id);
            out.add(m);
        }

        out.sort((a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
        return out;
    }

    public static boolean isProtected(InstalledMod m) {
        return m.protectedMod;
    }

    private static JsonObject readMeta(Path jar) {
        try (JarFile jf = new JarFile(jar.toFile())) {
            JarEntry e = jf.getJarEntry("fabric.mod.json");
            if (e == null) return null;
            try (InputStreamReader r = new InputStreamReader(jf.getInputStream(e), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(r).getAsJsonObject();
            }
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Errore lettura meta " + jar.getFileName() + ": " + e.getMessage());
            return null;
        }
    }

    private static String pretty(String s) {
        String r = s.replaceAll("[-_.]+", " ").trim();
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : r.toCharArray()) {
            if (c == ' ') {
                sb.append(' ');
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
