package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.ChatWindow;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChatComponent.class)
public class ChatHudLimitMixin {

    @ModifyConstant(method = "addMessageToDisplayQueue", constant = @Constant(intValue = 100), require = 0)
    private int mcw$increaseVisibleMessageLimit(int original) {
        return ChatWindow.MAX_MESSAGES;
    }

    @ModifyConstant(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", constant = @Constant(intValue = 100), require = 0)
    private int mcw$increaseAddMessageLimit(int original) {
        return ChatWindow.MAX_MESSAGES;
    }
}
