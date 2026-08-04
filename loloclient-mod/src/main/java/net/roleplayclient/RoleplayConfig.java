package net.roleplayclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurazione persistente del client: stato dei pacchetti, posizioni HUD,
 * volti conosciuti, messaggi rapidi, waypoint e sveglie.
 * Salvata in config/roleplay-client.json nella cartella di gioco.
 */
public class RoleplayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Posizione HUD normalizzata (0..1). */
    public record HudPos(float x, float y) {
    }

    public record Waypoint(String name, String world, double x, double y, double z) {
    }

    public record Alarm(String time, String message, boolean enabled) {
    }

    /**
     * Volto conosciuto.
     * <ul>
     *   <li>name: nick di Minecraft (sempre richiesto).</li>
     *   <li>description: descrizione mostrata al passaggio sulla "i" (usata dai volti AtlantisRP).</li>
     *   <li>rpName: nome lore (RP), usato per il riconoscimento in chat sui server RP.</li>
     *   <li>atlantis: se true il volto vale solo sui server coralmc.it / play.atlantisrp.it.</li>
     *   <li>servers: per i volti generici, elenco di server/IP su cui vale (vuoto = ovunque).</li>
     * </ul>
     */
    public record Face(String name, String description, String rpName, boolean atlantis, List<String> servers) {
        public Face(String name, String description) {
            this(name, description, "", false, List.of());
        }

        public Face(String name, String description, String rpName) {
            this(name, description, rpName, false, List.of());
        }
    }

    private final Map<String, Boolean> modules = new HashMap<>();
    private final Map<String, HudPos> positions = new LinkedHashMap<>();
    private final List<Face> faces = new ArrayList<>();
    private final List<String> quickMessages = new ArrayList<>();
    private final List<String> quickKeys = new ArrayList<>();
    private final List<Boolean> quickThreats = new ArrayList<>();
    private final List<Waypoint> waypoints = new ArrayList<>();
    private final List<Alarm> alarms = new ArrayList<>();
    private JsonObject settings = new JsonObject();

    public RoleplayConfig() {
        for (String m : Packages.all().keySet()) modules.put(m, true);
        positions.put("fps", new HudPos(0.005f, 0.005f));
        positions.put("cps", new HudPos(0.005f, 0.03f));
        positions.put("coords", new HudPos(0.005f, 0.055f));
        positions.put("clock", new HudPos(0.005f, 0.08f));
        positions.put("ping", new HudPos(0.005f, 0.105f));
        positions.put("oramondo", new HudPos(0.005f, 0.13f));
        positions.put("armor", new HudPos(0.005f, 0.16f));
        positions.put("waypoint", new HudPos(0.005f, 0.19f));
        positions.put("rptimers", new HudPos(0.005f, 0.22f));
        positions.put("rpstopwatch", new HudPos(0.005f, 0.25f));
        positions.put("sessiontime", new HudPos(0.005f, 0.28f));
        positions.put("clipready", new HudPos(0.005f, 0.31f));
        positions.put("desyncalert", new HudPos(0.005f, 0.34f));
        positions.put("watermark", new HudPos(0.5f, 0.94f));
        positions.put("reaction", new HudPos(0.5f, 0.62f));
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("roleplay-client.json");
    }

    /**
     * Migra la vecchia config "loloclient.json" (nome storico) nella nuova
     * "roleplay-client.json": una tantum, solo se il nuovo file non esiste.
     */
    private static Path existingSource() {
        Path p = path();
        if (Files.exists(p)) return p;
        Path old = FabricLoader.getInstance().getConfigDir().resolve("loloclient.json");
        return Files.exists(old) ? old : null;
    }

    /**
     * Conserva una copia del file corrotto: il prossimo save() lo sovrascriverebbe
     * e l'utente perderebbe ogni possibilità di recupero manuale.
     */
    private static void backupCorrupt(Path src) {
        try {
            Path bak = src.resolveSibling(src.getFileName().toString() + ".bak");
            Files.copy(src, bak, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[RoleplayClient] Config corrotta, copia salvata in " + bak);
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Impossibile salvare il backup della config corrotta: " + e.getMessage());
        }
    }

    public static RoleplayConfig load() {
        RoleplayConfig c = new RoleplayConfig();
        Path src;
        try {
            src = existingSource();
        } catch (Exception e) {
            return c;
        }
        if (src == null) return c;
        boolean migrated = !src.equals(path());

        JsonObject obj;
        try {
            String json = Files.readString(src, StandardCharsets.UTF_8);
            obj = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Errore caricamento config: " + e.getMessage());
            e.printStackTrace();
            backupCorrupt(src);
            return c;
        }

        // Ogni sezione (e ogni elemento delle liste) viene letta con il proprio
        // try/catch: un dato malformato non deve buttare via tutto il resto,
        // perché il primo save() successivo riscriverebbe il file troncato.
        try {
            JsonObject mods = obj.getAsJsonObject("modules");
            if (mods != null) {
                for (String m : Packages.all().keySet()) {
                    if (mods.has(m)) c.modules.put(m, mods.get(m).getAsBoolean());
                }
            }
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Config: sezione modules ignorata: " + e.getMessage());
        }
        try {
            JsonObject pos = obj.getAsJsonObject("positions");
            if (pos != null) {
                for (Map.Entry<String, com.google.gson.JsonElement> e : pos.entrySet()) {
                    try {
                        JsonObject o = e.getValue().getAsJsonObject();
                        if (o.has("x") && o.has("y")) {
                            c.positions.put(e.getKey(), new HudPos((float) o.get("x").getAsDouble(), (float) o.get("y").getAsDouble()));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Config: sezione positions ignorata: " + e.getMessage());
        }
        try {
            JsonArray fc = obj.getAsJsonArray("faces");
            if (fc != null) {
                for (var el : fc) {
                    try {
                        if (el.isJsonPrimitive()) {
                            c.faces.add(new Face(el.getAsString(), ""));
                        } else {
                            JsonObject o = el.getAsJsonObject();
                            List<String> servers = new ArrayList<>();
                            JsonArray sa = o.getAsJsonArray("servers");
                            if (sa != null) {
                                for (var s : sa) {
                                    if (s.isJsonPrimitive()) servers.add(s.getAsString());
                                }
                            }
                            c.faces.add(new Face(
                                    o.has("name") ? o.get("name").getAsString() : "",
                                    o.has("description") ? o.get("description").getAsString() : "",
                                    o.has("rpName") ? o.get("rpName").getAsString() : "",
                                    o.has("atlantis") && o.get("atlantis").getAsBoolean(),
                                    servers));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Config: sezione faces ignorata: " + e.getMessage());
        }
        try {
            JsonArray qm = obj.getAsJsonArray("quickMessages");
            if (qm != null) for (var el : qm) c.quickMessages.add(el.getAsString());
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Config: sezione quickMessages ignorata: " + e.getMessage());
        }
        try {
            JsonArray qk = obj.getAsJsonArray("quickKeys");
            if (qk != null) for (var el : qk) c.quickKeys.add(el.getAsString());
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Config: sezione quickKeys ignorata: " + e.getMessage());
        }
        while (c.quickKeys.size() < c.quickMessages.size()) c.quickKeys.add("");
        while (c.quickKeys.size() > c.quickMessages.size()) c.quickKeys.remove(c.quickKeys.size() - 1);
        try {
            JsonObject st = obj.getAsJsonObject("settings");
            if (st != null) c.settings = st;
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Config: sezione settings ignorata: " + e.getMessage());
        }
        try {
            JsonArray qt = obj.getAsJsonArray("quickThreats");
            if (qt != null) {
                for (var el : qt) {
                    try {
                        c.quickThreats.add(el.getAsBoolean());
                    } catch (Exception ignored) {
                        c.quickThreats.add(false);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Config: sezione quickThreats ignorata: " + e.getMessage());
        }
        while (c.quickThreats.size() < c.quickMessages.size()) c.quickThreats.add(false);
        while (c.quickThreats.size() > c.quickMessages.size()) c.quickThreats.remove(c.quickThreats.size() - 1);
        try {
            JsonArray wp = obj.getAsJsonArray("waypoints");
            if (wp != null) {
                for (var el : wp) {
                    try {
                        JsonObject o = el.getAsJsonObject();
                        c.waypoints.add(new Waypoint(
                                o.has("name") ? o.get("name").getAsString() : "",
                                o.has("world") ? o.get("world").getAsString() : "",
                                o.has("x") ? o.get("x").getAsDouble() : 0,
                                o.has("y") ? o.get("y").getAsDouble() : 0,
                                o.has("z") ? o.get("z").getAsDouble() : 0));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Config: sezione waypoints ignorata: " + e.getMessage());
        }
        try {
            JsonArray al = obj.getAsJsonArray("alarms");
            if (al != null) {
                for (var el : al) {
                    try {
                        JsonObject o = el.getAsJsonObject();
                        c.alarms.add(new Alarm(
                                o.has("time") ? o.get("time").getAsString() : "00:00",
                                o.has("message") ? o.get("message").getAsString() : "Sveglia!",
                                !o.has("enabled") || o.get("enabled").getAsBoolean()));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Config: sezione alarms ignorata: " + e.getMessage());
        }
        if (migrated) c.save();
        return c;
    }

    public boolean isEnabled(String module) {
        return modules.getOrDefault(module, true);
    }

    public void setEnabled(String module, boolean on) {
        modules.put(module, on);
        save();
    }

    public HudPos getPosition(String module) {
        return positions.getOrDefault(module, new HudPos(0.005f, 0.005f));
    }

    public Map<String, HudPos> positions() {
        return Collections.unmodifiableMap(positions);
    }

    /** Sposta un modulo HUD senza salvare (utile durante il trascinamento nell'editor). */
    public void movePosition(String module, float x, float y) {
        positions.put(module, new HudPos(Math.max(0, Math.min(1, x)), Math.max(0, Math.min(1, y))));
    }

    public void setPosition(String module, float x, float y) {
        movePosition(module, x, y);
        save();
    }

    public List<Face> faces() {
        return faces;
    }

    public List<String> quickMessages() {
        return quickMessages;
    }

    /** Tasto assegnato a ciascun messaggio rapido (stessa posizione di quickMessages, "" = nessuno). */
    public List<String> quickMessageKeys() {
        return quickKeys;
    }

    /** True se il messaggio rapido i-esimo è marcato come "minaccia" (avvia il tempo di reazione). */
    public List<Boolean> quickThreats() {
        return quickThreats;
    }

    public void setQuickThreat(int index, boolean threat) {
        if (index >= 0 && index < quickThreats.size()) {
            quickThreats.set(index, threat);
            save();
        }
    }

    /** Modifica la lista marcando minaccia (usata dopo un'aggiunta/rimozione). */
    public void syncQuickThreats() {
        while (quickThreats.size() < quickMessages.size()) quickThreats.add(false);
        while (quickThreats.size() > quickMessages.size()) quickThreats.remove(quickThreats.size() - 1);
        save();
    }

    // ===================== Impostazioni moduli (sezione "settings") =====================

    public boolean getBool(String module, String key, boolean def) {
        return getPrimitive(module, key) != null && getPrimitive(module, key).getAsBoolean();
    }

    public int getInt(String module, String key, int def) {
        JsonElement e = getPrimitive(module, key);
        if (e == null) return def;
        try {
            return e.getAsInt();
        } catch (Exception ex) {
            return def;
        }
    }

    public float getFloat(String module, String key, float def) {
        JsonElement e = getPrimitive(module, key);
        if (e == null) return def;
        try {
            return e.getAsFloat();
        } catch (Exception ex) {
            return def;
        }
    }

    public String getString(String module, String key, String def) {
        JsonElement e = getPrimitive(module, key);
        if (e == null) return def;
        try {
            return e.getAsString();
        } catch (Exception ex) {
            return def;
        }
    }

    private JsonElement getPrimitive(String module, String key) {
        JsonObject o = settings.getAsJsonObject(module);
        if (o == null) return null;
        return o.has(key) ? o.get(key) : null;
    }

    public void set(String module, String key, Object value) {
        JsonObject o = settings.getAsJsonObject(module);
        if (o == null) {
            o = new JsonObject();
            settings.add(module, o);
        }
        if (value instanceof Boolean b) o.addProperty(key, b);
        else if (value instanceof Number n) o.addProperty(key, n);
        else o.addProperty(key, String.valueOf(value));
        save();
    }

    public List<Waypoint> waypoints() {
        return waypoints;
    }

    public List<Alarm> alarms() {
        return alarms;
    }

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            JsonObject mods = new JsonObject();
            for (String m : Packages.all().keySet()) mods.addProperty(m, isEnabled(m));
            obj.add("modules", mods);

            JsonObject pos = new JsonObject();
            for (Map.Entry<String, HudPos> e : positions.entrySet()) {
                if (!Packages.exists(e.getKey())) continue;
                JsonObject o = new JsonObject();
                o.addProperty("x", e.getValue().x());
                o.addProperty("y", e.getValue().y());
                pos.add(e.getKey(), o);
            }
            obj.add("positions", pos);

            JsonArray fc = new JsonArray();
            for (Face f : faces) {
                JsonObject o = new JsonObject();
                o.addProperty("name", f.name());
                if (f.description() != null && !f.description().isEmpty()) o.addProperty("description", f.description());
                if (f.rpName() != null && !f.rpName().isEmpty()) o.addProperty("rpName", f.rpName());
                if (f.atlantis()) o.addProperty("atlantis", true);
                if (f.servers() != null && !f.servers().isEmpty()) {
                    JsonArray sa = new JsonArray();
                    for (String s : f.servers()) {
                        if (s != null && !s.isEmpty()) sa.add(s);
                    }
                    if (!sa.isEmpty()) o.add("servers", sa);
                }
                fc.add(o);
            }
            obj.add("faces", fc);

            JsonArray qm = new JsonArray();
            for (String q : quickMessages) qm.add(q);
            obj.add("quickMessages", qm);

            JsonArray qk = new JsonArray();
            for (String k : quickKeys) qk.add(k);
            obj.add("quickKeys", qk);

            JsonArray qt = new JsonArray();
            for (Boolean t : quickThreats) qt.add(t != null && t);
            obj.add("quickThreats", qt);

            obj.add("settings", settings);

            JsonArray wp = new JsonArray();
            for (Waypoint w : waypoints) {
                JsonObject o = new JsonObject();
                o.addProperty("name", w.name());
                o.addProperty("world", w.world());
                o.addProperty("x", w.x());
                o.addProperty("y", w.y());
                o.addProperty("z", w.z());
                wp.add(o);
            }
            obj.add("waypoints", wp);

            JsonArray al = new JsonArray();
            for (Alarm a : alarms) {
                JsonObject o = new JsonObject();
                String t = a.time();
                if (t.length() == 4 && t.charAt(1) == ':') t = "0" + t;
                o.addProperty("time", t);
                o.addProperty("message", a.message());
                o.addProperty("enabled", a.enabled());
                al.add(o);
            }
            obj.add("alarms", al);

            Path p = path();
            Files.createDirectories(p.getParent());
            Path tmp = p.resolveSibling(p.getFileName().toString() + ".tmp");
            Files.writeString(tmp, GSON.toJson(obj), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tmp, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Errore salvataggio config: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
