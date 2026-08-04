package net.roleplayclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.roleplayclient.modules.CleanScreenshotManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clean Screenshot: intercetta la gestione input del client. Se il modulo è
 * attivo e F2 (screenshot) è stato premuto, la pressione viene consumata qui
 * (wasPressed) così lo screenshot vanilla non parte; la cattura pulita avviene
 * alla fine del render del mondo. Se il modulo è spento, il vanilla resta attivo.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientScreenshotMixin {

    @Inject(method = "handleInputEvents", at = @At("HEAD"), cancellable = true)
    private void roleplayclient_cleanScreenshot(CallbackInfo ci) {
        if (CleanScreenshotManager.tryConsumeF2()) {
            ci.cancel();
        }
    }
}
