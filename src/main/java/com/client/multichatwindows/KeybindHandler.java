package com.client.multichatwindows;

import com.client.multichatwindows.config.ui.NotificationHistoryScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindHandler {
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("multichatwindows", "main"));

    private static KeyBinding historyKey;

    private KeybindHandler() {
    }

    public static void init() {
        historyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.multichatwindows.notification_history",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (historyKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(
                        new NotificationHistoryScreen(MinecraftClient.getInstance().currentScreen)
                );
            }
        });
    }
}
