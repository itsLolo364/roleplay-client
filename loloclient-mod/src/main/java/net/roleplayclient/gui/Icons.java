package net.roleplayclient.gui;

import net.minecraft.client.texture.NativeImage;

import java.util.HashMap;
import java.util.Map;

/**
 * Generatore procedurale di icone "Material You": ogni pacchetto ha un logo
 * personale (rounded-square con gradiente dell'accento + simbolo bianco),
 * in stile espressivo come le icone del launcher.
 */
public final class Icons {
    private static final int S = 64;
    private static final int LOGO = 96;

    private static final Map<String, int[]> ACCENTS = new HashMap<>();

    static {
        ACCENTS.put("fps", new int[]{0x2D, 0xD4, 0xBF});
        ACCENTS.put("cps", new int[]{0xFC, 0xAD, 0x14});
        ACCENTS.put("coords", new int[]{0x24, 0x7C, 0xE2});
        ACCENTS.put("clock", new int[]{0x8B, 0x7C, 0xF6});
        ACCENTS.put("ping", new int[]{0x60, 0xA5, 0xFA});
        ACCENTS.put("armor", new int[]{0x2D, 0xD4, 0xBF});
        ACCENTS.put("volti", new int[]{0xFC, 0xAD, 0x14});
        ACCENTS.put("rpmessages", new int[]{0x24, 0x7C, 0xE2});
        ACCENTS.put("logscene", new int[]{0x8B, 0x7C, 0xF6});
        ACCENTS.put("menzioni", new int[]{0xF4, 0x72, 0xB6});
        ACCENTS.put("oramondo", new int[]{0x24, 0x7C, 0xE2});
        ACCENTS.put("sveglie", new int[]{0xFC, 0xAD, 0x14});
        ACCENTS.put("zoom", new int[]{0x2D, 0xD4, 0xBF});
        ACCENTS.put("cinema", new int[]{0x8B, 0x7C, 0xF6});
        ACCENTS.put("waypoint", new int[]{0xFC, 0xAD, 0x14});
        ACCENTS.put("rptimers", new int[]{0xFC, 0xAD, 0x14});
        ACCENTS.put("rpstopwatch", new int[]{0x2D, 0xD4, 0xBF});
        ACCENTS.put("cleanscreenshot", new int[]{0x24, 0x7C, 0xE2});
        ACCENTS.put("crosshair", new int[]{0x8B, 0x7C, 0xF6});
        ACCENTS.put("desyncalert", new int[]{0x60, 0xA5, 0xFA});
        ACCENTS.put("chatsearch", new int[]{0x60, 0xA5, 0xFA});
        ACCENTS.put("sessiontime", new int[]{0x2D, 0xD4, 0xBF});
        ACCENTS.put("clipready", new int[]{0xFC, 0xAD, 0x14});
        ACCENTS.put("watermark", new int[]{0x3D, 0x8C, 0xFF});
    }

    private Icons() {
    }

    private static int[] accent(String id) {
        int[] a = ACCENTS.get(id);
        if (a != null) return a;
        return new int[]{0x24, 0x7C, 0xE2};
    }

