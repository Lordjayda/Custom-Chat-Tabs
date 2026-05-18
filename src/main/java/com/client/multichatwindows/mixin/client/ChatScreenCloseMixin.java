package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenCloseMixin {

    /**
     * Wird aufgerufen, wenn der Screen verworfen/geschlossen wird.
     * (Wird beim Screenwechsel von MinecraftClient#setScreen getriggert.)
     */
    @Inject(method = "removed()V", at = @At("HEAD"), require = 0)
    private void mcw$removed(CallbackInfo ci) {
        WindowService.resetScrollOnChatClose();
    }

    /**
     * Fallback für Yarn/MC Versionen, die statt removed() ein close()/onClose() haben.
     * require=0 verhindert einen Crash, falls es die Methode nicht gibt.
     */
    @Inject(method = "close()V", at = @At("HEAD"), require = 0)
    private void mcw$close(CallbackInfo ci) {
        WindowService.resetScrollOnChatClose();
    }
}