package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.config.ui.ChatActionMenuScreen;
import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$mouseClickedClickObject(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() == 1) {
            openActionScreen(click.x(), click.y(), cir);
        }
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$mouseClickedLegacy(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 1) {
            openActionScreen(mouseX, mouseY, cir);
        }
    }

    private void openActionScreen(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        Text message = WindowService.messageAt(mouseX, mouseY);
        if (message != null) {
            MinecraftClient.getInstance().setScreen(new ChatActionMenuScreen((ChatScreen) (Object) this, message));
            cir.setReturnValue(true);
        }
    }
}
