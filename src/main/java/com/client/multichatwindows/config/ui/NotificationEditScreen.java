package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class NotificationEditScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int notificationIndex;

    public NotificationEditScreen(Screen parent, String serverKey, int notificationIndex) {
        super(Text.translatable("multichatwindows.notifications.edit.title"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.notificationIndex = notificationIndex;
    }

    @Override
    protected void init() {
        clearChildren();

        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        if (notificationIndex < 0 || notificationIndex >= serverConfig.notifications.size()) {
            addBackButton();
            return;
        }

        NotificationConfig notification = serverConfig.notifications.get(notificationIndex);
        int centerX = width / 2;
        int y = 54;

        addDrawableChild(new DarkButton(centerX - 120, y, 240, 22, Text.translatable(notification.enabled ? "multichatwindows.notifications.enabled.on" : "multichatwindows.notifications.enabled.off"), () -> {
            notification.enabled = !notification.enabled;
            ConfigManager.saveServer(serverKey, serverConfig);
            init();
        }));

        y += 32;
        addDrawableChild(new DarkButton(centerX - 120, y, 240, 22, Text.translatable("multichatwindows.notifications.trigger"), () ->
                MinecraftClient.getInstance().setScreen(new NotificationBasicsScreen(this, serverKey, notificationIndex))
        ));

        y += 32;
        addDrawableChild(new DarkButton(centerX - 120, y, 240, 22, Text.translatable("multichatwindows.notifications.screens"), () ->
                MinecraftClient.getInstance().setScreen(new ScreenSelectionScreen(this, serverKey, notificationIndex))
        ));

        

        addBackButton();
    }

    private void addBackButton() {
        addDrawableChild(new DarkButton(width / 2 - 120, height - 28, 240, 20, Text.translatable("multichatwindows.back"), () ->
                MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.edit.title"), width / 2, 14, 0xFFFFFFFF);
    }
}
