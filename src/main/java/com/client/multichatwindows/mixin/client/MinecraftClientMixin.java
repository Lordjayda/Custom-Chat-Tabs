package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.WindowService;
import com.client.multichatwindows.util.MinecraftGuiAccess;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MinecraftClientMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), require = 0)
    private void mcw$onSetScreen(Screen screen, CallbackInfo ci) {
        Screen previous = MinecraftGuiAccess.currentScreen();
        if (previous instanceof ChatScreen && !(screen instanceof ChatScreen)) {
            WindowService.resetScrollOnChatClose();
        }
    }
}
