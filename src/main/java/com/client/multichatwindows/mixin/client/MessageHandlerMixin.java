package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.time.Instant;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin {
    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void mcw$onGameMessage(Text message, boolean overlay, CallbackInfo ci) {
        if (overlay) return;
        if (WindowService.routeIncoming(message)) ci.cancel();
    }

    @Inject(method = "addToChatLog(Lnet/minecraft/text/Text;Ljava/time/Instant;)V", at = @At("HEAD"), cancellable = true)
    private void mcw$addToChatLog(Text message, Instant timestamp, CallbackInfo ci) {
        if (WindowService.routeIncoming(message)) ci.cancel();
    }
}
