package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.GlobalConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ConfigBackgroundScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private GlobalConfig config;
    private TextFieldWidget pathField;
    private TextFieldWidget opacityField;

    public ConfigBackgroundScreen(Screen parent) {
        super(Text.translatable("multichatwindows.config_background.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        ConfigManager.init();
        config = ConfigManager.global();
        int centerX = width / 2;
        int y = 54;

        addDrawableChild(new DarkButton(centerX - 130, y, 260, 20, Text.translatable(config.configBackgroundEnabled ? "multichatwindows.config_background.on" : "multichatwindows.config_background.off"), () -> {
            config.configBackgroundEnabled = !config.configBackgroundEnabled;
            saveAndInvalidate();
            init();
        }));
        y += 30;

        pathField = new TextFieldWidget(textRenderer, centerX - 160, y, 320, 22, Text.translatable("multichatwindows.config_background.path"));
        pathField.setMaxLength(2048);
        pathField.setText(config.configBackgroundPath == null ? "" : config.configBackgroundPath);
        pathField.setChangedListener(value -> {
            config.configBackgroundPath = ConfigManager.normalizeUserPathText(value);
            saveAndInvalidate();
        });
        addDrawableChild(pathField);
        y += 30;

        opacityField = new TextFieldWidget(textRenderer, centerX - 160, y, 150, 22, Text.translatable("multichatwindows.config_background.opacity"));
        opacityField.setText(trimFloat(config.configBackgroundOpacity));
        opacityField.setChangedListener(value -> {
            config.configBackgroundOpacity = clamp(parseFloat(value, config.configBackgroundOpacity), 0.0f, 1.0f);
            saveAndInvalidate();
        });
        addDrawableChild(opacityField);

        addDrawableChild(new DarkButton(centerX + 10, y, 150, 22, Text.translatable("multichatwindows.config_background.clear"), () -> {
            config.configBackgroundPath = "";
            saveAndInvalidate();
            init();
        }));

        addDrawableChild(new DarkButton(centerX - 130, height - 28, 260, 20, Text.translatable("multichatwindows.back"), () -> {
            applyFields();
            MinecraftClient.getInstance().setScreen(parent);
        }));
    }


    private void applyFields() {
        if (config == null) {
            return;
        }
        if (pathField != null) {
            config.configBackgroundPath = ConfigManager.normalizeUserPathText(pathField.getText());
        }
        if (opacityField != null) {
            config.configBackgroundOpacity = clamp(parseFloat(opacityField.getText(), config.configBackgroundOpacity), 0.0f, 1.0f);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.config_background.title"), width / 2, 14, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.config_background.hint"), width / 2, 30, 0xFFB0B0B0);
    }
}
