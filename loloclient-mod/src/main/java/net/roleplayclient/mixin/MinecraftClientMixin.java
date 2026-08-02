package net.roleplayclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.roleplayclient.screen.RcOptionsScreen;
import net.roleplayclient.screen.RcPauseScreen;
import net.roleplayclient.screen.RcTitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Sostituisce Title / Pause / Options vanilla con le screen Liquid Glass RC.
 */
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
            return new RcTitleScreen();
        }
        if (screen.getClass() == GameMenuScreen.class) {
            return new RcPauseScreen();
        }
        if (screen.getClass() == OptionsScreen.class) {
            Screen parent = client.currentScreen;
            // Da sotto-menu opzioni (Video/Controls/…) torna a Pause/Title, non al sotto-menu
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
}