    /** Icona di un pacchetto: rounded-square con gradiente e simbolo bianco. */
    public static NativeImage buildPackage(String id) {
        NativeImage img = new NativeImage(S, S, true);
        int[] acc = accent(id);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double px = x + 0.5;
                double py = y + 0.5;
                if (sdRoundRect(px, py, S / 2d, S / 2d, S / 2d - 1, S / 2d - 1, 16) > 0) {
                    img.setColorArgb(x, y, 0);
                    continue;
                }
                double cov = motif(px, py, id);
                int[] bg = gradient(px, py, acc);
                int ar = bg[0], ag = bg[1], ab = bg[2];
                if (cov > 0) {
                    ar = (int) Math.round(255 + (ar - 255) * cov);
                    ag = (int) Math.round(255 + (ag - 255) * cov);
                    ab = (int) Math.round(255 + (ab - 255) * cov);
                }
                img.setColorArgb(x, y, 0xFF000000 | (ar << 16) | (ag << 8) | ab);
            }
        }
        return img;
    }

    /** Logo dell'app: squircle con gradiente ambra->blu e simbolo "chat". */
    public static NativeImage buildLogo() {
        NativeImage img = new NativeImage(LOGO, LOGO, true);
        int[] acc = new int[]{0xFC, 0xAD, 0x14};
        for (int y = 0; y < LOGO; y++) {
            for (int x = 0; x < LOGO; x++) {
                double px = x + 0.5;
                double py = y + 0.5;
                if (sdRoundRect(px, py, LOGO / 2d, LOGO / 2d, LOGO / 2d - 1, LOGO / 2d - 1, 24) > 0) {
                    img.setColorArgb(x, y, 0);
                    continue;
                }
                // gradiente diagonale ambra -> blu
                double u = (px + py) / (2d * LOGO);
                int r = (int) Math.round(lerp(0xFC, 0x24, u));
                int g = (int) Math.round(lerp(0xAD, 0x7C, u));
                int b = (int) Math.round(lerp(0x14, 0xE2, u));
                double cov = logoMark(px, py);
                if (cov > 0) {
                    r = (int) Math.round(255 + (r - 255) * cov);
                    g = (int) Math.round(255 + (g - 255) * cov);
                    b = (int) Math.round(255 + (b - 255) * cov);
                }
                img.setColorArgb(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    /**
     * Logo minimale per icona finestra OS: stesso squircle ambra→blu,
     * solo le iniziali "RC" stilizzate, senza mark chat dettagliato.
     */
    public static NativeImage buildMinimalLogo(int size) {
        NativeImage img = new NativeImage(size, size, true);
        double half = size / 2d;
        double rad = size * 0.28;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double px = x + 0.5;
                double py = y + 0.5;
                if (sdRoundRect(px, py, half, half, half - 1, half - 1, rad) > 0) {
                    img.setColorArgb(x, y, 0);
                    continue;
                }
                double u = (px + py) / (2d * size);
                int r = (int) Math.round(lerp(0xFC, 0x24, u));
                int g = (int) Math.round(lerp(0xAD, 0x7C, u));
                int b = (int) Math.round(lerp(0x14, 0xE2, u));
                // mark minimale: due barre verticali stilizzate "RC"
                double cov = 0;
                double s = size / 96d;
                cov = Math.max(cov, fill(sdRoundRect(px, py, half - 14 * s, half, 5 * s, 22 * s, 2.5 * s)));
                cov = Math.max(cov, fill(sdRoundRect(px, py, half + 10 * s, half, 5 * s, 22 * s, 2.5 * s)));
                cov = Math.max(cov, fill(sdRoundRect(px, py, half - 2 * s, half - 14 * s, 18 * s, 4 * s, 2 * s)));
                if (cov > 0) {
                    r = (int) Math.round(255 + (r - 255) * cov);
                    g = (int) Math.round(255 + (g - 255) * cov);
                    b = (int) Math.round(255 + (b - 255) * cov);
                }
                img.setColorArgb(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    // ===================== MOTIVI =====================

    private static double motif(double px, double py, String id) {
        return switch (id) {
            case "fps" -> threeBars(px, py);
            case "cps" -> doubleClick(px, py);
            case "coords" -> crosshair(px, py);
            case "clock" -> clock(px, py, false);
            case "ping" -> pulse(px, py);
            case "armor" -> shield(px, py);
            case "volti" -> people(px, py);
            case "rpmessages" -> bubble(px, py);
            case "logscene" -> book(px, py);
            case "menzioni" -> alert(px, py);
            case "oramondo" -> globe(px, py);
            case "sveglie" -> alarm(px, py);
            case "zoom" -> magnifier(px, py);
            case "cinema" -> play(px, py);
            case "waypoint" -> pin(px, py);
            case "rptimers" -> timer(px, py);
            case "rpstopwatch" -> stopwatch(px, py);
            case "cleanscreenshot" -> camera(px, py);
            case "crosshair" -> crosshair(px, py);
            case "desyncalert" -> waveOff(px, py);
            case "chatsearch" -> magnifier(px, py);
            case "sessiontime" -> clock(px, py, false);
            case "clipready" -> clapper(px, py);
            case "watermark" -> award(px, py);
            default -> 0;
        };
    }

    /** Bubble chat: viene riutilizzata per RPMessages e (con contenuto) per Menzioni. */
    private static double threeBars(double px, double py) {
        double[] hs = {18, 30, 12};
        double cov = 0;
        for (int i = 0; i < 3; i++) {
            double cx = 20 + i * 12;
            double h = hs[i];
            cov = Math.max(cov, fill(sdRoundRect(px, py, cx, 48 - h / 2, 4, h / 2, 3)));
        }
        return cov;
    }

    private static double doubleClick(double px, double py) {
        double cov = 0;
        for (double cx : new double[]{23, 41}) {
            cov = Math.max(cov, fill(sdCircle(px, py, cx, 32, 8)));
            cov = Math.max(0, cov - fill(sdCircle(px, py, cx, 32, 2.6)));
        }
        return cov;
    }

    private static double crosshair(double px, double py) {
        double cov = stroke(sdRing(px, py, 32, 32, 12.5), 3);
        cov = Math.max(cov, stroke(sdSeg(px, py, 10, 32, 54, 32), 3));
        cov = Math.max(cov, stroke(sdSeg(px, py, 32, 10, 32, 54), 3));
        cov = Math.max(cov, fill(sdCircle(px, py, 32, 32, 3.4)));
        return cov;
    }

    private static double clock(double px, double py, boolean alarm) {
        double cov = stroke(sdRing(px, py, 32, 31, 12.5), 3);
        cov = Math.max(cov, stroke(sdSeg(px, py, 32, 31, 32, 23), 3));
        cov = Math.max(cov, stroke(sdSeg(px, py, 32, 31, 41, 31), 3));
        if (alarm) {
            cov = Math.max(cov, stroke(sdSeg(px, py, 29, 16, 35, 16), 3));
            cov = Math.max(cov, stroke(sdSeg(px, py, 24, 46, 28, 41), 3));
            cov = Math.max(cov, stroke(sdSeg(px, py, 40, 46, 36, 41), 3));
        }
        return cov;
    }

    private static double pulse(double px, double py) {
        double cov = stroke(sdSeg(px, py, 10, 32, 22, 32), 3.5);
        cov = Math.max(cov, stroke(sdSeg(px, py, 22, 32, 28, 20), 3.5));
        cov = Math.max(cov, stroke(sdSeg(px, py, 28, 20, 36, 44), 3.5));
        cov = Math.max(cov, stroke(sdSeg(px, py, 36, 44, 42, 32), 3.5));
        cov = Math.max(cov, stroke(sdSeg(px, py, 42, 32, 54, 32), 3.5));
        cov = Math.max(cov, fill(sdCircle(px, py, 54, 32, 3)));
        return cov;
    }

    private static double shield(double px, double py) {
        double cov = fill(sdRoundRect(px, py, 32, 27, 15, 11, 6));
        cov = Math.max(cov, inTri(px, py, 17, 28, 47, 28, 32, 52) ? 1 : 0);
        return cov;
    }

    private static double people(double px, double py) {
        double cov = fill(sdCircle(px, py, 24, 23, 6));
        cov = Math.max(cov, fill(sdRoundRect(px, py, 24, 33, 9, 6, 4)));
        cov = Math.max(cov, fill(sdCircle(px, py, 40, 23, 6)));
        cov = Math.max(cov, fill(sdRoundRect(px, py, 40, 33, 9, 6, 4)));
        return cov;
    }

    private static double chatBubble(double px, double py) {
        double cov = fill(sdRoundRect(px, py, 32, 30, 15, 11, 7));
        cov = Math.max(cov, inTri(px, py, 26, 36, 33, 36, 26, 44) ? 1 : 0);
        return cov;
    }

    private static double bubble(double px, double py) {
        double cov = chatBubble(px, py);
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 24, 30, 2.3)));
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 32, 30, 2.3)));
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 40, 30, 2.3)));
        return cov;
    }

    private static double book(double px, double py) {
        double cov = fill(sdRoundRect(px, py, 22, 32, 9, 13, 3));
        cov = Math.max(cov, fill(sdRoundRect(px, py, 42, 32, 9, 13, 3)));
        cov = Math.min(cov, 1 - stroke(sdSeg(px, py, 32, 20, 32, 44), 2.2));
        cov = Math.min(cov, 1 - stroke(sdSeg(px, py, 19, 28, 25, 28), 2.2));
        cov = Math.min(cov, 1 - stroke(sdSeg(px, py, 19, 34, 25, 34), 2.2));
        cov = Math.min(cov, 1 - stroke(sdSeg(px, py, 39, 28, 45, 28), 2.2));
        cov = Math.min(cov, 1 - stroke(sdSeg(px, py, 39, 34, 45, 34), 2.2));
        return cov;
    }

    private static double alert(double px, double py) {
        double cov = chatBubble(px, py);
        cov = Math.min(cov, 1 - stroke(sdSeg(px, py, 32, 24, 32, 31), 3));
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 32, 36, 2.2)));
        return cov;
    }

    private static double globe(double px, double py) {
        double cov = fill(sdCircle(px, py, 32, 32, 14));
        cov = Math.min(cov, 1 - (inEllipse(px, py, 32, 32, 6, 13) ? 1 : 0));
        cov = Math.min(cov, 1 - stroke(sdSeg(px, py, 18, 32, 46, 32), 3));
        return cov;
    }

    private static double alarm(double px, double py) {
        double cov = stroke(sdRing(px, py, 32, 31, 12.5), 3);
        cov = Math.max(cov, stroke(sdSeg(px, py, 32, 31, 32, 23), 3));
        cov = Math.max(cov, stroke(sdSeg(px, py, 32, 31, 41, 31), 3));
        cov = Math.max(cov, stroke(sdSeg(px, py, 29, 16, 35, 16), 3));
        cov = Math.max(cov, stroke(sdSeg(px, py, 24, 46, 28, 41), 3));
        cov = Math.max(cov, stroke(sdSeg(px, py, 40, 46, 36, 41), 3));
        return cov;
    }

    private static double magnifier(double px, double py) {
        double cov = stroke(sdRing(px, py, 27, 27, 10.5), 4);
        cov = Math.max(cov, stroke(sdSeg(px, py, 35, 35, 48, 48), 5));
        return cov;
    }

    private static double play(double px, double py) {
        return inTri(px, py, 27, 20, 50, 32, 27, 44) ? 1 : 0;
    }

    private static double pin(double px, double py) {
        double cov = fill(sdCircle(px, py, 32, 26, 9));
        cov = Math.max(cov, inTri(px, py, 23, 31, 41, 31, 32, 48) ? 1 : 0);
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 32, 26, 3)));
        return cov;
    }

    /** Simbolo del logo: bubble + sparkle Material You. */
    private static double logoMark(double px, double py) {
        double cov = fill(sdRoundRect(px, py, 46, 56, 28, 18, 11));
        cov = Math.max(cov, inTri(px, py, 28, 70, 37, 70, 28, 80) ? 1 : 0);
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 34, 56, 2.6)));
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 46, 56, 2.6)));
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 58, 56, 2.6)));
        double sp = sparkle(px, py, 74, 26, 13);
        cov = Math.max(cov, sp);
        cov = Math.max(cov, sparkle(px, py, 60, 14, 6));
        return cov;
    }

    private static double sparkle(double px, double py, double cx, double cy, double r) {
        double dx = px - cx;
        double dy = py - cy;
        double d = Math.pow(Math.abs(dx), 0.6) + Math.pow(Math.abs(dy), 0.6) - r;
        return fill(d);
    }

    private static double camera(double px, double py) {
        double cov = fill(sdRoundRect(px, py, 32, 36, 16, 12, 5));
        cov = Math.max(cov, fill(sdRoundRect(px, py, 32, 23, 6, 3, 2)));
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 32, 36, 7)));
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 32, 36, 2.2)));
        return cov;
    }

    private static double timer(double px, double py) {
        double cov = stroke(sdRoundRect(px, py, 32, 32, 13, 15, 3), 3);
        cov = Math.max(cov, inTri(px, py, 23, 24, 41, 24, 32, 36) ? 1 : 0);
        cov = Math.max(cov, inTri(px, py, 23, 40, 41, 40, 32, 28) ? 1 : 0);
        return cov;
    }

    private static double stopwatch(double px, double py) {
        double cov = stroke(sdRing(px, py, 32, 36, 11), 3);
        cov = Math.max(cov, fill(sdRoundRect(px, py, 26, 21, 3, 3, 1)));
        cov = Math.max(cov, fill(sdRoundRect(px, py, 38, 21, 3, 3, 1)));
        cov = Math.max(cov, stroke(sdSeg(px, py, 32, 36, 32, 29), 3));
        cov = Math.max(cov, stroke(sdSeg(px, py, 32, 36, 39, 40), 3));
        return cov;
    }

    private static double waveOff(double px, double py) {
        double cov = stroke(sdSeg(px, py, 12, 40, 20, 32), 3.5);
        cov = Math.max(cov, stroke(sdSeg(px, py, 20, 32, 24, 28), 3.5));
        cov = Math.max(cov, stroke(sdSeg(px, py, 40, 24, 44, 20), 3.5));
        cov = Math.max(cov, stroke(sdSeg(px, py, 44, 20, 52, 12), 3.5));
        cov = Math.max(cov, stroke(sdSeg(px, py, 14, 14, 50, 50), 3.5));
        return cov;
    }

    private static double clapper(double px, double py) {
        double cov = fill(sdRoundRect(px, py, 32, 36, 14, 11, 3));
        cov = Math.max(cov, inTri(px, py, 20, 30, 44, 30, 20, 38) ? 1 : 0);
        cov = Math.max(cov, inTri(px, py, 20, 38, 44, 30, 44, 38) ? 1 : 0);
        cov = Math.min(cov, 1 - stroke(sdSeg(px, py, 24, 33, 40, 31), 2));
        cov = Math.min(cov, 1 - stroke(sdSeg(px, py, 22, 36, 38, 34), 2));
        return cov;
    }

    private static double award(double px, double py) {
        double cov = fill(sdCircle(px, py, 32, 28, 7));
        cov = Math.max(cov, stroke(sdSeg(px, py, 28, 33, 24, 42), 3));
        cov = Math.max(cov, stroke(sdSeg(px, py, 36, 33, 40, 42), 3));
        cov = Math.max(cov, fill(sdCircle(px, py, 24, 43, 2.6)));
        cov = Math.max(cov, fill(sdCircle(px, py, 40, 43, 2.6)));
        cov = Math.min(cov, 1 - fill(sdCircle(px, py, 32, 28, 2.4)));
        return cov;
    }

    // ===================== HELPERS =====================

    private static int[] gradient(double px, double py, int[] acc) {
        double u = (px + py) / (2d * S);
        int r = (int) Math.round(lerp(acc[0] * 0.75, 255, Math.min(1, u * 1.35)));
        int g = (int) Math.round(lerp(acc[1] * 0.75, 255, Math.min(1, u * 1.35)));
        int b = (int) Math.round(lerp(acc[2] * 0.75, 255, Math.min(1, u * 1.35)));
        r = (int) Math.round(lerp(r, 0, u * 0.18));
        g = (int) Math.round(lerp(g, 0, u * 0.18));
        b = (int) Math.round(lerp(b, 0, u * 0.18));
        return new int[]{r, g, b};
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double fill(double sd) {
        return Math.max(0, Math.min(1, 0.5 - sd));
    }

    private static double stroke(double sd, double t) {
        return Math.max(0, Math.min(1, t / 2 - Math.abs(sd)));
    }

    // ---- SDF ----

    private static double sdCircle(double px, double py, double cx, double cy, double r) {
        return Math.hypot(px - cx, py - cy) - r;
    }

    private static double sdRing(double px, double py, double cx, double cy, double midR) {
        return Math.abs(Math.hypot(px - cx, py - cy) - midR);
    }

    private static double sdRoundRect(double px, double py, double cx, double cy, double hw, double hh, double r) {
        double dx = Math.abs(px - cx) - hw + r;
        double dy = Math.abs(py - cy) - hh + r;
        double ax = Math.max(dx, 0);
        double ay = Math.max(dy, 0);
        return Math.hypot(ax, ay) + Math.min(Math.max(dx, dy), 0) - r;
    }

    private static double sdSeg(double px, double py, double ax, double ay, double bx, double by) {
        double abx = bx - ax;
        double aby = by - ay;
        double len2 = abx * abx + aby * aby;
        if (len2 == 0) return Math.hypot(px - ax, py - ay);
        double apx = px - ax;
        double apy = py - ay;
        double t = Math.max(0, Math.min(1, (apx * abx + apy * aby) / len2));
        return Math.hypot(apx - abx * t, apy - aby * t);
    }

    private static boolean inTri(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
        double d1 = sign(px, py, ax, ay, bx, by);
        double d2 = sign(px, py, bx, by, cx, cy);
        double d3 = sign(px, py, cx, cy, ax, ay);
        boolean neg = d1 < 0 || d2 < 0 || d3 < 0;
        boolean pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    private static double sign(double px, double py, double ax, double ay, double bx, double by) {
        return (px - bx) * (ay - by) - (ax - bx) * (py - by);
    }

    private static boolean inEllipse(double px, double py, double cx, double cy, double hr, double vr) {
        double dx = (px - cx) / hr;
        double dy = (py - cy) / vr;
        return dx * dx + dy * dy <= 1;
    }
}
