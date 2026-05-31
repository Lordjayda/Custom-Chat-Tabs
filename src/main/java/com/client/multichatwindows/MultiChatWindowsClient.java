package com.client.multichatwindows;
import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.hud.HudOverlay;
import com.client.multichatwindows.hud.WindowService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
public class MultiChatWindowsClient implements ClientModInitializer {
    @Override public void onInitializeClient(){ ConfigManager.init(); WindowService.rebuildForCurrentServer(); HudOverlay.init(); ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->client.execute(WindowService::rebuildForCurrentServer)); }
}
