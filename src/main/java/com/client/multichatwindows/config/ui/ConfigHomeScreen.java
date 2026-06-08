package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.GlobalConfig;
import com.client.multichatwindows.config.model.Language;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigHomeScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private GlobalConfig config;

    public ConfigHomeScreen(Screen parent) {
        super(Text.translatable("multichatwindows.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ConfigManager.init();
        config = ConfigManager.global();
        clearChildren();
        int centerX = width / 2;
        int y = 46;

        addDrawableChild(new DarkButton(centerX - 110, y, 220, 20, Text.translatable(config.enabled ? "multichatwindows.enabled.on" : "multichatwindows.enabled.off"), () -> {
            boolean wasEnabled = config.enabled;
            config.enabled = !config.enabled;
            ConfigManager.saveGlobal();
            if (!wasEnabled && config.enabled) {
                com.client.multichatwindows.hud.WindowService.importDisabledBacklogAndClearVanillaChat();
            }
            init();
        }));
        y += 26;

        addDrawableChild(new DarkButton(centerX - 110, y, 220, 20, Text.translatable("multichatwindows.language.current", Text.translatable(langKey(config.language))), () -> {
            config.language = config.language == Language.EN_US ? Language.DE_DE : Language.EN_US;
            ConfigManager.saveGlobal();
            init();
        }));
        y += 26;

        addDrawableChild(new DarkButton(centerX - 110, y, 220, 20, Text.translatable(config.autoAddServer ? "multichatwindows.auto_add_server.on" : "multichatwindows.auto_add_server.off"), () -> {
            config.autoAddServer = !config.autoAddServer;
            ConfigManager.saveGlobal();
            init();
        }));
        y += 26;

        addDrawableChild(new DarkButton(centerX - 110, y, 220, 20, Text.translatable("multichatwindows.servers.open"), () -> MinecraftClient.getInstance().setScreen(new ServersScreen(this))));
        y += 26;

        addDrawableChild(new DarkButton(centerX - 110, y, 220, 20, Text.translatable(config.debug ? "multichatwindows.debug.on" : "multichatwindows.debug.off"), () -> {
            config.debug = !config.debug;
            ConfigManager.saveGlobal();
            init();
        }));
        y += 26;

        addDrawableChild(new DarkButton(centerX - 110, y, 220, 20, Text.translatable("multichatwindows.config_background.open"), () -> MinecraftClient.getInstance().setScreen(new ConfigBackgroundScreen(this))));
        addDrawableChild(new DarkButton(centerX - 110, height - 28, 220, 20, Text.translatable("multichatwindows.back"), () -> MinecraftClient.getInstance().setScreen(parent)));
    }

    private String langKey(Language language) {
        return language == Language.DE_DE ? "multichatwindows.language.de_de" : "multichatwindows.language.en_us";
    }

    @Override
    public void removed() {
        if (config != null) {
            ConfigManager.saveGlobal();
        }
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.title"), width / 2, 14, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.config.subtitle"), width / 2, 28, 0xFFB0B0B0);
    }
}
