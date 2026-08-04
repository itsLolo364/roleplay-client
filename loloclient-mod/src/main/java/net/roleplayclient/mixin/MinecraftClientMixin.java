package net.roleplayclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.screen.RcOptionsScreen;
import net.roleplayclient.screen.RcPauseScreen;
import net.roleplayclient.screen.RcTitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.lang.reflect.Field;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen roleplayclient$redirectScreens(Screen screen) {
        if (screen == null) return null;
        if (screen instanceof RcTitleScreen || screen instanceof RcPauseScreen || screen instanceof RcOptionsScreen) {
            return screen;
        }
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (screen.getClass() == TitleScreen.class) {
            if (RoleplayClientMod.config().getBool("rctitle", "vanilla", false)) {
                return screen;
            }
            boolean fade = readBool(screen, "doBackgroundFade", true);
            return new RcTitleScreen(fade);
        }
        if (screen.getClass() == GameMenuScreen.class) {
            boolean pauseOnly = readBool(screen, "pauseOnly", false);
            return new RcPauseScreen(pauseOnly);
        }
        if (screen.getClass() == OptionsScreen.class) {
            Screen parent = client.currentScreen;
            if (parent == null
                    || parent instanceof OptionsScreen
                    || parent instanceof RcOptionsScreen
                    || parent.getClass().getName().contains(".option.")) {
                parent = client.world != null ? new RcPauseScreen() : new RcTitleScreen();
            }
            return new RcOptionsScreen(parent, client.options);
        }
        return screen;
    }

    private static boolean readBool(Screen screen, String fieldName, boolean def) {
        try {
            Field f = screen.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.getBoolean(screen);
        } catch (Exception e) {
            return def;
        }
    }
}
