package net.roleplayclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.roleplayclient.gui.GlassUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ricerca chat: quando è impostata una query (dalla schermata di chat),
 * mostra un overlay con i messaggi del cronologico che la contengono.
 * Il mixin su ChatHud.render disegna l'overlay dopo la chat stessa.
 */
public final class ChatSearchManager {
    private static final int MAX_RESULTS = 8;
    private static String query = "";

    private ChatSearchManager() {
    }

    public static void setQuery(String q) {
        query = q == null ? "" : q.trim();
    }

    public static String query() {
        return query;
    }

    public static boolean isActive() {
        return !query.isEmpty();
    }

    /** Converte un OrderedText (la riga renderizzata) in stringa senza codici colore. */
    public static String orderedToString(OrderedText text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        text.accept(new CharacterVisitor() {
            @Override
            public boolean accept(int index, Style style, int codepoint) {
                sb.appendCodePoint(codepoint);
                return true;
            }
        });
        return sb.toString();
    }

    /** Elenco delle righe visibili che contengono la query (dalla più recente). */
    public static List<String> matches(List<ChatHudLine.Visible> visible) {
        List<String> out = new ArrayList<>();
        if (query.isEmpty() || visible == null) return out;
        String needle = query.toLowerCase(Locale.ROOT);
        for (int i = visible.size() - 1; i >= 0 && out.size() < MAX_RESULTS; i--) {
            String s = orderedToString(visible.get(i).content());
            if (s.toLowerCase(Locale.ROOT).contains(needle)) out.add(s);
        }
        return out;
    }

    /** Overlay dei risultati, disegnato sotto il campo di ricerca (in alto a sinistra). */
    public static void renderOverlay(DrawContext ctx, List<ChatHudLine.Visible> visible) {
        if (!isActive()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        List<String> matches = matches(visible);

        int x = 8;
        int y = 36;
        int maxW = Math.min(360, client.getWindow().getScaledWidth() - 16);
        int lineH = 12;

        String header = matches.isEmpty() ? "Ricerca: nessun risultato" : "Ricerca: " + matches.size() + " risultato/i";
        int headerW = Math.max(160, Math.min(maxW, client.textRenderer.getWidth(header) + 24));
        GlassUi.chip(ctx, x, y, headerW, 20, GlassUi.GLASS_1);
        ctx.drawText(client.textRenderer, header, x + 8, y + 5, GlassUi.AMBER, false);
        y += 22;

        for (String m : matches) {
            String line = m.replace("\n", " ");
            int lw = client.textRenderer.getWidth(line);
            if (lw > maxW - 8) line = client.textRenderer.trimToWidth(line, maxW - 24) + "...";
            GlassUi.chip(ctx, x, y, Math.min(maxW, client.textRenderer.getWidth(line) + 20), lineH + 6, GlassUi.GLASS_0);
            ctx.drawText(client.textRenderer, line, x + 6, y + 2, GlassUi.TEXT, false);
            y += lineH + 7;
        }
    }
}
