package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotificationPositionStyleScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    public NotificationPositionStyleScreen(Screen parent, String serverKey) {
        super(Component.translatable("multichatwindows.position_style"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearWidgets();
        int centerX = width / 2;
        int y = 54;

        addRenderableWidget(new DarkButton(centerX - 130, y, 260, 22, Component.translatable("multichatwindows.position"), () ->
                Minecraft.getInstance().setScreen(new NotificationPositionScreen(this, serverKey))
        ));

        y += 32;
        addRenderableWidget(new DarkButton(centerX - 130, y, 260, 22, Component.translatable("multichatwindows.style"), () ->
                Minecraft.getInstance().setScreen(new NotificationStyleScreen(this, serverKey))
        ));

        y += 32;
        addRenderableWidget(new DarkButton(centerX - 130, y, 260, 22, Component.translatable("multichatwindows.reset"), () -> {
            ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
            serverConfig.notificationX = -1;
            serverConfig.notificationY = 12;
            serverConfig.notificationWidth = 220;
            serverConfig.notificationTextScale = 1.0f;
            serverConfig.notificationMaxVisible = 5;
            ConfigManager.saveServer(serverKey, serverConfig);
        }));

        addRenderableWidget(new DarkButton(centerX - 130, height - 28, 260, 20, Component.translatable("multichatwindows.back"), () ->
                Minecraft.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.position_style"), width / 2, 14, 0xFFFFFFFF);
    }
}
