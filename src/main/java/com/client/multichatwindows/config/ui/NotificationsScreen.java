package com.client.multichatwindows.config.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotificationsScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    public NotificationsScreen(Screen parent, String serverKey) {
        super(Component.translatable("multichatwindows.notifications.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = width / 2;
        int y = 54;

        addRenderableWidget(new DarkButton(centerX - 130, y, 260, 22, Component.translatable("multichatwindows.notifications.rules"), () ->
                Minecraft.getInstance().setScreen(new NotificationRulesScreen(this, serverKey))
        ));

        y += 32;

        addRenderableWidget(new DarkButton(centerX - 130, y, 260, 22, Component.translatable("multichatwindows.position_style"), () ->
                Minecraft.getInstance().setScreen(new NotificationPositionStyleScreen(this, serverKey))
        ));

        y += 32;

        addRenderableWidget(new DarkButton(centerX - 130, y, 260, 22, Component.translatable("multichatwindows.notifications.history"), () ->
                Minecraft.getInstance().setScreen(new NotificationHistoryScreen(this))
        ));

        addRenderableWidget(new DarkButton(centerX - 130, height - 28, 260, 20, Component.translatable("multichatwindows.back"), () ->
                Minecraft.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.notifications.title"), width / 2, 14, 0xFFFFFFFF);
    }
}
