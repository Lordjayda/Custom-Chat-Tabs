package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotificationRulesScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private int scrollY = 0;

    public NotificationRulesScreen(Screen parent, String serverKey) {
        super(Component.translatable("multichatwindows.notifications.rules"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearWidgets();

        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        int centerX = width / 2;
        int top = 50;
        int bottom = height - 90;
        int rowHeight = 26;
        int maxScroll = Math.max(0, serverConfig.notifications.size() * rowHeight - (bottom - top));
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));

        int y = top - scrollY;
        for (int i = 0; i < serverConfig.notifications.size(); i++) {
            NotificationConfig notification = serverConfig.notifications.get(i);
            final int index = i;

            if (y + 22 >= top && y <= bottom) {
                addRenderableWidget(new DarkButton(centerX - 150, y, 238, 22, Component.literal(buildLabel(notification)), () ->
                        Minecraft.getInstance().setScreen(new NotificationEditScreen(this, serverKey, index))
                ));

                addRenderableWidget(new DarkButton(centerX + 94, y, 56, 22, Component.translatable("multichatwindows.delete"), () -> {
                    serverConfig.notifications.remove(index);
                    ConfigManager.saveServer(serverKey, serverConfig);
                    init();
                }));
            }
            y += rowHeight;
        }

        addRenderableWidget(new DarkButton(centerX - 130, height - 54, 260, 20, Component.translatable("multichatwindows.notifications.add"), () -> {
            NotificationConfig notification = new NotificationConfig();
            notification.id = "n" + System.currentTimeMillis();
            notification.enabled = true;
            notification.anyMessage = true;
            notification.keyword = "";
            notification.screenDependent = false;
            serverConfig.notifications.add(notification);
            ConfigManager.saveServer(serverKey, serverConfig);
            Minecraft.getInstance().setScreen(new NotificationEditScreen(this, serverKey, serverConfig.notifications.size() - 1));
        }));

        addRenderableWidget(new DarkButton(centerX - 130, height - 28, 260, 20, Component.translatable("multichatwindows.back"), () ->
                Minecraft.getInstance().setScreen(parent)
        ));
    }

    private String buildLabel(NotificationConfig notification) {
        String enabled = notification.enabled ? "✓" : "✗";
        String mode = notification.keyword != null && !notification.keyword.isBlank()
                ? "\"" + notification.keyword.trim() + "\""
                : Component.translatable("multichatwindows.notifications.mode.any.short").getString();
        String screens = notification.screenDependent
                ? Component.translatable("multichatwindows.notifications.screens.short").getString()
                : Component.translatable("multichatwindows.notifications.all_screens.short").getString();
        return enabled + " " + mode + " / " + screens;
    }

    @Override
    protected boolean onScroll(int delta) {
        scrollY += delta;
        init();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.notifications.rules"), width / 2, 14, 0xFFFFFFFF);
    }
}
