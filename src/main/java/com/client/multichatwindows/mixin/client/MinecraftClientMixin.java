package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), require = 0)
    private void mcw$onSetScreen(Screen screen, CallbackInfo ci) {
        MinecraftClient minecraftClient = (MinecraftClient) (Object) this;
        if (minecraftClient.currentScreen instanceof ChatScreen && !(screen instanceof ChatScreen)) {
            WindowService.resetScrollOnChatClose();
        }
    }
}
