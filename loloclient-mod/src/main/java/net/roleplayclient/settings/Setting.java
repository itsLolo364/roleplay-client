package net.roleplayclient.settings;

/**
 * Definizione di un'opzione configurabile di un modulo, mostrata nella
 * ModuleSettingsScreen generica. I valori vengono salvati nella sezione
 * "settings" della config di RoleplayClient.
 */
public record Setting(String key, String label, Type type, float min, float max, float step, Object def) {

    public enum Type { BOOL, INT, FLOAT, STRING }

    public boolean isNumeric() {
        return type == Type.INT || type == Type.FLOAT;
    }

    public String defaultValueString() {
        return String.valueOf(def);
    }
}
