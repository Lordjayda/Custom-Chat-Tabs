package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.notification.NotificationSoundPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotificationSoundScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int notificationIndex;
    private ServerConfig serverConfig;
    private NotificationConfig notification;
    private EditBox pathField;
    private EditBox volumeField;
    private EditBox pitchField;

    public NotificationSoundScreen(Screen parent, String serverKey, int notificationIndex) {
        super(Component.translatable("multichatwindows.notifications.sound"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.notificationIndex = notificationIndex;
    }

    @Override
    protected void init() {
        clearWidgets();
        serverConfig = ConfigManager.getOrCreateServer(serverKey);
        if (notificationIndex < 0 || notificationIndex >= serverConfig.notifications.size()) {
            addBackButton();
            return;
        }
        notification = serverConfig.notifications.get(notificationIndex);
        int centerX = width / 2;
        int y = 54;
        addRenderableWidget(new DarkButton(centerX - 130, y, 260, 20, Component.translatable(notification.soundEnabled ? "multichatwindows.notifications.sound.on" : "multichatwindows.notifications.sound.off"), () -> {
            notification.soundEnabled = !notification.soundEnabled;
            save();
            init();
        }));
        y += 30;
        pathField = new EditBox(font, centerX - 130, y, 260, 22, Component.translatable("multichatwindows.notifications.custom_sound_path"));
        pathField.setMaxLength(2048);
        pathField.setValue(notification.customSoundPath == null ? "" : notification.customSoundPath);
        pathField.setResponder(value -> {
            notification.customSoundPath = ConfigManager.normalizeUserPathText(value);
            if (!notification.customSoundPath.isBlank()) {
                notification.soundEnabled = true;
            }
            save();
        });
        addRenderableWidget(pathField);
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
        addRenderableWidget(new DarkButton(centerX - 130, y, 260, 20, Component.translatable("multichatwindows.notifications.sound.test"), () -> {
            applyFields();
            NotificationSoundPlayer.playTest(notification);
        }));
        addBackButton();
    }

    private EditBox addFloatField(int x, int y, int width, String key, float value, java.util.function.Consumer<String> onChanged) {
        EditBox field = new EditBox(font, x, y, width, 22, Component.translatable(key));
        field.setValue(trimFloat(value));
        field.setResponder(onChanged);
        addRenderableWidget(field);
        return field;
    }

    private void addBackButton() {
        addRenderableWidget(new DarkButton(width / 2 - 130, height - 28, 260, 20, Component.translatable("multichatwindows.back"), () -> {
            applyFields();
            com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent);
        }));
    }

    private void applyFields() {
        if (notification == null) return;
        if (pathField != null) notification.customSoundPath = ConfigManager.normalizeUserPathText(pathField.getValue());
        if (volumeField != null) notification.volume = clamp(parseFloat(volumeField.getValue(), notification.volume), 0.0f, 2.0f);
        if (pitchField != null) notification.pitch = clamp(parseFloat(pitchField.getValue(), notification.pitch), 0.5f, 2.0f);
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
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.notifications.sound"), width / 2, 14, 0xFFFFFFFF);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.notifications.custom_sound_hint"), width / 2, 30, 0xFFB0B0B0);
    }
}
