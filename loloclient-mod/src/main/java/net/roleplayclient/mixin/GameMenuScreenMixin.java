package net.roleplayclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.screen.RcPauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pausa default di Minecraft: quando la modalità "GUI vanilla" è attiva la
 * schermata di pausa è quella originale; questo mixin aggiunge un pulsante in
 * basso a destra per tornare alla pausa Roleplay Client.
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends GameMenuScreen {

    public GameMenuScreenMixin() {
        super(false);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void roleplayclient$addRcGuiButton(CallbackInfo ci) {
        if (!RoleplayClientMod.config().getBool("rctitle", "vanilla", false)) return;
        ButtonWidget btn = ButtonWidget.builder(
                        Text.literal("RC GUI"),
                        b -> {
                            RoleplayClientMod.config().set("rctitle", "vanilla", false);
                            MinecraftClient.getInstance().setScreen(new RcPauseScreen(true));
                        })
                .dimensions(this.width - 110, this.height - 32, 100, 20)
                .build();
        this.addDrawableChild(btn);
    }
}
