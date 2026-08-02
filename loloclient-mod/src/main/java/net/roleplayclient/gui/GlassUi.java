package net.roleplayclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.roleplayclient.RoleplayClientMod;

import java.util.HashMap;
import java.util.Map;

/**
 * UI Liquid Glass allineata al launcher (public/index.html, tema dark):
 * aurora navy, superfici vetro, accent ambra, toggle Material, bottoni pill.
 */
public final class GlassUi {
    // ---- Palette = token launcher dark ----
    public static final int AMBER = 0xFFFCAD14;
    public static final int AMBER_2 = 0xFFF09300;
    public static final int ON_ACCENT = 0xFF3A2400;
    public static final int AMBER_SOFT = 0x29FCAD14;   // ~0.16
    public static final int AMBER_LINE = 0x6BFCAD14;   // ~0.42
    public static final int AMBER_GLOW = 0x59FCAD14;   // ~0.35

    public static final int BLUE = 0xFF3D8CFF;
    public static final int BLUE_2 = 0xFF247CE2;
    public static final int BLUE_SOFT = 0x293D8CFF;
    public static final int TEAL = 0xFF2DD4BF;
    public static final int VIOLET = 0xFF8B5CF6;
    public static final int PINK = 0xFFF472B6;

    public static final int BG = 0xFF050A16;
    public static final int NAVY = 0xFF0B1D39;

    public static final int GLASS_0 = 0x8C0E182E;      // rgba(14,24,46,0.55)
    public static final int GLASS_1 = 0xB80E182E;      // 0.72
    public static final int GLASS_2 = 0xE0091124;      // 0.88

    public static final int PANEL_TOP = 0xB80E182E;
    public static final int PANEL_BOTTOM = 0xE0091124;
    public static final int PANEL_BORDER = 0x42FFFFFF; // glass-line-strong ~0.26
    public static final int PANEL_SPECULAR = 0x73FFFFFF; // spec ~0.45

    public static final int CARD = 0xB80E182E;
    public static final int CARD_HOVER = 0xCC152038;
    public static final int CARD_SEL = 0xE01A2848;

    public static final int TEXT = 0xFFF6F3EE;         // --on
    public static final int MUTED = 0xA3ECF2FC;        // --on-variant ~0.64
    public static final int DIM = 0x66ECF2FC;

    public static final int BORDER = 0x21FFFFFF;       // glass-line ~0.13
    public static final int BORDER_STRONG = 0x42FFFFFF;
    public static final int SPECULAR = 0x73FFFFFF;

    public static final int TOGGLE_W = 52;
    public static final int TOGGLE_H = 30;
    public static final int TOGGLE_KNOB = 24;

    // ---- Texture ----
    private static final int TILE = 48;
    private static final int SLICE = 21;
    private static final int RADIUS = 18;
    private static final int CHIP_TILE = 32;
    private static final int CHIP_R = 14;
    private static final int CHIP_SLICE = 14;

    private static Identifier amberTex;
    private static Identifier blueTex;
    private static Identifier tealTex;
    private static Identifier violetTex;
    private static Identifier glassTex;
    private static Identifier logoTex;
    private static Identifier logoMinimalTex;
    private static final Map<String, Identifier> pkgTex = new HashMap<>();
    private static final Map<Integer, Identifier> chipTex = new HashMap<>();
    private static final Map<String, Identifier> discTex = new HashMap<>();
    private static boolean initialized = false;

    private static final int[] CHIP_COLORS = {
            GLASS_0, GLASS_1, GLASS_2, CARD, CARD_HOVER, CARD_SEL,
            PANEL_BOTTOM, AMBER, AMBER_2, AMBER_SOFT, AMBER_LINE,
            0x800B1D39, 0x660B1D39, 0x550B1D39, 0xEE0B1D39,
            0xCC0B1D39, 0x55247CE2, 0xFFE6A411, BLUE_SOFT, BLUE_2
    };
    private static final int[] DISC_COLORS = {
            0xFFFFFFFF, 0xFFE8EEF7, AMBER, AMBER_2, ON_ACCENT
    };
    private static final int[] DISC_SIZES = {8, 10, 14, 16, 24, 32};

    private GlassUi() {
    }

