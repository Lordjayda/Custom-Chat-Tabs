package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.ChatWindow;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChatHud.class)
public class ChatHudLimitMixin {

    @ModifyConstant(method = "addVisibleMessage", constant = @Constant(intValue = 100), require = 0)
    private int mcw$increaseVisibleMessageLimit(int original) {
        return ChatWindow.MAX_MESSAGES;
    }

    @ModifyConstant(method = "addMessage(Lnet/minecraft/text/Text;)V", constant = @Constant(intValue = 100), require = 0)
    private int mcw$increaseAddMessageLimit(int original) {
        return ChatWindow.MAX_MESSAGES;
    }
}
