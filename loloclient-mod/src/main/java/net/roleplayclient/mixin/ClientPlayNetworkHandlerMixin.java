package net.roleplayclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.roleplayclient.modules.ClipReadyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clip Ready: ogni suono ricevuto dal server fa lampeggiare l'indicatore
 * (momento potenzialmente da registrare). Il flash è gestito dal manager.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void roleplayclient_clipSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        ClipReadyManager.onPlaySound();
    }
}
