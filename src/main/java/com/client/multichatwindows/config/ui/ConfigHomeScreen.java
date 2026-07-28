package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.GlobalConfig;
import com.client.multichatwindows.config.model.Language;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigHomeScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private GlobalConfig config;
    private Component importExportStatus = Component.empty();

    public ConfigHomeScreen(Screen parent) {
        super(Component.translatable("multichatwindows.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ConfigManager.init();
        config = ConfigManager.global();
        clearWidgets();

        addRenderableWidget(new DarkButton(8, 32, 90, 20, t("config_export"), () -> {
            String selected = chooseConfigFile(false);
            if (selected == null || selected.isBlank()) {
                importExportStatus = t("export_cancelled");
                return;
            }

            String path = ConfigManager.exportConfig(Path.of(selected));
            importExportStatus = path == null || path.isBlank()
                    ? t("export_failed")
                    : t("export_success", path);
        }));

        addRenderableWidget(new DarkButton(104, 32, 90, 20, t("config_import"), () -> {
            String selected = chooseConfigFile(true);
            if (selected == null || selected.isBlank()) {
                importExportStatus = t("import_cancelled");
                return;
            }

            String path = ConfigManager.importConfig(Path.of(selected));
            if (path == null || path.isBlank()) {
                importExportStatus = t("import_failed");
                return;
            }
            config = ConfigManager.global();
            com.client.multichatwindows.hud.WindowService.rebuildForCurrentServer();
            importExportStatus = t("import_success", path);
            init();
        }));

        int centerX = width / 2;
        int y = 62;

        addRenderableWidget(new DarkButton(centerX - 110, y, 220, 20, t(config.enabled ? "enabled_on" : "enabled_off"), () -> {
            boolean wasEnabled = config.enabled;
            config.enabled = !config.enabled;
            ConfigManager.saveGlobal();
            if (!wasEnabled && config.enabled) {
                com.client.multichatwindows.hud.WindowService.importDisabledBacklogAndClearVanillaChat();
            }
            init();
        }));
        y += 26;

        addRenderableWidget(new DarkButton(centerX - 110, y, 220, 20, t("language_current", languageName(config.language)), () -> {
            config.language = config.language == Language.EN_US ? Language.DE_DE : Language.EN_US;
            ConfigManager.saveGlobal();
            importExportStatus = Component.empty();
            init();
        }));
        y += 26;

        addRenderableWidget(new DarkButton(centerX - 110, y, 220, 20, t(config.autoAddServer ? "auto_add_server_on" : "auto_add_server_off"), () -> {
            config.autoAddServer = !config.autoAddServer;
            ConfigManager.saveGlobal();
            init();
        }));
        y += 26;

        addRenderableWidget(new DarkButton(centerX - 110, y, 220, 20, t("servers_open"), () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(new ServersScreen(this))));
        y += 26;

        addRenderableWidget(new DarkButton(centerX - 110, y, 220, 20, t(config.debug ? "debug_on" : "debug_off"), () -> {
            config.debug = !config.debug;
            ConfigManager.saveGlobal();
            init();
        }));
        y += 26;

        addRenderableWidget(new DarkButton(centerX - 110, y, 220, 20, t(config.chatHistoryEnabled ? "chat_history_on" : "chat_history_off"), () -> {
            config.chatHistoryEnabled = !config.chatHistoryEnabled;
            ConfigManager.saveGlobal();
            com.client.multichatwindows.hud.WindowService.rebuildForCurrentServer();
            init();
        }));
        y += 26;

        addRenderableWidget(new DarkButton(centerX - 110, y, 220, 20, t("config_background_open"), () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(new ConfigBackgroundScreen(this))));
        addRenderableWidget(new DarkButton(centerX - 110, height - 28, 220, 20, t("back"), () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent)));
    }

    private String chooseConfigFile(boolean importMode) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.pointers(stack.UTF8("*.json"));
            String fallback = ConfigManager.exportFile().toAbsolutePath().normalize().toString();

            String selected = importMode
                    ? TinyFileDialogs.tinyfd_openFileDialog(
                            "MultiChatWindows Config importieren",
                            fallback,
                            filters,
                            "JSON Config (*.json)",
                            false
                    )
                    : TinyFileDialogs.tinyfd_saveFileDialog(
                            "MultiChatWindows Config exportieren",
                            fallback,
                            filters,
                            "JSON Config (*.json)"
                    );

            if (selected == null || selected.isBlank()) {
                return "";
            }

            if (!importMode && !selected.toLowerCase(Locale.ROOT).endsWith(".json")) {
                selected = selected + ".json";
            }

            return Path.of(selected).toAbsolutePath().normalize().toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    

    private Component t(String key, Object... args) {
        return Component.literal(format(message(key), args));
    }

    private String message(String key) {
        boolean de = config != null && config.language == Language.DE_DE;
        return switch (key) {
            case "title" -> "Multi Chat Windows";
            case "config_subtitle" -> de ? "Einstellungen" : "Settings";
            case "config_export" -> "Config Export";
            case "config_import" -> "Config Import";
            case "enabled_on" -> de ? "Aktiv: AN" : "Enabled: ON";
            case "enabled_off" -> de ? "Aktiv: AUS" : "Enabled: OFF";
            case "language_current" -> de ? "Sprache: %s" : "Language: %s";
            case "auto_add_server_on" -> de ? "Server automatisch hinzufügen: AN" : "Auto add server: ON";
            case "auto_add_server_off" -> de ? "Server automatisch hinzufügen: AUS" : "Auto add server: OFF";
            case "servers_open" -> de ? "Server" : "Servers";
            case "debug_on" -> de ? "Debug: AN" : "Debug: ON";
            case "debug_off" -> de ? "Debug: AUS" : "Debug: OFF";
            case "chat_history_on" -> de ? "Chat-History: AN" : "Chat history: ON";
            case "chat_history_off" -> de ? "Chat-History: AUS" : "Chat history: OFF";
            case "config_background_open" -> de ? "Config-Hintergrund" : "Config background";
            case "back" -> de ? "Zurück" : "Back";
            case "export_cancelled" -> de ? "Export abgebrochen" : "Export cancelled";
            case "export_failed" -> de ? "Config-Export fehlgeschlagen" : "Config export failed";
            case "export_success" -> de ? "Exportiert: %s" : "Exported: %s";
            case "import_cancelled" -> de ? "Import abgebrochen" : "Import cancelled";
            case "import_failed" -> de ? "Import fehlgeschlagen: Datei fehlt oder ist ungültig" : "Import failed: file missing or invalid";
            case "import_success" -> de ? "Importiert: %s" : "Imported: %s";
            default -> key;
        };
    }

    private String format(String template, Object... args) {
        try {
            return String.format(template, args);
        } catch (Throwable ignored) {
            return template;
        }
    }

    private String languageName(Language language) {
        return language == Language.DE_DE ? "Deutsch" : "Englisch";
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(context, font, t("title"), width / 2, 14, 0xFFFFFFFF);
        com.client.multichatwindows.util.GuiDrawHelper.centered(context, font, t("config_subtitle"), width / 2, 28, 0xFFB0B0B0);
        if (importExportStatus != null && !importExportStatus.getString().isBlank()) {
            com.client.multichatwindows.util.GuiDrawHelper.text(context, font, importExportStatus, 8, 56, 0xFFB0FFB0);
        }
    }
}
