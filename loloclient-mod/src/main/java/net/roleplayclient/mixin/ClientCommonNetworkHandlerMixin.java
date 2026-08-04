package net.roleplayclient.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.roleplayclient.modules.DesyncAlertManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allerta desync: registra l'arrivo dei keep-alive dal server (eseguito su
 * thread di rete; il manager usa un volatile). Se i keep-alive si fermano
 * oltre la soglia, l'HUD mostra l'avviso.
 */
@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {

    @Inject(method = "onKeepAlive", at = @At("HEAD"))
    private void roleplayclient_keepAlive(KeepAliveS2CPacket packet, CallbackInfo ci) {
        DesyncAlertManager.onKeepAlive();
    }
}
