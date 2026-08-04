package net.roleplayclient.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.roleplayclient.RoleplayClientMod;
import net.roleplayclient.modules.ChatSearchManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ricerca chat: aggiunge un campo di ricerca sopra la riga di input della
 * chat. Scrivendoci si imposta la query che filtra il cronologico.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenSearchMixin {

    @Shadow
    protected TextFieldWidget chatField;

    @Shadow
    protected TextRenderer textRenderer;

    @Shadow
    protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T child);

    @Unique
    private TextFieldWidget roleplayclient$searchField;

    @Inject(method = "init", at = @At("TAIL"))
    private void roleplayclient_addSearchField(CallbackInfo ci) {
        if (!RoleplayClientMod.config().isEnabled("chatsearch")) return;
        int x = this.chatField.getX();
        int y = this.chatField.getY() - 26;
        int w = Math.min(280, this.chatField.getWidth());
        this.roleplayclient$searchField = new TextFieldWidget(this.textRenderer, x, y, w, 20, Text.literal(""));
        this.roleplayclient$searchField.setMaxLength(60);
        this.roleplayclient$searchField.setPlaceholder(Text.literal("Cerca nella chat..."));
        this.roleplayclient$searchField.setChangedListener(ChatSearchManager::setQuery);
        this.roleplayclient$searchField.setText(ChatSearchManager.query());
        this.addDrawableChild(this.roleplayclient$searchField);
    }
}
