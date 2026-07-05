package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), require = 0)
    private void mcw$onSetScreen(Screen screen, CallbackInfo ci) {
        Minecraft minecraftClient = (Minecraft) (Object) this;
        if (minecraftClient.screen instanceof ChatScreen && !(screen instanceof ChatScreen)) {
            WindowService.resetScrollOnChatClose();
        }
    }
}
