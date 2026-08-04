package net.roleplayclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.gui.WindowIcons;
import net.roleplayclient.hud.HudRenderer;
import net.roleplayclient.modules.AlarmManager;
import net.roleplayclient.modules.ChatSearchManager;
import net.roleplayclient.modules.CinemaManager;
import net.roleplayclient.modules.CleanScreenshotManager;
import net.roleplayclient.modules.ClipReadyManager;
import net.roleplayclient.modules.ClickCounter;
import net.roleplayclient.modules.CrosshairManager;
import net.roleplayclient.modules.DesyncAlertManager;
import net.roleplayclient.modules.FacesManager;
import net.roleplayclient.modules.MentionsManager;
import net.roleplayclient.modules.QuickMessagesManager;
import net.roleplayclient.modules.SceneLogManager;
import net.roleplayclient.modules.SessionTimeManager;
import net.roleplayclient.modules.StopwatchManager;
import net.roleplayclient.modules.TimersManager;
import net.roleplayclient.modules.WaypointManager;
import net.roleplayclient.screen.ModMenuScreen;

public class RoleplayClientMod implements ClientModInitializer {
    public static final String MOD_ID = "roleplay-client";

    // Tasti: Alt = zoom, ò (tasto ";" US) = cinema, + (tasto "]" US) = messaggi rapidi
    public static final KeyBinding ZOOM_KEY = new KeyBinding("key.roleplay-client.zoom",
            InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_LEFT_ALT, "key.category.roleplay-client");
    public static final KeyBinding CINEMA_KEY = new KeyBinding("key.roleplay-client.cinema",
            InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_SEMICOLON, "key.category.roleplay-client");
    public static final KeyBinding QUICK_KEY = new KeyBinding("key.roleplay-client.quick",
            InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_RIGHT_BRACKET, "key.category.roleplay-client");
    public static final KeyBinding VOLTI_KEY = new KeyBinding("key.roleplay-client.volti",
            InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_V, "key.category.roleplay-client");
    public static final KeyBinding STOPWATCH_KEY = new KeyBinding("key.roleplay-client.stopwatch",
            InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_F10, "key.category.roleplay-client");

    private static RoleplayConfig config;
    private static float zoomSmooth = 0f;
    private static boolean windowIconApplied = false;

    public static RoleplayConfig config() {
        return config;
    }

    public static void showToast(String msg) {
        HudRenderer.showToast(msg);
    }

    /** Fattore di zoom corrente (0..1), aggiornato nel tick per una transizione morbida. */
    public static float zoomFactor() {
        return zoomSmooth;
    }

    /**
     * Scrive un segnale per il launcher e chiude il gioco: il launcher
     * rileva il segnale e riavvia Minecraft in automatico con le mod aggiornate.
     */
    public static void requestRestart() {
        try {
            java.nio.file.Path dir = FabricLoader.getInstance().getGameDir().resolve(".roleplay-client");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve("restart.json"),
                    "{\"action\":\"restart\"}", java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Se il segnale non è stato scritto il launcher non riavvierebbe nulla:
            // meglio restare in gioco che chiudersi senza ritorno.
            System.err.println("[RoleplayClient] Errore richiesta restart: " + e.getMessage());
            e.printStackTrace();
            showToast("Riavvio non riuscito: impossibile scrivere il segnale per il launcher");
            return;
        }
        MinecraftClient.getInstance().scheduleStop();
    }

