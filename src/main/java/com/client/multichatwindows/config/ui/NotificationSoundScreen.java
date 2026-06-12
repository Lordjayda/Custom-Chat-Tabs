package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.notification.NotificationSoundPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class NotificationSoundScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int notificationIndex;
    private ServerConfig serverConfig;
    private NotificationConfig notification;
    private TextFieldWidget pathField;
    private TextFieldWidget volumeField;
    private TextFieldWidget pitchField;

    public NotificationSoundScreen(Screen parent, String serverKey, int notificationIndex) {
        super(Text.translatable("multichatwindows.notifications.sound"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.notificationIndex = notificationIndex;
    }

    @Override
    protected void init() {
        clearChildren();
        serverConfig = ConfigManager.getOrCreateServer(serverKey);
        if (notificationIndex < 0 || notificationIndex >= serverConfig.notifications.size()) {
            addBackButton();
            return;
        }
        notification = serverConfig.notifications.get(notificationIndex);
        int centerX = width / 2;
        int y = 54;
        addDrawableChild(new DarkButton(centerX - 130, y, 260, 20, Text.translatable(notification.soundEnabled ? "multichatwindows.notifications.sound.on" : "multichatwindows.notifications.sound.off"), () -> {
            notification.soundEnabled = !notification.soundEnabled;
            save();
            init();
        }));
        y += 30;
        pathField = new TextFieldWidget(textRenderer, centerX - 130, y, 260, 22, Text.translatable("multichatwindows.notifications.custom_sound_path"));
        pathField.setMaxLength(2048);
        pathField.setText(notification.customSoundPath == null ? "" : notification.customSoundPath);
        pathField.setChangedListener(value -> {
            notification.customSoundPath = ConfigManager.normalizeUserPathText(value);
            if (!notification.customSoundPath.isBlank()) {
                notification.soundEnabled = true;
            }
            save();
        });
        addDrawableChild(pathField);
        y += 30;
        volumeField = addFloatField(centerX - 130, y, 125, "multichatwindows.notifications.volume", notification.volume, value -> {
            notification.volume = clamp(parseFloat(value, notification.volume), 0.0f, 2.0f);
            save();
        });
        pitchField = addFloatField(centerX + 5, y, 125, "multichatwindows.notifications.pitch", notification.pitch, value -> {
            notification.pitch = clamp(parseFloat(value, notification.pitch), 0.5f, 2.0f);
            save();
        });
        y += 34;
        addDrawableChild(new DarkButton(centerX - 130, y, 260, 20, Text.translatable("multichatwindows.notifications.sound.test"), () -> {
            applyFields();
            NotificationSoundPlayer.playTest(notification);
        }));
        addBackButton();
    }

    private TextFieldWidget addFloatField(int x, int y, int width, String key, float value, java.util.function.Consumer<String> onChanged) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, 22, Text.translatable(key));
        field.setText(trimFloat(value));
        field.setChangedListener(onChanged);
        addDrawableChild(field);
        return field;
    }

    private void addBackButton() {
        addDrawableChild(new DarkButton(width / 2 - 130, height - 28, 260, 20, Text.translatable("multichatwindows.back"), () -> {
            applyFields();
            MinecraftClient.getInstance().setScreen(parent);
        }));
    }

    private void applyFields() {
        if (notification == null) return;
        if (pathField != null) notification.customSoundPath = ConfigManager.normalizeUserPathText(pathField.getText());
        if (volumeField != null) notification.volume = clamp(parseFloat(volumeField.getText(), notification.volume), 0.0f, 2.0f);
        if (pitchField != null) notification.pitch = clamp(parseFloat(pitchField.getText(), notification.pitch), 0.5f, 2.0f);
        save();
    }

    private void save() {
        if (serverConfig != null) ConfigManager.saveServer(serverKey, serverConfig);
    }

    private float parseFloat(String value, float fallback) {
        try { return Float.parseFloat(value.trim().replace(',', '.')); } catch (Throwable ignored) { return fallback; }
    }
    private String trimFloat(float value) { String text = Float.toString(value); return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text; }
    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    @Override
    public void removed() {
        applyFields();
        super.removed();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.sound"), width / 2, 14, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.custom_sound_hint"), width / 2, 30, 0xFFB0B0B0);
    }
}
