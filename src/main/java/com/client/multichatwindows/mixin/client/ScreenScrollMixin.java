package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ScreenScrollMixin {

    // Variante A: (mouseX, mouseY, amount) -> boolean
    @Inject(method = "mouseScrolled(DDD)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$mouseScrolled3(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (WindowService.scrollAt(mouseX, mouseY, amount)) {
            cir.setReturnValue(true);
        }
    }

    // Variante B: (mouseX, mouseY, horizontalAmount, verticalAmount) -> boolean
    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$mouseScrolled4(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (WindowService.scrollAt(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}