package net.roleplayclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.screen.RcOptionsScreen;
import net.roleplayclient.screen.RcPauseScreen;
import net.roleplayclient.screen.RcTitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opzioni default di Minecraft: quando la modalità "GUI vanilla" è attiva la
 * schermata delle opzioni è quella originale; questo mixin aggiunge un pulsante
 * in alto a destra per tornare alle opzioni Roleplay Client.
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends OptionsScreen {

    public OptionsScreenMixin() {
        super(null, null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void roleplayclient$addRcGuiButton(CallbackInfo ci) {
        if (!RoleplayClientMod.config().getBool("rctitle", "vanilla", false)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        ButtonWidget btn = ButtonWidget.builder(
                        Text.literal("RC GUI"),
                        b -> {
                            RoleplayClientMod.config().set("rctitle", "vanilla", false);
                            Screen parent = client.world != null ? new RcPauseScreen(true) : new RcTitleScreen();
                            client.setScreen(new RcOptionsScreen(parent, client.options));
                        })
                .dimensions(this.width - 110, 10, 100, 20)
                .build();
        this.addDrawableChild(btn);
    }
}
