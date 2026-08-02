package net.roleplayclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.roleplayclient.gui.GlassUi;
import net.roleplayclient.screen.RcOptionsScreen;
import net.roleplayclient.screen.RcPauseScreen;
import net.roleplayclient.screen.RcTitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chrome Liquid Glass su schermate menu vanilla (world/multiplayer/options figlie).
 */
@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void roleplayclient$glassBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (self instanceof RcTitleScreen || self instanceof RcPauseScreen || self instanceof RcOptionsScreen) {
            return;
        }
        String name = self.getClass().getName();
        if (name.contains("option")
                || name.contains("pack")
                || name.contains("world")
                || name.contains("multiplayer")
                || name.contains("realms")
                || name.contains("advancement")
                || name.contains("StatsScreen")
                || name.contains("stat")) {
            GlassUi.background(context);
            ci.cancel();
        }
    }
}
