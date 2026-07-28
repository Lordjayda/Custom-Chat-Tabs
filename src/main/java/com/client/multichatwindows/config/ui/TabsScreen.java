package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TabsScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    public TabsScreen(Screen parent, String serverKey) {
        super(Component.translatable("multichatwindows.servermenu.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearWidgets();
        ServerConfig sc = ConfigManager.getOrCreateServer(serverKey);

        int cx = width / 2;
        int y = 54;

        addRenderableWidget(new DarkButton(
                cx - 110, y, 220, 20,
                Component.translatable("multichatwindows.screens.open"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(new ScreensMenuScreen(this, serverKey))
        ));
        y += 26;

        addRenderableWidget(new DarkButton(
                cx - 110, y, 220, 20,
                Component.translatable("multichatwindows.notifications.open"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(new NotificationsScreen(this, serverKey))
        ));
        y += 26;

        addRenderableWidget(new DarkButton(
                cx - 110, y, 220, 20,
                Component.translatable("multichatwindows.chatfilters.open"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(new ChatFiltersScreen(this, serverKey))
        ));
        y += 26;

        addRenderableWidget(new DarkButton(
                cx - 110, y, 220, 20,
                Component.literal("Timestamp Style"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(new TimestampMenuScreen(this, serverKey))
        ));
        y += 26;

        addRenderableWidget(new DarkButton(
                cx - 110, y, 220, 20,
                Component.translatable(sc.noChatClearing ? "multichatwindows.no_chat_clearing.on" : "multichatwindows.no_chat_clearing.off"),
                () -> {
                    sc.noChatClearing = !sc.noChatClearing;
                    ConfigManager.saveServer(serverKey, sc);
                    WindowService.rebuildForCurrentServer();
                    init();
                }
        ));

        

        addRenderableWidget(new DarkButton(
                cx - 110, height - 28, 220, 20,
                Component.translatable("multichatwindows.back"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent)
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                font,
                Component.translatable("multichatwindows.servermenu.title"),
                width / 2,
                14,
                0xFFFFFFFF
        );
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                font,
                Component.translatable("multichatwindows.server.current", Component.literal(serverKey)),
                width / 2,
                28,
                0xFFB0B0B0
        );
    }
}
