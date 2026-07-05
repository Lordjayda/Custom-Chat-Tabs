package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ChatComponent.DisplayMode;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatHudFeedbackMixin {
    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void mcw$cancelAlreadyRoutedChatComponentMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (WindowService.wasAlreadyRouted(message)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void mcw$hideVanillaChatWhenEnabled(GuiGraphicsExtractor context, Font font, int tickCount, int mouseX, int mouseY, DisplayMode displayMode, boolean focused, CallbackInfo ci) {
        if (WindowService.isGloballyEnabled()) {
            WindowService.clearVanillaChatMessages();
            ci.cancel();
        }
    }
}
