package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class NotificationRulesScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private int scrollY = 0;

    public NotificationRulesScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.notifications.rules"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();

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
                addDrawableChild(new DarkButton(centerX - 150, y, 238, 22, Text.literal(buildLabel(notification)), () ->
                        MinecraftClient.getInstance().setScreen(new NotificationEditScreen(this, serverKey, index))
                ));

                addDrawableChild(new DarkButton(centerX + 94, y, 56, 22, Text.translatable("multichatwindows.delete"), () -> {
                    serverConfig.notifications.remove(index);
                    ConfigManager.saveServer(serverKey, serverConfig);
                    init();
                }));
            }
            y += rowHeight;
        }

        addDrawableChild(new DarkButton(centerX - 130, height - 54, 260, 20, Text.translatable("multichatwindows.notifications.add"), () -> {
            NotificationConfig notification = new NotificationConfig();
            notification.id = "n" + System.currentTimeMillis();
            notification.enabled = true;
            notification.anyMessage = true;
            notification.keyword = "";
            notification.screenDependent = false;
            serverConfig.notifications.add(notification);
            ConfigManager.saveServer(serverKey, serverConfig);
            MinecraftClient.getInstance().setScreen(new NotificationEditScreen(this, serverKey, serverConfig.notifications.size() - 1));
        }));

        addDrawableChild(new DarkButton(centerX - 130, height - 28, 260, 20, Text.translatable("multichatwindows.back"), () ->
                MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    private String buildLabel(NotificationConfig notification) {
        String enabled = notification.enabled ? "✓" : "✗";
        String mode = notification.keyword != null && !notification.keyword.isBlank()
                ? "\"" + notification.keyword.trim() + "\""
                : Text.translatable("multichatwindows.notifications.mode.any.short").getString();
        String screens = notification.screenDependent
                ? Text.translatable("multichatwindows.notifications.screens.short").getString()
                : Text.translatable("multichatwindows.notifications.all_screens.short").getString();
        return enabled + " " + mode + " / " + screens;
    }

    @Override
    protected boolean onScroll(int delta) {
        scrollY += delta;
        init();
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.rules"), width / 2, 14, 0xFFFFFFFF);
    }
}
