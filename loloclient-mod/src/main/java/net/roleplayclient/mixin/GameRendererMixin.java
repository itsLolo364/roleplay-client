package net.roleplayclient.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.roleplayclient.RoleplayClientMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zoom: riduce il FOV finché il tasto dedicato è premuto, con transizione morbida.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
    private void roleplayclient_zoom(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float factor = RoleplayClientMod.zoomFactor();
        if (factor > 0.001f) {
            cir.setReturnValue(cir.getReturnValueF() * (1.0f - factor * 0.5f));
        }
    }
}
