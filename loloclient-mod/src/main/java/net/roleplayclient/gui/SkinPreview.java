package net.roleplayclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.session.Session;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.roleplayclient.RoleplayClientMod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Preview full-body Crafty Skin Service ({@code render.crafty.gg/3d/full}),
 * parity launcher. Offline → {@code MHF_Steve}.
 * <p>
 * Crafty risponde in WebP → decode via TwelveMonkeys ImageIO → NativeImage.
 */
public final class SkinPreview {
    private static final String MHF_STEVE = "MHF_Steve";
    private static final String MHF_STEVE_UUID = "c06f89064c8a49119c29ea1dbd1aab82";

    private static final Identifier TEX_ID = Identifier.of(RoleplayClientMod.MOD_ID, "crafty_skin_preview");
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rc-crafty-skin");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final long RETRY_BACKOFF_MS = 30_000;
    private static final int MAX_ATTEMPTS = 3;

    private static volatile String loadingKey;
    private static volatile String readyKey;
    private static volatile boolean ready;
    private static volatile boolean failed;
    private static volatile boolean loading;
    private static volatile long lastFailMs;
    private static volatile int attempts;
    private static int texW = 1;
    private static int texH = 1;

    private SkinPreview() {
    }

    static {
        try {
            javax.imageio.ImageIO.scanForPlugins();
        } catch (Exception e) {
            System.err.println("[RoleplayClient] ImageIO plugin scan failed: " + e.getMessage());
        }
    }

    public static boolean isLoading() {
        return loading && !ready;
    }

    public static boolean draw(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;

        ensureLoaded(client);
        if (!ready || failed) return false;

        float scale = Math.min(w / (float) texW, h / (float) texH);
        int dw = Math.max(1, Math.round(texW * scale));
        int dh = Math.max(1, Math.round(texH * scale));
        int dx = x + (w - dw) / 2;
        int dy = y + (h - dh) / 2;
        ctx.drawTexturedQuad(TEX_ID, dx, dy, dx + dw, dy + dh, 0f, 0f, 1f, 1f);
        return true;
    }

    private static void ensureLoaded(MinecraftClient client) {
        String key = resolveRenderKey(client);
        if (key.equals(readyKey) && ready) return;
        if (key.equals(loadingKey) && (loading || ready)) return;
        // draw() chiama questo metodo a ogni frame: dopo un fallimento serve un
        // backoff (e un tetto di tentativi), altrimenti si martella il servizio
        // con una richiesta HTTP per frame finché non torna raggiungibile.
        if (key.equals(loadingKey) && failed) {
            if (attempts >= MAX_ATTEMPTS) return;
            if (System.currentTimeMillis() - lastFailMs < RETRY_BACKOFF_MS) return;
        }
        if (!key.equals(loadingKey)) attempts = 0;

        loadingKey = key;
        failed = false;
        ready = false;
        loading = true;
        IO.execute(() -> downloadAndRegister(client, key));
    }

    /** Premium MSA/MOJANG → UUID; offline/legacy → MHF_Steve. */
    static String resolveRenderKey(MinecraftClient client) {
        Session session = client.getSession();
        if (session == null) return MHF_STEVE;

        Session.AccountType type = session.getAccountType();
        UUID uuid = session.getUuidOrNull();

        if (type == Session.AccountType.MSA || type == Session.AccountType.MOJANG) {
            if (uuid != null) {
                return uuid.toString().replace("-", "").toLowerCase(Locale.ROOT);
            }
            String name = session.getUsername();
            if (name != null && !name.isBlank()) return name;
        }
        return MHF_STEVE;
    }

    private static void downloadAndRegister(MinecraftClient client, String key) {
        // Se la skin dell'account non è disponibile si ripiega su Steve
        // (quando la chiave non è già Steve).
        String[] urls = {
                craftyUrl(key, 300, 400),
                key.equalsIgnoreCase(MHF_STEVE) ? null : craftyUrl(MHF_STEVE_UUID, 300, 400)
        };

        byte[] bytes = null;
        for (String url : urls) {
            if (url == null) continue;
            bytes = httpGet(url);
            if (bytes != null && bytes.length > 64) break;
        }

        if (bytes == null || !key.equals(loadingKey)) {
            if (key.equals(loadingKey)) {
                failed = true;
                loading = false;
                lastFailMs = System.currentTimeMillis();
                attempts++;
            }
            return;
        }

        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bi == null) throw new IllegalStateException("ImageIO could not decode Crafty response (WebP?)");

            NativeImage image = toNative(bi);
            int w = image.getWidth();
            int h = image.getHeight();

            client.execute(() -> {
                if (!key.equals(loadingKey)) {
                    image.close();
                    return;
                }
                TextureManager tm = client.getTextureManager();
                tm.destroyTexture(TEX_ID);
                NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "rc-crafty-skin", image);
                tex.setFilter(true, false);
                tm.registerTexture(TEX_ID, tex);
                texW = w;
                texH = h;
                readyKey = key;
                ready = true;
                failed = false;
                loading = false;
            });
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Crafty skin decode failed: " + e.getMessage());
            if (key.equals(loadingKey)) {
                failed = true;
                loading = false;
                lastFailMs = System.currentTimeMillis();
                attempts++;
            }
        }
    }

    private static NativeImage toNative(BufferedImage bi) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        NativeImage img = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setColorArgb(x, y, bi.getRGB(x, y));
            }
        }
        return img;
    }

    private static String craftyUrl(String id, int width, int height) {
        // 3D Full Body — stessi parametri del builder crafty.gg/skin-service
        return "https://render.crafty.gg/3d/full/" + id
                + "?width=" + width
                + "&height=" + height
                + "&x=-30&z=50";
    }

    private static byte[] httpGet(String url) {
        try {
            // User-Agent onesto: identificarsi come browser con Referer contraffatto
            // per aggirare l'hotlink protection non è accettabile verso un servizio terzo.
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "RoleplayClient/" + RoleplayClientMod.MOD_ID)
                    .header("Accept", "image/webp,image/png,image/*,*/*;q=0.8")
                    .GET()
                    .build();
            HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() >= 200 && res.statusCode() < 300) return res.body();
            System.err.println("[RoleplayClient] Crafty HTTP " + res.statusCode() + " for " + url);
        } catch (Exception e) {
            System.err.println("[RoleplayClient] Crafty GET failed: " + e.getMessage());
        }
        return null;
    }
}
