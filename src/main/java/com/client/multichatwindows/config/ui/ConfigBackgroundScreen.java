package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.GlobalConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigBackgroundScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private GlobalConfig config;
    private EditBox pathField;
    private EditBox opacityField;

    public ConfigBackgroundScreen(Screen parent) {
        super(Component.translatable("multichatwindows.config_background.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        ConfigManager.init();
        config = ConfigManager.global();
        int centerX = width / 2;
        int y = 54;

        addRenderableWidget(new DarkButton(centerX - 130, y, 260, 20, Component.translatable(config.configBackgroundEnabled ? "multichatwindows.config_background.on" : "multichatwindows.config_background.off"), () -> {
            config.configBackgroundEnabled = !config.configBackgroundEnabled;
            saveAndInvalidate();
            init();
        }));
        y += 30;

        pathField = new EditBox(font, centerX - 160, y, 320, 22, Component.translatable("multichatwindows.config_background.path"));
        pathField.setMaxLength(2048);
        pathField.setValue(config.configBackgroundPath == null ? "" : config.configBackgroundPath);
        pathField.setResponder(value -> {
            config.configBackgroundPath = ConfigManager.normalizeUserPathText(value);
            saveAndInvalidate();
        });
        addRenderableWidget(pathField);
        y += 30;

        opacityField = new EditBox(font, centerX - 160, y, 150, 22, Component.translatable("multichatwindows.config_background.opacity"));
        opacityField.setValue(trimFloat(config.configBackgroundOpacity));
        opacityField.setResponder(value -> {
            config.configBackgroundOpacity = clamp(parseFloat(value, config.configBackgroundOpacity), 0.0f, 1.0f);
            saveAndInvalidate();
        });
        addRenderableWidget(opacityField);

        addRenderableWidget(new DarkButton(centerX + 10, y, 150, 22, Component.translatable("multichatwindows.config_background.clear"), () -> {
            config.configBackgroundPath = "";
            saveAndInvalidate();
            init();
        }));

        addRenderableWidget(new DarkButton(centerX - 130, height - 28, 260, 20, Component.translatable("multichatwindows.back"), () -> {
            applyFields();
            com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent);
        }));
    }


    private void applyFields() {
        if (config == null) {
            return;
        }
        if (pathField != null) {
            config.configBackgroundPath = ConfigManager.normalizeUserPathText(pathField.getValue());
        }
        if (opacityField != null) {
            config.configBackgroundOpacity = clamp(parseFloat(opacityField.getValue(), config.configBackgroundOpacity), 0.0f, 1.0f);
        }
        saveAndInvalidate();
    }

    private void saveAndInvalidate() {
        ConfigManager.saveGlobal();
        ConfigBackgroundRenderer.invalidate();
    }

    private float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim().replace(',', '.'));
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String trimFloat(float value) {
        String text = Float.toString(value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    @Override
    public void removed() {
        applyFields();
        super.removed();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(context, font, Component.translatable("multichatwindows.config_background.title"), width / 2, 14, 0xFFFFFFFF);
        com.client.multichatwindows.util.GuiDrawHelper.centered(context, font, Component.translatable("multichatwindows.config_background.hint"), width / 2, 30, 0xFFB0B0B0);
    }
}
