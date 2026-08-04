package net.roleplayclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.screen.RcTitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GUI default di Minecraft: quando la modalità "GUI vanilla" è attiva la
 * schermata principale è quella originale; questo mixin aggiunge un piccolo
 * pulsante in basso a destra per tornare alla GUI Roleplay Client.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends TitleScreen {

    public TitleScreenMixin() {
        super();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void roleplayclient$addRcGuiButton(CallbackInfo ci) {
        if (!RoleplayClientMod.config().getBool("rctitle", "vanilla", false)) return;
        ButtonWidget btn = ButtonWidget.builder(
                        Text.literal("RC GUI"),
                        b -> {
                            RoleplayClientMod.config().set("rctitle", "vanilla", false);
                            MinecraftClient.getInstance().setScreen(new RcTitleScreen());
                        })
                .dimensions(this.width - 110, this.height - 32, 100, 20)
                .build();
        this.addDrawableChild(btn);
    }
}
