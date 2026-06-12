package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class NotificationPositionStyleScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    public NotificationPositionStyleScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.position_style"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();
        int centerX = width / 2;
        int y = 54;

        addDrawableChild(new DarkButton(centerX - 130, y, 260, 22, Text.translatable("multichatwindows.position"), () ->
                MinecraftClient.getInstance().setScreen(new NotificationPositionScreen(this, serverKey))
        ));

        y += 32;
        addDrawableChild(new DarkButton(centerX - 130, y, 260, 22, Text.translatable("multichatwindows.style"), () ->
                MinecraftClient.getInstance().setScreen(new NotificationStyleScreen(this, serverKey))
        ));

        y += 32;
        addDrawableChild(new DarkButton(centerX - 130, y, 260, 22, Text.translatable("multichatwindows.reset"), () -> {
            ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
            serverConfig.notificationX = -1;
            serverConfig.notificationY = 12;
            serverConfig.notificationWidth = 220;
            serverConfig.notificationTextScale = 1.0f;
            serverConfig.notificationMaxVisible = 5;
            ConfigManager.saveServer(serverKey, serverConfig);
        }));

        addDrawableChild(new DarkButton(centerX - 130, height - 28, 260, 20, Text.translatable("multichatwindows.back"), () ->
                MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.position_style"), width / 2, 14, 0xFFFFFFFF);
    }
}
