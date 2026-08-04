package net.roleplayclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.modules.CrosshairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Crosshair RP: se il modulo è attivo, cancella il mirino vanilla e ne
 * disegna uno personalizzato (colore/dimensione dalla config).
 */
@Mixin(InGameHud.class)
public class InGameHudCrosshairMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void roleplayclient_crosshair(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
        if (!RoleplayClientMod.config().isEnabled("crosshair")) return;
        ci.cancel();
        CrosshairManager.render(ctx, MinecraftClient.getInstance());
    }
}
