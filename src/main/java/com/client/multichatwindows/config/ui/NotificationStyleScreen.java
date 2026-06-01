package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class NotificationStyleScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private TextFieldWidget scaleField;

    public NotificationStyleScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.notifications.style"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        int centerX = width / 2;

        scaleField = new TextFieldWidget(textRenderer, centerX - 130, 54, 260, 22, Text.translatable("multichatwindows.notifications.text_scale"));
        scaleField.setText(String.valueOf(serverConfig.notificationTextScale));
        scaleField.setChangedListener(value -> {
            apply(serverConfig);
            ConfigManager.saveServer(serverKey, serverConfig);
        });
        addDrawableChild(scaleField);

        addDrawableChild(new DarkButton(centerX - 130, height - 54, 260, 20, Text.translatable("multichatwindows.save"), () -> {
            apply(serverConfig);
            ConfigManager.saveServer(serverKey, serverConfig);
            MinecraftClient.getInstance().setScreen(parent);
        }));

        addDrawableChild(new DarkButton(centerX - 130, height - 28, 260, 20, Text.translatable("multichatwindows.back"), () -> MinecraftClient.getInstance().setScreen(parent)));
    }

    private void apply(ServerConfig serverConfig) {
        try { serverConfig.notificationTextScale = Float.parseFloat(scaleField.getText()); } catch (Exception ignored) {}
        serverConfig.notificationTextScale = Math.max(0.5f, Math.min(3.0f, serverConfig.notificationTextScale));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.style"), width / 2, 14, 0xFFFFFFFF);
        renderPreview(ctx, ConfigManager.getOrCreateServer(serverKey));
        TooltipHelper.renderIfHovered(ctx, textRenderer, mouseX, mouseY, scaleField.getX(), scaleField.getY(), 260, 22, Text.translatable("multichatwindows.tooltip.notification.text_scale"));
    }

    private void renderPreview(DrawContext ctx, ServerConfig serverConfig) {
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

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.preview.title"), (int)((previewX + 6) / scale), (int)((previewY + 6) / scale), 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.preview.style"), (int)((previewX + 6) / scale), (int)((previewY + 20 * scale) / scale), 0xFFDDDDDD);
        ctx.getMatrices().popMatrix();
    }
}
