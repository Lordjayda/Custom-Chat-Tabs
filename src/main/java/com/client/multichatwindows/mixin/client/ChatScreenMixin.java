package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.ChatCopyMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void mcw$renderCopyMenu(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.textRenderer != null) {
            ChatCopyMenu.render(context, client.textRenderer);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"), require = 0)
    private void mcw$removed(CallbackInfo ci) {
        ChatCopyMenu.close();
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$mouseClickedClickObject(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        handleClick(click.x(), click.y(), click.button(), cir);
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$mouseClickedLegacy(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        handleClick(mouseX, mouseY, button, cir);
    }

    private void handleClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (ChatCopyMenu.isVisible()) {
            if (ChatCopyMenu.handleClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
            return;
        }

        if (button == 1) {
            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = client == null ? 320 : client.getWindow().getScaledWidth();
            int screenHeight = client == null ? 240 : client.getWindow().getScaledHeight();
            if (ChatCopyMenu.open(mouseX, mouseY, screenWidth, screenHeight)) {
                cir.setReturnValue(true);
            }
        }
    }
}
