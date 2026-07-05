package com.client.multichatwindows.notification;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class NotificationHudEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("multichatwindows", "notifications"), (drawContext, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.font == null) {
                return;
            }
            NotificationOverlay.render(drawContext, client.font, drawContext.guiWidth());
        });
    }
}
