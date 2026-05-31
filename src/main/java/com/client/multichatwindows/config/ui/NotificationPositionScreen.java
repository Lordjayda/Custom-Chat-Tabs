package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class NotificationPositionScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget widthField;
    private TextFieldWidget maxVisibleField;

    public NotificationPositionScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.notifications.position"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        int centerX = width / 2;
        int y = 54;

        addDrawableChild(new DarkButton(centerX - 130, y, 260, 22, Text.translatable(serverConfig.notificationX < 0 ? "multichatwindows.notifications.default_position.on" : "multichatwindows.notifications.default_position.off"), () -> {
            if (serverConfig.notificationX < 0) {
                serverConfig.notificationX = Math.max(0, width - serverConfig.notificationWidth - 12);
                serverConfig.notificationY = 12;
            } else {
                serverConfig.notificationX = -1;
                serverConfig.notificationY = 12;
            }
            ConfigManager.saveServer(serverKey, serverConfig);
            init();
        }));

        y += 34;
        xField = addField(centerX - 130, y, 125, "X", String.valueOf(serverConfig.notificationX));
        yField = addField(centerX + 5, y, 125, "Y", String.valueOf(serverConfig.notificationY));

        y += 34;
        widthField = addField(centerX - 130, y, 260, Text.translatable("multichatwindows.width").getString(), String.valueOf(serverConfig.notificationWidth));

        y += 34;
        maxVisibleField = addField(centerX - 130, y, 260, Text.translatable("multichatwindows.notifications.max_visible").getString(), String.valueOf(serverConfig.notificationMaxVisible));

        addChangedListeners(serverConfig);

        addDrawableChild(new DarkButton(centerX - 130, height - 54, 260, 20, Text.translatable("multichatwindows.save"), () -> {
            apply(serverConfig);
            ConfigManager.saveServer(serverKey, serverConfig);
            MinecraftClient.getInstance().setScreen(parent);
        }));

        addDrawableChild(new DarkButton(centerX - 130, height - 28, 260, 20, Text.translatable("multichatwindows.back"), () -> MinecraftClient.getInstance().setScreen(parent)));
    }

    private TextFieldWidget addField(int x, int y, int w, String label, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, w, 22, Text.literal(label));
        field.setText(value);
        addDrawableChild(field);
        return field;
    }

    private void addChangedListeners(ServerConfig serverConfig) {
        xField.setChangedListener(value -> liveApply(serverConfig));
        yField.setChangedListener(value -> liveApply(serverConfig));
        widthField.setChangedListener(value -> liveApply(serverConfig));
        maxVisibleField.setChangedListener(value -> liveApply(serverConfig));
    }

    private void liveApply(ServerConfig serverConfig) {
        apply(serverConfig);
        ConfigManager.saveServer(serverKey, serverConfig);
    }

    private void apply(ServerConfig serverConfig) {
        try { serverConfig.notificationX = Integer.parseInt(xField.getText()); } catch (Exception ignored) {}
        try { serverConfig.notificationY = Integer.parseInt(yField.getText()); } catch (Exception ignored) {}
        try { serverConfig.notificationWidth = Integer.parseInt(widthField.getText()); } catch (Exception ignored) {}
        try { serverConfig.notificationMaxVisible = Integer.parseInt(maxVisibleField.getText()); } catch (Exception ignored) {}
        serverConfig.notificationWidth = Math.max(120, Math.min(500, serverConfig.notificationWidth));
        serverConfig.notificationMaxVisible = Math.max(1, Math.min(10, serverConfig.notificationMaxVisible));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.position"), width / 2, 14, 0xFFFFFFFF);
        renderPreview(ctx, ConfigManager.getOrCreateServer(serverKey));

        TooltipHelper.renderIfHovered(ctx, textRenderer, mouseX, mouseY, xField.getX(), xField.getY(), 125, 22, Text.translatable("multichatwindows.tooltip.notification.x"));
        TooltipHelper.renderIfHovered(ctx, textRenderer, mouseX, mouseY, yField.getX(), yField.getY(), 125, 22, Text.translatable("multichatwindows.tooltip.notification.y"));
        TooltipHelper.renderIfHovered(ctx, textRenderer, mouseX, mouseY, widthField.getX(), widthField.getY(), 260, 22, Text.translatable("multichatwindows.tooltip.notification.width"));
        TooltipHelper.renderIfHovered(ctx, textRenderer, mouseX, mouseY, maxVisibleField.getX(), maxVisibleField.getY(), 260, 22, Text.translatable("multichatwindows.tooltip.notification.max_visible"));
    }

    private void renderPreview(DrawContext ctx, ServerConfig serverConfig) {
        int previewWidth = Math.max(120, Math.min(500, serverConfig.notificationWidth));
        int previewX = serverConfig.notificationX < 0 ? width - previewWidth - 12 : serverConfig.notificationX;
        int previewY = Math.max(0, serverConfig.notificationY);
        int previewHeight = 42;

        previewX = Math.max(0, Math.min(width - previewWidth, previewX));
        previewY = Math.max(0, Math.min(height - previewHeight, previewY));

        ctx.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0xCC101010);
        ctx.fill(previewX, previewY, previewX + previewWidth, previewY + 1, 0xFFFFFFFF);
        ctx.fill(previewX, previewY + previewHeight - 1, previewX + previewWidth, previewY + previewHeight, 0xFFFFFFFF);
        ctx.fill(previewX, previewY, previewX + 1, previewY + previewHeight, 0xFFFFFFFF);
        ctx.fill(previewX + previewWidth - 1, previewY, previewX + previewWidth, previewY + previewHeight, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.preview.title"), previewX + 6, previewY + 6, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.preview.position"), previewX + 6, previewY + 20, 0xFFDDDDDD);
    }
}
