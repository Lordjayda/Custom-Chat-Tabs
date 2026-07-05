package com.client.multichatwindows;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.ui.ConfigHomeScreen;
import com.client.multichatwindows.hud.HudOverlay;
import com.client.multichatwindows.hud.WindowService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

public class MultiChatWindowsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.init();
        WindowService.rebuildForCurrentServer();
        HudOverlay.init();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(WindowService::rebuildForCurrentServer));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("mcw")
                        .executes(context -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.execute(() -> client.setScreen(new ConfigHomeScreen(null)));
                            return 1;
                        })
        ));
    }
}
