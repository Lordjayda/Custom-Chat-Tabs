package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotificationEditScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int notificationIndex;

    public NotificationEditScreen(Screen parent, String serverKey, int notificationIndex) {
        super(Component.translatable("multichatwindows.notifications.edit.title"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.notificationIndex = notificationIndex;
    }

    @Override
    protected void init() {
        clearWidgets();

        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        if (notificationIndex < 0 || notificationIndex >= serverConfig.notifications.size()) {
            addBackButton();
            return;
        }

        NotificationConfig notification = serverConfig.notifications.get(notificationIndex);
        int centerX = width / 2;
        int y = 54;

        addRenderableWidget(new DarkButton(centerX - 120, y, 240, 22, Component.translatable(notification.enabled ? "multichatwindows.notifications.enabled.on" : "multichatwindows.notifications.enabled.off"), () -> {
            notification.enabled = !notification.enabled;
            ConfigManager.saveServer(serverKey, serverConfig);
            init();
        }));

        y += 32;
        addRenderableWidget(new DarkButton(centerX - 120, y, 240, 22, Component.translatable("multichatwindows.notifications.trigger"), () ->
                Minecraft.getInstance().setScreen(new NotificationBasicsScreen(this, serverKey, notificationIndex))
        ));

        y += 32;
        addRenderableWidget(new DarkButton(centerX - 120, y, 240, 22, Component.translatable("multichatwindows.notifications.screens"), () ->
                Minecraft.getInstance().setScreen(new ScreenSelectionScreen(this, serverKey, notificationIndex))
        ));

        y += 32;
        addRenderableWidget(new DarkButton(centerX - 120, y, 240, 22, Component.translatable("multichatwindows.notifications.sound"), () ->
                Minecraft.getInstance().setScreen(new NotificationSoundScreen(this, serverKey, notificationIndex))
        ));

        addBackButton();
    }

    private void addBackButton() {
        addRenderableWidget(new DarkButton(width / 2 - 120, height - 28, 240, 20, Component.translatable("multichatwindows.back"), () ->
                Minecraft.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.notifications.edit.title"), width / 2, 14, 0xFFFFFFFF);
    }
}
