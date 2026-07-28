package com.client.multichatwindows;

import com.client.multichatwindows.config.ui.NotificationHistoryScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindHandler {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("multichatwindows", "main"));

    private static KeyMapping historyKey;

    private KeybindHandler() {
    }

    public static void init() {
        historyKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.multichatwindows.notification_history",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (historyKey.consumeClick()) {
                com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(
                        new NotificationHistoryScreen(com.client.multichatwindows.util.MinecraftGuiAccess.currentScreen())
                );
            }
        });
    }
}
