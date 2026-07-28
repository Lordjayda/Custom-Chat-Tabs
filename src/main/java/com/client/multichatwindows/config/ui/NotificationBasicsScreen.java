package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotificationBasicsScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int notificationIndex;
    private EditBox keywordField;

    public NotificationBasicsScreen(Screen parent, String serverKey, int notificationIndex) {
        super(Component.translatable("multichatwindows.notifications.trigger"));
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
        addRenderableWidget(new DarkButton(centerX - 120, y, 240, 22, Component.translatable((notification.keyword != null && !notification.keyword.isBlank()) ? "multichatwindows.notifications.mode.keyword" : "multichatwindows.notifications.mode.any"), () -> {
            if (notification.keyword != null && !notification.keyword.isBlank()) {
                notification.keyword = "";
                notification.anyMessage = true;
            } else {
                notification.keyword = "hi";
                notification.anyMessage = false;
            }
            ConfigManager.saveServer(serverKey, serverConfig);
            init();
        }));

        y += 32;
        keywordField = new EditBox(font, centerX - 120, y, 240, 22, Component.translatable("multichatwindows.notifications.keyword"));
        keywordField.setValue(notification.keyword == null ? "" : notification.keyword);
        keywordField.setResponder(value -> {
            notification.keyword = value;
            notification.anyMessage = value == null || value.isBlank();
            ConfigManager.saveServer(serverKey, serverConfig);
        });
        addRenderableWidget(keywordField);

        y += 32;
        addRenderableWidget(new DarkButton(centerX - 120, y, 240, 22, Component.translatable(notification.screenDependent ? "multichatwindows.notifications.screenDependent.on" : "multichatwindows.notifications.screenDependent.off"), () -> {
            notification.screenDependent = !notification.screenDependent;
            ConfigManager.saveServer(serverKey, serverConfig);
            init();
        }));

        

        addBackButton();
    }

    private void addBackButton() {
        addRenderableWidget(new DarkButton(width / 2 - 120, height - 28, 240, 20, Component.translatable("multichatwindows.back"), () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.notifications.trigger"), width / 2, 14, 0xFFFFFFFF);
        TooltipHelper.renderIfHovered(ctx, font, mouseX, mouseY, keywordField.getX(), keywordField.getY(), 240, 22, Component.translatable("multichatwindows.tooltip.notification.keyword"));
    }
}
