package net.roleplayclient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Definizione dei pacchetti interni di Roleplay Client.
 * I pacchetti sono moduli del client, attivabili dal tab "Moduli"
 * del menu mod. Alcuni hanno opzioni configurabili (screens dedicate).
 */
public final class Packages {

    public record Pkg(String id, String name, String desc, boolean hud, boolean configurable, boolean keyBind) {
    }

    private static final Map<String, Pkg> PKGS = new LinkedHashMap<>();
    // Vista immutabile che preserva l'ordine di inserimento (Map.copyOf lo perderebbe).
    private static final Map<String, Pkg> VIEW = Collections.unmodifiableMap(PKGS);

    static {
        add(new Pkg("fps", "Contatore FPS", "Mostra gli FPS in alto a sinistra dello schermo.", true, false, false));
        add(new Pkg("cps", "Contatore CPS", "Mostra i click al secondo, sinistro e destro.", true, false, false));
        add(new Pkg("coords", "Coordinate", "Mostra la posizione X Y Z e la dimensione in cui ti trovi.", true, false, false));
        add(new Pkg("clock", "Orologio", "Mostra l'ora di sistema (orario reale).", true, false, false));
        add(new Pkg("ping", "Ping", "Mostra la latenza verso il server, aggiornata in tempo reale.", true, false, false));
        add(new Pkg("armor", "Armatura", "Mostra le armature equipaggiate con la durabilità rimanente.", true, false, false));
        add(new Pkg("volti", "Volti conosciuti", "Badge \"i\" in chat e in tab per le facce conosciute, con nota al passaggio del mouse e avviso quando si collegano.", false, true, false));
        add(new Pkg("rpmessages", "Messaggi rapidi", "Frasi e comandi RP pronti: premendo il tasto assegnato si invia il messaggio, \"+\" apre la lista.", false, true, true));
        add(new Pkg("logscene", "Diario scene", "Registra le scene RP (frasi con *) in un file di testo nella cartella del gioco.", false, false, false));
        add(new Pkg("menzioni", "Menzioni", "Avviso quando il tuo nome viene menzionato in chat.", false, false, false));
        add(new Pkg("oramondo", "Ora del mondo", "Mostra l'ora in-game del server sull'HUD.", true, false, false));
        add(new Pkg("sveglie", "Sveglie", "Sveglie a orario reale: allo scattare suona un avviso e viene mostrato un messaggio.", false, true, false));
        add(new Pkg("zoom", "Zoom", "Tieni premuto il tasto (Alt) per ingrandire la visuale.", false, false, true));
        add(new Pkg("cinema", "Cinema", "Premi il tasto (ò) per nascondere l'HUD con una visuale cinematografica.", false, false, true));
        add(new Pkg("waypoint", "Waypoint", "Salva posizioni nel mondo: distanza e direzione mostrate sull'HUD.", true, true, false));
        add(new Pkg("rptimers", "RP Timers", "Timer per scene RP e tempo di reazione dopo le azioni marcate come minaccia.", true, true, false));
        add(new Pkg("rpstopwatch", "RP Cronometro", "Cronometro per scene RP, con tasto dedicato.", true, false, true));
        add(new Pkg("cleanscreenshot", "Clean Screenshot", "Sostituisce F2: screenshot pulito del mondo, senza HUD e chat.", false, false, false));
        add(new Pkg("crosshair", "Crosshair RP", "Personalizza il mirino: colore e dimensione.", false, true, false));
        add(new Pkg("desyncalert", "Allerta desync", "Avviso quando il server non risponde oltre la soglia impostata.", true, true, false));
        add(new Pkg("chatsearch", "Ricerca chat", "Cerca nel cronologico della chat dal campo di ricerca dedicato.", false, false, false));
        add(new Pkg("sessiontime", "Tempo di sessione", "Mostra da quanto tempo sei connesso al server.", true, false, false));
        add(new Pkg("clipready", "Clip Ready", "Indicatore stato Medals (clip pronta) e avviso dei momenti da registrare.", true, true, false));
        add(new Pkg("watermark", "Action Watermark", "Watermark Roleplay Client sempre visibile sullo schermo.", true, false, false));
    }

    private static void add(Pkg p) {
        PKGS.put(p.id(), p);
    }

    public static Map<String, Pkg> all() {
        return VIEW;
    }

    public static Pkg get(String id) {
        return PKGS.get(id);
    }

    public static boolean exists(String id) {
        return PKGS.containsKey(id);
    }
}
