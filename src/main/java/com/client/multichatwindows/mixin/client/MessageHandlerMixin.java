package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.WindowService;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;

@Mixin(ChatListener.class)
public class MessageHandlerMixin {
    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$onGameMessage(Component message, boolean overlay, CallbackInfo ci) {
        if (overlay) {
            return;
        }
        if (!WindowService.isGloballyEnabled()) {
            WindowService.captureWhileDisabled(message);
            return;
        }
        if (WindowService.routeIncoming(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$onDisguisedChatMessage(Component message, ChatType.Bound bound, CallbackInfo ci) {
        if (!WindowService.isGloballyEnabled()) {
            WindowService.captureWhileDisabled(message);
            return;
        }
        if (WindowService.routeIncoming(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$onPlayerChatMessage(PlayerChatMessage message, GameProfile sender, ChatType.Bound bound, CallbackInfo ci) {
        Component routed = WindowService.componentFromPlayerChatMessage(message, sender, bound);
        if (!WindowService.isGloballyEnabled()) {
            WindowService.captureWhileDisabled(routed);
            return;
        }
        if (WindowService.routeIncoming(routed)) {
            ci.cancel();
        }
    }

    @Inject(method = "logSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/time/Instant;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$addToChatLog(Component message, Instant timestamp, CallbackInfo ci) {
        if (!WindowService.isGloballyEnabled()) {
            WindowService.captureWhileDisabled(message);
            return;
        }
        if (WindowService.routeIncoming(message)) {
            ci.cancel();
        }
    }
}
