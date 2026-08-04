package net.roleplayclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.screen.RcOptionsScreen;
import net.roleplayclient.screen.RcPauseScreen;
import net.roleplayclient.screen.RcTitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void roleplayclient$glassBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (self instanceof RcTitleScreen || self instanceof RcPauseScreen || self instanceof RcOptionsScreen) {
            return;
        }
        if (self instanceof StatsScreen
                || self instanceof AdvancementsScreen
                || self instanceof OptionsScreen
                || self instanceof SelectWorldScreen
                || self instanceof MultiplayerScreen
                || self instanceof GameMenuScreen
                || self instanceof TitleScreen) {
            GlassUi.background(context);
            ci.cancel();
        }
    }
}
