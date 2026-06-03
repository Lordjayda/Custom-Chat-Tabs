package com.client.multichatwindows.notification;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public final class NotificationHudEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.textRenderer == null) {
                return;
            }
            NotificationOverlay.render(drawContext, client.textRenderer, drawContext.getScaledWindowWidth());
        });
    }
}
