package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotificationStyleScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private EditBox scaleField;

    public NotificationStyleScreen(Screen parent, String serverKey) {
        super(Component.translatable("multichatwindows.notifications.style"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearWidgets();
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        int centerX = width / 2;

        scaleField = new EditBox(font, centerX - 130, 54, 260, 22, Component.translatable("multichatwindows.notifications.text_scale"));
        scaleField.setValue(String.valueOf(serverConfig.notificationTextScale));
        scaleField.setResponder(value -> {
            apply(serverConfig);
            ConfigManager.saveServer(serverKey, serverConfig);
        });
        addRenderableWidget(scaleField);

        

        addRenderableWidget(new DarkButton(centerX - 130, height - 28, 260, 20, Component.translatable("multichatwindows.back"), () -> Minecraft.getInstance().setScreen(parent)));
    }

    private void apply(ServerConfig serverConfig) {
        try { serverConfig.notificationTextScale = Float.parseFloat(scaleField.getValue()); } catch (Exception ignored) {}
        serverConfig.notificationTextScale = Math.max(0.5f, Math.min(3.0f, serverConfig.notificationTextScale));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.notifications.style"), width / 2, 14, 0xFFFFFFFF);
        renderPreview(ctx, ConfigManager.getOrCreateServer(serverKey));
        TooltipHelper.renderIfHovered(ctx, font, mouseX, mouseY, scaleField.getX(), scaleField.getY(), 260, 22, Component.translatable("multichatwindows.tooltip.notification.text_scale"));
    }

    private void renderPreview(GuiGraphicsExtractor ctx, ServerConfig serverConfig) {
        int previewWidth = 260;
        int previewX = width / 2 - 130;
        int previewY = 92;
        float scale = Math.max(0.5f, Math.min(3.0f, serverConfig.notificationTextScale));
        int previewHeight = Math.max(42, (int) (42 * scale));

        ctx.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0xCC101010);
        ctx.fill(previewX, previewY, previewX + previewWidth, previewY + 1, 0xFFFFFFFF);
        ctx.fill(previewX, previewY + previewHeight - 1, previewX + previewWidth, previewY + previewHeight, 0xFFFFFFFF);
        ctx.fill(previewX, previewY, previewX + 1, previewY + previewHeight, 0xFFFFFFFF);
        ctx.fill(previewX + previewWidth - 1, previewY, previewX + previewWidth, previewY + previewHeight, 0xFFFFFFFF);

        ctx.pose().pushMatrix();
        ctx.pose().scale(scale, scale);
        com.client.multichatwindows.util.GuiDrawHelper.text(ctx, font, Component.translatable("multichatwindows.notifications.preview.title"), (int)((previewX + 6) / scale), (int)((previewY + 6) / scale), 0xFFFFFFFF);
        com.client.multichatwindows.util.GuiDrawHelper.text(ctx, font, Component.translatable("multichatwindows.notifications.preview.style"), (int)((previewX + 6) / scale), (int)((previewY + 20 * scale) / scale), 0xFFDDDDDD);
        ctx.pose().popMatrix();
    }
}