    @Override
    public void onInitializeClient() {
        config = RoleplayConfig.load();

        KeyBindingHelper.registerKeyBinding(ZOOM_KEY);
        KeyBindingHelper.registerKeyBinding(CINEMA_KEY);
        KeyBindingHelper.registerKeyBinding(QUICK_KEY);
        KeyBindingHelper.registerKeyBinding(VOLTI_KEY);
        KeyBindingHelper.registerKeyBinding(STOPWATCH_KEY);

        // Pulsante "Roleplay Client Mods" dentro Opzioni -> Controlli, sotto "Impostazioni tasti"
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ControlsOptionsScreen) {
                addControlsButton(client, screen);
            }
        });

        // Moduli HUD (effetto immediato)
        HudRenderCallback.EVENT.register((context, tickCounter) -> HudRenderer.render(context));

        // Clean Screenshot: cattura del mondo renderizzato PRIMA dell'HUD.
        WorldRenderEvents.LAST.register(ctx -> CleanScreenshotManager.onWorldRender());

        // Eventi chat: volti conosciuti, menzioni e momenti clip
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> {
            FacesManager.onChat(message, signedMessage, sender, params, timestamp);
            MentionsManager.onChat(message, signedMessage, sender, params, timestamp);
            SceneLogManager.onChat(message, signedMessage, sender, params, timestamp);
            ClipReadyManager.onChat(sender);
        });

        ClientTickEvents.END_CLIENT_TICK.register(RoleplayClientMod::tick);

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new IdentifiableResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of(RoleplayClientMod.MOD_ID, "glassui_reinit");
                    }
                    @Override
                    public CompletableFuture<Void> reload(net.minecraft.resource.ResourceReloader.Synchronizer synchronizer, ResourceManager manager, java.util.concurrent.Executor prepareExecutor, java.util.concurrent.Executor applyExecutor) {
                        return synchronizer.whenPrepared(null).thenRun(() -> GlassUi.reinit());
                    }
                });
    }

    private static boolean prevLeftDown;
    private static boolean prevRightDown;

    private static void tick(MinecraftClient client) {
        // Contatore CPS: rilevamento del fronte di pressione (down && !prevDown).
        // wasLeftButtonClicked() è uno stato "tenuto premuto", non un evento:
        // campionarlo per tick contava ~20 CPS a pulsante tenuto.
        long handle = client.getWindow().getHandle();
        boolean leftDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        boolean rightDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (leftDown && !prevLeftDown) ClickCounter.registerLeft();
        if (rightDown && !prevRightDown) ClickCounter.registerRight();
        prevLeftDown = leftDown;
        prevRightDown = rightDown;

        // Zoom con transizione morbida
        boolean zoom = config.isEnabled("zoom") && ZOOM_KEY.isPressed();
        float target = zoom ? 1f : 0f;
        zoomSmooth += (target - zoomSmooth) * 0.25f;
        if (Math.abs(zoomSmooth - target) < 0.01f) zoomSmooth = target;

        // Cinema
        while (CINEMA_KEY.wasPressed()) {
            if (config.isEnabled("cinema")) CinemaManager.toggle();
        }

        // Messaggi rapidi (apri lista)
        while (QUICK_KEY.wasPressed()) {
            if (config.isEnabled("rpmessages")) {
                client.setScreen(new net.roleplayclient.screen.QuickMessagesScreen(client.currentScreen));
                break; // apri solo una volta
            }
        }

        // Volti conosciuti (apri lista)
        while (VOLTI_KEY.wasPressed()) {
            if (config.isEnabled("volti")) {
                client.setScreen(new net.roleplayclient.screen.FacesScreen(client.currentScreen));
                break; // apri solo una volta
            }
        }

        applyWindowIconOnce(client);

        // Tasti assegnati ai singoli messaggi rapidi
        QuickMessagesManager.tickKeys();

        // Cronometro RP (tasto F10): alterna avvio/pausa, doppio click resetta
        while (STOPWATCH_KEY.wasPressed()) {
            if (config.isEnabled("rpstopwatch")) StopwatchManager.toggle();
        }

        FacesManager.tick();
        WaypointManager.tick();
        AlarmManager.tick();
        CinemaManager.tick();
        TimersManager.tick();
        SessionTimeManager.tick();
        DesyncAlertManager.tick();
        ClipReadyManager.tick();
        HudRenderer.tickFps();
    }

    private static void applyWindowIconOnce(MinecraftClient client) {
        if (windowIconApplied || client.getWindow() == null) return;
        windowIconApplied = true;
        try {
            GlassUi.init();
            WindowIcons.apply();
        } catch (Exception e) {
            System.err.println("[Roleplay Client] Init UI/icona: " + e.getMessage());
        }
    }

    /**
     * Aggiunge il pulsante "Roleplay Client Mods" alla schermata Controlli
     * (Opzioni -> Controlli), nella riga immediatamente sotto "Impostazioni tasti".
     * La schermata usa un OptionListWidget (lista scorrevole): la nuova voce
     * viene inserita subito dopo quella del pulsante KeyBinds, quindi segue
     * lo scroll come le altre righe.
     */
    private void addControlsButton(MinecraftClient client, Screen screen) {
        try {
            OptionListWidget body = null;
            for (Element child : screen.children()) {
                if (child instanceof OptionListWidget olw) {
                    body = olw;
                    break;
                }
            }
            if (body == null) {
                System.out.println("[Roleplay Client] Controlli: OptionListWidget non trovato");
                return;
            }

            // Pulsante a tutta larghezza della riga (310px, come getRowWidth())
            ButtonWidget button = ButtonWidget.builder(Text.literal("Roleplay Client Mods"),
                            b -> client.setScreen(new ModMenuScreen(screen)))
                    .width(310)
                    .build();

            body.addWidgetEntry(button, null);
            @SuppressWarnings("unchecked")
            java.util.List<Object> children = (java.util.List<Object>) (java.util.List<?>) body.children();
            java.util.List<Object> copy = new java.util.ArrayList<>(children);
            if (copy.size() >= 2) {
                Object entry = copy.remove(copy.size() - 1);
                copy.add(1, entry);
            }
            children.clear();
            children.addAll(copy);
            System.out.println("[Roleplay Client] Pulsante aggiunto in Controlli (riga 1)");
        } catch (Exception e) {
            System.err.println("[Roleplay Client] Errore aggiunta pulsante controlli: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