    public static void init() {
        if (initialized) return;
        MinecraftClient client = MinecraftClient.getInstance();
        var mgr = client.getTextureManager();

        amberTex = Identifier.of(RoleplayClientMod.MOD_ID, "aurora_amber");
        blueTex = Identifier.of(RoleplayClientMod.MOD_ID, "aurora_blue");
        tealTex = Identifier.of(RoleplayClientMod.MOD_ID, "aurora_teal");
        violetTex = Identifier.of(RoleplayClientMod.MOD_ID, "aurora_violet");
        glassTex = Identifier.of(RoleplayClientMod.MOD_ID, "glass_panel");
        logoTex = Identifier.of(RoleplayClientMod.MOD_ID, "app_logo");
        logoMinimalTex = Identifier.of(RoleplayClientMod.MOD_ID, "app_logo_minimal");

        register(mgr, amberTex, "rc-aurora-amber", makeBlob(384, 0xFC, 0xAD, 0x14), true);
        register(mgr, blueTex, "rc-aurora-blue", makeBlob(384, 0x24, 0x7C, 0xE2), true);
        register(mgr, tealTex, "rc-aurora-teal", makeBlob(384, 0x2D, 0xD4, 0xBF), true);
        register(mgr, violetTex, "rc-aurora-violet", makeBlob(384, 0x8B, 0x5C, 0xF6), true);
        register(mgr, glassTex, "rc-glass-panel", makeGlassTile(), false);
        register(mgr, logoTex, "rc-app-logo", Icons.buildLogo(), false);
        register(mgr, logoMinimalTex, "rc-app-logo-min", Icons.buildMinimalLogo(128), false);

        for (int color : CHIP_COLORS) ensureChip(color);
        for (int color : DISC_COLORS) {
            for (int size : DISC_SIZES) ensureDisc(color, size);
        }

        initialized = true;
    }

