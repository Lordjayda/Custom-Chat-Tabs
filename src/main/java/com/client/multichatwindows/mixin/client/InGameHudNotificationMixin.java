package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.notification.NotificationOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudNotificationMixin {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void mcw$renderNotifications(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }
        NotificationOverlay.render(context, client.textRenderer, context.getScaledWindowWidth());
    }
}
