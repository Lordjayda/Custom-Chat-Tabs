package com.client.multichatwindows.config.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class NotificationsScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    public NotificationsScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.notifications.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();

        int centerX = width / 2;
        int y = 54;

        addDrawableChild(new DarkButton(centerX - 130, y, 260, 22, Text.translatable("multichatwindows.notifications.rules"), () ->
                MinecraftClient.getInstance().setScreen(new NotificationRulesScreen(this, serverKey))
        ));

        y += 32;

        addDrawableChild(new DarkButton(centerX - 130, y, 260, 22, Text.translatable("multichatwindows.position_style"), () ->
                MinecraftClient.getInstance().setScreen(new NotificationPositionStyleScreen(this, serverKey))
        ));

        y += 32;

        addDrawableChild(new DarkButton(centerX - 130, y, 260, 22, Text.translatable("multichatwindows.notifications.history"), () ->
                MinecraftClient.getInstance().setScreen(new NotificationHistoryScreen(this))
        ));

        addDrawableChild(new DarkButton(centerX - 130, height - 28, 260, 20, Text.translatable("multichatwindows.back"), () ->
                MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.title"), width / 2, 14, 0xFFFFFFFF);
    }
}