    private static void register(TextureManager mgr, Identifier id, String name, NativeImage img, boolean bilinear) {
        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> name, img);
        // Mai mipmap su UI: causano cubi bianchi semi-trasparenti agli angoli 9-slice
        tex.setFilter(bilinear, false);
        mgr.registerTexture(id, tex);
    }

    private static Identifier ensureChip(int color) {
        Identifier id = chipTex.get(color);
        if (id != null) return id;
        String key = "chip_" + Integer.toUnsignedString(color, 16);
        id = Identifier.of(RoleplayClientMod.MOD_ID, key);
        register(MinecraftClient.getInstance().getTextureManager(), id, "rc-" + key, makeChip(color), false);
        chipTex.put(color, id);
        return id;
    }

    private static Identifier ensureDisc(int color, int size) {
        int keySize = Math.max(1, Math.min(size, 64));
        String mapKey = color + "_" + keySize;
        Identifier id = discTex.get(mapKey);
        if (id != null) return id;
        String key = "disc_" + keySize + "_" + Integer.toUnsignedString(color, 16);
        id = Identifier.of(RoleplayClientMod.MOD_ID, key);
        register(MinecraftClient.getInstance().getTextureManager(), id, "rc-" + key, makeDisc(keySize, color), false);
        discTex.put(mapKey, id);
        return id;
    }

    private static NativeImage makeBlob(int size, int r, int g, int b) {
        NativeImage img = new NativeImage(size, size, true);
        float c = (size - 1) / 2f;
        float R = c + 1f;
        float aMax = 0.55f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c;
                float dy = y - c;
                float d = (float) Math.sqrt(dx * dx + dy * dy) / R;
                float t = 1f - Math.min(1f, d);
                float a = aMax * t * t * t;
                int ai = Math.round(a * 255f);
                if (ai > 4) {
                    img.setColorArgb(x, y, (ai << 24) | (r << 16) | (g << 8) | b);
                } else {
                    img.setColorArgb(x, y, 0);
                }
            }
        }
        return img;
    }

    private static NativeImage makeGlassTile() {
        NativeImage img = new NativeImage(TILE, TILE, true);
        int c = RADIUS;
        int[] top = rgbaOf(PANEL_TOP);
        int[] bottom = rgbaOf(PANEL_BOTTOM);
        int[] border = rgbaOf(PANEL_BORDER);
        int[] spec = rgbaOf(PANEL_SPECULAR);
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                float t = y / (float) (TILE - 1);
                int a = Math.round(top[0] + (bottom[0] - top[0]) * t);
                int r = Math.round(top[1] + (bottom[1] - top[1]) * t);
                int g = Math.round(top[2] + (bottom[2] - top[2]) * t);
                int b = Math.round(top[3] + (bottom[3] - top[3]) * t);

                if (!insideRoundRect(x, y, c)) {
                    img.setColorArgb(x, y, 0);
                    continue;
                }
                float edgeDist = edgeDistance(x, y, c, TILE);
                if (edgeDist < 1.2f) {
                    float ef = 1f - (edgeDist / 1.2f);
                    // bordo soft, senza spingere verso bianco puro
                    a = Math.min(255, Math.round(a + border[0] * ef * 0.12f));
                    r = Math.round(r + (border[1] - r) * ef * 0.08f);
                    g = Math.round(g + (border[2] - g) * ef * 0.08f);
                    b = Math.round(b + (border[3] - b) * ef * 0.08f);
                }
                if (y < 2) {
                    float sf = 1f - (y / 2f);
                    r = Math.round(r + (spec[1] - r) * sf * 0.08f);
                    g = Math.round(g + (spec[2] - g) * sf * 0.08f);
                    b = Math.round(b + (spec[3] - b) * sf * 0.08f);
                }
                img.setColorArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    private static float edgeDistance(int x, int y, int r, int size) {
        int w = size - 1;
        int h = size - 1;
        int cx = Math.max(r, Math.min(w - r, x));
        int cy = Math.max(r, Math.min(h - r, y));
        float dx = x - cx;
        float dy = y - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float cornerDist = Math.max(0f, r - dist);
        float straightDist = Math.min(Math.min(x, w - x), Math.min(y, h - y));
        straightDist = Math.min(straightDist, r);
        return Math.min(straightDist, cornerDist);
    }

    private static int[] rgbaOf(int argb) {
        return new int[]{(argb >> 24) & 0xFF, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
    }

    private static boolean insideRoundRect(int x, int y, int r, int size) {
        int w = size - 1;
        int h = size - 1;
        int cx = Math.max(r, Math.min(w - r, x));
        int cy = Math.max(r, Math.min(h - r, y));
        int dx = x - cx;
        int dy = y - cy;
        return dx * dx + dy * dy <= r * r + 1;
    }

    private static boolean insideRoundRect(int x, int y, int r) {
        return insideRoundRect(x, y, r, TILE);
    }

    private static NativeImage makeChip(int color) {
        NativeImage img = new NativeImage(CHIP_TILE, CHIP_TILE, true);
        int[] c = rgbaOf(color);
        for (int y = 0; y < CHIP_TILE; y++) {
            for (int x = 0; x < CHIP_TILE; x++) {
                if (!insideRoundRect(x, y, CHIP_R, CHIP_TILE)) {
                    img.setColorArgb(x, y, 0);
                    continue;
                }
                int a = c[0], r = c[1], g = c[2], b = c[3];
                float edgeDist = edgeDistance(x, y, CHIP_R, CHIP_TILE);
                if (edgeDist < 1.2f) {
                    float ef = 1f - (edgeDist / 1.2f);
                    a = Math.min(255, Math.round(a + ef * 10f));
                    // leggero bordo specular, non bianco aggressivo
                    r = Math.round(r + (255 - r) * ef * 0.03f);
                    g = Math.round(g + (255 - g) * ef * 0.03f);
                    b = Math.round(b + (255 - b) * ef * 0.03f);
                }
                img.setColorArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    private static NativeImage makeDisc(int size, int color) {
        NativeImage img = new NativeImage(size, size, true);
        float c = (size - 1) / 2f;
        float r = c + 0.35f;
        int[] col = rgbaOf(color);
        int rgb = (col[1] << 16) | (col[2] << 8) | col[3];
        int baseA = col[0] == 0 ? 255 : col[0];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c;
                float dy = y - c;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float cov = Math.max(0f, Math.min(1f, r - d + 0.5f));
                int a = Math.round(cov * baseA);
                if (a > 12) {
                    img.setColorArgb(x, y, (a << 24) | rgb);
                } else {
                    img.setColorArgb(x, y, 0);
                }
            }
        }
        return img;
    }

    // ===================== RENDER =====================

    public static Identifier logo() {
        init();
        return logoTex;
    }

    public static Identifier logoMinimal() {
        init();
        return logoMinimalTex;
    }

    public static Identifier pkgIcon(String id) {
        init();
        Identifier tex = pkgTex.get(id);
        if (tex == null) {
            tex = Identifier.of(RoleplayClientMod.MOD_ID, "pkg_" + id);
            register(MinecraftClient.getInstance().getTextureManager(), tex, "rc-pkg-" + id, Icons.buildPackage(id), false);
            pkgTex.put(id, tex);
        }
        return tex;
    }

    private static long auroraTickMs = 0;
    private static final float[] auroraPos = new float[10]; // cx,cy × 5

    public static void background(DrawContext ctx) {
        init();
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        ctx.fill(0, 0, w, h, BG);
        drawAurora(ctx, w, h);
    }

    private static void drawAurora(DrawContext ctx, int w, int h) {
        long now = System.currentTimeMillis();
        // Aggiorna posizioni ~20fps per ridurre sin/cos e jitter
        if (now - auroraTickMs >= 48) {
            auroraTickMs = now;
            double t = now / 1000.0;
            int a = Math.max(420, w / 2);
            int b = Math.max(420, h / 2);
            auroraPos[0] = w * 0.14f + (float) (Math.sin(t * 0.35) * w * 0.04);
            auroraPos[1] = h * -0.05f + (float) (Math.cos(t * 0.25) * h * 0.03);
            auroraPos[2] = w * 1.0f + (float) (Math.cos(t * 0.3) * w * 0.05);
            auroraPos[3] = h * 0.0f + (float) (Math.sin(t * 0.4) * h * 0.04);
            auroraPos[4] = w * 0.0f + (float) (Math.sin(t * 0.28) * w * 0.04);
            auroraPos[5] = h * 1.04f + (float) (Math.cos(t * 0.32) * h * 0.03);
            auroraPos[6] = w * 0.55f + (float) (Math.sin(t * 0.4) * w * 0.06);
            auroraPos[7] = h * 1.18f + (float) (Math.cos(t * 0.35) * h * 0.04);
            auroraPos[8] = w * 0.12f + (float) (Math.sin(t * 0.5) * w * 0.03);
            auroraPos[9] = h * 0.12f + (float) (Math.cos(t * 0.4) * h * 0.03);
            // store sizes in local via a/b encoded — redraw uses live sizes
        }
        int a = Math.max(420, w / 2);
        int b = Math.max(420, h / 2);
        drawBlob(ctx, amberTex, auroraPos[0], auroraPos[1], (int) (a * 1.1f));
        drawBlob(ctx, blueTex, auroraPos[2], auroraPos[3], a);
        drawBlob(ctx, violetTex, auroraPos[4], auroraPos[5], b);
        drawBlob(ctx, tealTex, auroraPos[6], auroraPos[7], (int) (b * 0.85f));
        drawBlob(ctx, amberTex, auroraPos[8], auroraPos[9], (int) (b * 0.55f));
    }

    private static void drawBlob(DrawContext ctx, Identifier tex, float cx, float cy, int size) {
        int x = Math.round(cx) - size / 2;
        int y = Math.round(cy) - size / 2;
        ctx.drawTexturedQuad(tex, x, y, x + size, y + size, 0f, 0f, 1f, 1f);
    }

    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        init();
        if (w <= 2 * SLICE || h <= 2 * SLICE) {
            chip(ctx, x, y, w, h, PANEL_BOTTOM);
            return;
        }
        float s = TILE;
        tex(ctx, glassTex, x, y, SLICE, SLICE, 0, 0, SLICE, SLICE);
        tex(ctx, glassTex, x + w - SLICE, y, SLICE, SLICE, s - SLICE, 0, s, SLICE);
        tex(ctx, glassTex, x, y + h - SLICE, SLICE, SLICE, 0, s - SLICE, SLICE, s);
        tex(ctx, glassTex, x + w - SLICE, y + h - SLICE, SLICE, SLICE, s - SLICE, s - SLICE, s, s);
        tex(ctx, glassTex, x + SLICE, y, w - 2 * SLICE, SLICE, SLICE, 0, s - SLICE, SLICE);
        tex(ctx, glassTex, x + SLICE, y + h - SLICE, w - 2 * SLICE, SLICE, SLICE, s - SLICE, s - SLICE, s);
        tex(ctx, glassTex, x, y + SLICE, SLICE, h - 2 * SLICE, 0, SLICE, SLICE, s - SLICE);
        tex(ctx, glassTex, x + w - SLICE, y + SLICE, SLICE, h - 2 * SLICE, s - SLICE, SLICE, s, s - SLICE);
        tex(ctx, glassTex, x + SLICE, y + SLICE, w - 2 * SLICE, h - 2 * SLICE, SLICE, SLICE, s - SLICE, s - SLICE);
    }

    private static void tex(DrawContext ctx, Identifier id, int x, int y, int w, int h, float u1, float v1, float u2, float v2) {
        texS(ctx, id, x, y, w, h, u1, v1, u2, v2, TILE);
    }

    private static void texS(DrawContext ctx, Identifier id, int x, int y, int w, int h, float u1, float v1, float u2, float v2, float tileSize) {
        ctx.drawTexturedQuad(id, x, y, x + w, y + h, u1 / tileSize, v1 / tileSize, u2 / tileSize, v2 / tileSize);
    }

    public static void card(DrawContext ctx, int x, int y, int w, int h, int fill) {
        chip(ctx, x, y, w, h, fill);
    }

    public static void chip(DrawContext ctx, int x, int y, int w, int h, int color) {
        init();
        if (w < 2 || h < 2) return;
        if (w < 2 * CHIP_SLICE || h < 2 * CHIP_SLICE) {
            // piccoli elementi: disc/rounded fill via texture se possibile
            Identifier id = ensureChip(color);
            ctx.drawTexturedQuad(id, x, y, x + w, y + h, 0f, 0f, 1f, 1f);
            return;
        }
        Identifier id = ensureChip(color);
        float s = CHIP_TILE;
        int sl = CHIP_SLICE;
        texS(ctx, id, x, y, sl, sl, 0, 0, sl, sl, s);
        texS(ctx, id, x + w - sl, y, sl, sl, s - sl, 0, s, sl, s);
        texS(ctx, id, x, y + h - sl, sl, sl, 0, s - sl, sl, s, s);
        texS(ctx, id, x + w - sl, y + h - sl, sl, sl, s - sl, s - sl, s, s, s);
        texS(ctx, id, x + sl, y, w - 2 * sl, sl, sl, 0, s - sl, sl, s);
        texS(ctx, id, x + sl, y + h - sl, w - 2 * sl, sl, sl, s - sl, s - sl, s, s);
        texS(ctx, id, x, y + sl, sl, h - 2 * sl, 0, sl, sl, s - sl, s);
        texS(ctx, id, x + w - sl, y + sl, sl, h - 2 * sl, s - sl, sl, s, s - sl, s);
        texS(ctx, id, x + sl, y + sl, w - 2 * sl, h - 2 * sl, sl, sl, s - sl, s - sl, s);
    }

    public static void disc(DrawContext ctx, int x, int y, int size, int color) {
        init();
        if (size <= 0) return;
        Identifier id = ensureDisc(color, size);
        ctx.drawTexturedQuad(id, x, y, x + size, y + size, 0f, 0f, 1f, 1f);
    }

    public static void disc(DrawContext ctx, int x, int y, int size) {
        disc(ctx, x, y, size, 0xFFE8EEF7);
    }

    public static void border(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + 1, BORDER_STRONG);
        ctx.fill(x, y + h - 1, x + w, y + h, BORDER);
        ctx.fill(x, y, x + 1, y + h, BORDER);
        ctx.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    /** Soft amber glow under a primary CTA (approssimazione box-shadow launcher). */
    public static void accentGlow(DrawContext ctx, int x, int y, int w, int h) {
        chip(ctx, x - 4, y - 3, w + 8, h + 6, AMBER_GLOW);
        chip(ctx, x - 2, y - 1, w + 4, h + 2, AMBER_SOFT);
    }

    /** Glow blu soft (skin panel / highlight secondario). */
    public static void blueGlow(DrawContext ctx, int x, int y, int w, int h) {
        chip(ctx, x - 4, y - 3, w + 8, h + 6, BLUE_SOFT);
    }

    /** Ombra soft sotto card (layer navy trasparente). */
    public static void softShadow(DrawContext ctx, int x, int y, int w, int h) {
        chip(ctx, x + 2, y + 3, w, h, 0x66050A16);
    }

    /** Card info stile launcher stats grid. */
    public static void statCard(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                                int x, int y, int w, int h, String label, String value) {
        softShadow(ctx, x, y, w, h);
        chip(ctx, x, y, w, h, GLASS_1);
        border(ctx, x, y, w, h);
        // specular top edge
        ctx.fill(x + 4, y + 1, x + w - 4, y + 2, 0x33FFFFFF);
        eyebrow(ctx, tr, label, x + 12, y + 10);
        String v = value == null ? "—" : value;
        ctx.drawText(tr, v, x + 12, y + h - 22, TEXT, true);
    }

    public static void sectionTitle(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                                    String text, int x, int y) {
        ctx.drawText(tr, text, x, y, TEXT, true);
    }

    /**
     * Toggle Material stile launcher (.toggle 52x30):
     * OFF = glass track, ON = gradient ambra + glow, knob bianco.
     */
    public static void toggle(DrawContext ctx, int x, int y, boolean on) {
        toggle(ctx, x, y, TOGGLE_W, TOGGLE_H, on);
    }

    public static void toggle(DrawContext ctx, int x, int y, int w, int h, boolean on) {
        init();
        if (on) {
            chip(ctx, x - 2, y - 1, w + 4, h + 2, AMBER_GLOW);
            chip(ctx, x, y, w, h, AMBER);
        } else {
            chip(ctx, x, y, w, h, GLASS_1);
            border(ctx, x, y, w, h);
        }
        int knob = Math.min(TOGGLE_KNOB, Math.max(12, h - 6));
        int pad = Math.max(2, (h - knob) / 2);
        int kx = on ? x + w - knob - pad : x + pad;
        disc(ctx, kx, y + pad, knob, 0xFFE8EEF7);
    }

    public static boolean hitToggle(double mx, double my, int x, int y) {
        return mx >= x && mx < x + TOGGLE_W && my >= y && my < y + TOGGLE_H;
    }

    /** Riga menu stile .menu-item / .menu-item.active — Material Expressive pill. */
    public static void navRow(DrawContext ctx, int x, int y, int w, int h, boolean selected, boolean hover) {
        if (selected) {
            chip(ctx, x - 1, y - 1, w + 2, h + 2, AMBER_GLOW);
            chip(ctx, x, y, w, h, AMBER_SOFT);
            ctx.fill(x, y, x + w, y + 1, AMBER_LINE);
            ctx.fill(x, y + h - 1, x + w, y + h, AMBER_LINE);
            ctx.fill(x, y, x + 1, y + h, AMBER_LINE);
            ctx.fill(x + w - 1, y, x + w, y + h, AMBER_LINE);
        } else if (hover) {
            chip(ctx, x, y, w, h, CARD_HOVER);
            border(ctx, x, y, w, h);
        } else {
            chip(ctx, x, y, w, h, CARD);
        }
    }

    public static void eyebrow(DrawContext ctx, net.minecraft.client.font.TextRenderer tr, String text, int x, int y) {
        ctx.drawText(tr, text.toUpperCase(), x, y, AMBER, false);
    }

    /** Multiline muted description; returns Y after last line. */
    public static int drawWrapped(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                                  String text, int x, int y, int maxW, int color) {
        if (text == null || text.isEmpty()) return y;
        StringBuilder line = new StringBuilder();
        int cy = y;
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (tr.getWidth(next) > maxW && !line.isEmpty()) {
                ctx.drawText(tr, line.toString(), x, cy, color, false);
                cy += 12;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) {
            ctx.drawText(tr, line.toString(), x, cy, color, false);
            cy += 12;
        }
        return cy;
    }
}
