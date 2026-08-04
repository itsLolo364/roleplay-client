package net.roleplayclient.settings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry delle impostazioni esposte dalla ModuleSettingsScreen per ciascun
 * modulo configurabile. I default qui devono combaciare con quelli letti
 * direttamente dai manager (che usano gli stessi valori come fallback).
 */
public final class SettingDefs {

    private static final Map<String, List<Setting>> DEFS = new LinkedHashMap<>();

    static {
        DEFS.put("rptimers", List.of(
                new Setting("reactionSeconds", "Tempo di reazione (s)", Setting.Type.FLOAT, 0.5f, 120, 0.5f, 1.5f)));
        DEFS.put("crosshair", List.of(
                new Setting("size", "Dimensione mirino", Setting.Type.INT, 2, 24, 1, 6),
                new Setting("hide", "Nascondi mirino", Setting.Type.BOOL, 0, 0, 0, false),
                new Setting("color", "Colore (#RRGGBB)", Setting.Type.STRING, 0, 0, 0, "#FFFFFF")));
        DEFS.put("desyncalert", List.of(
                new Setting("thresholdSeconds", "Soglia senza risposta (s)", Setting.Type.INT, 1, 60, 1, 5)));
        DEFS.put("clipready", List.of(
                new Setting("healthUrl", "URL health Medals", Setting.Type.STRING, 0, 0, 0,
                        "http://localhost:12665/api/v1/health"),
                new Setting("pollSeconds", "Poll health (s)", Setting.Type.INT, 1, 60, 1, 5)));
    }

    private SettingDefs() {
    }

    public static List<Setting> forModule(String id) {
        return DEFS.getOrDefault(id, List.of());
    }

    public static boolean hasSettings(String id) {
        return !forModule(id).isEmpty();
    }

    public static Map<String, List<Setting>> all() {
        return Collections.unmodifiableMap(DEFS);
    }
}
