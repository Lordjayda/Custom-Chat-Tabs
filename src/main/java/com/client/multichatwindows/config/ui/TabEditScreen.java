package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TabEditScreen extends ScrollableDarkScreen {

    private final Screen parent;
    private final String serverKey;
    private final int tabIndex;

    private ServerConfig sc;
    private TabConfig tab;
    private EditBox nameField;

    public TabEditScreen(Screen parent, String serverKey, int tabIndex) {
        super(Component.translatable("multichatwindows.tab.edit.title"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.tabIndex = tabIndex;
    }

    @Override
    protected void init() {
        clearWidgets();

        sc = ConfigManager.getOrCreateServer(serverKey);
        tab = (tabIndex >= 0 && tabIndex < sc.tabs.size()) ? sc.tabs.get(tabIndex) : null;

        int cx = width / 2;
        int y = 46;

        if (tab == null) {
            addRenderableWidget(new DarkButton(
                    cx - 100,
                    height - 28,
                    200,
                    20,
                    Component.translatable("multichatwindows.back"),
                    () -> Minecraft.getInstance().setScreen(parent)
            ));
            return;
        }

        boolean isAll = "all".equalsIgnoreCase(tab.id);

        nameField = new EditBox(
                font,
                cx - 100,
                y,
                200,
                20,
                Component.translatable("multichatwindows.tab.name")
        );

        nameField.setValue(tab.name);
        nameField.setEditable(!isAll);
        nameField.setResponder(value -> {
            if (!isAll) {
                tab.name = value == null || value.isBlank() ? Component.translatable("multichatwindows.tab.default_name").getString() : value;
                ConfigManager.saveServer(serverKey, sc);
                ConfigManager.saveTab(serverKey, tab);
                WindowService.rebuildForCurrentServer();
            }
        });
        addRenderableWidget(nameField);

        y += 26;

        addRenderableWidget(new DarkButton(
                cx - 100,
                y,
                200,
                20,
                Component.translatable(tab.enabled
                        ? "multichatwindows.enabled.on"
                        : "multichatwindows.enabled.off"),
                () -> {
                    tab.enabled = !tab.enabled;

                    ConfigManager.saveServer(serverKey, sc);
                    ConfigManager.saveTab(serverKey, tab);

                    WindowService.rebuildForCurrentServer();

                    init();
                }
        ));

        y += 26;

        addRenderableWidget(new DarkButton(
                cx - 100,
                y,
                200,
                20,
                Component.translatable("multichatwindows.style.open"),
                () -> Minecraft.getInstance().setScreen(new StyleMenuScreen(this, serverKey, tabIndex))
        ));

        y += 26;

        if (!isAll) {
            addRenderableWidget(new DarkButton(
                    cx - 100,
                    y,
                    200,
                    20,
                    Component.translatable(tab.filterAllChat
                            ? "multichatwindows.filter_all.on"
                            : "multichatwindows.filter_all.off"),
                    () -> {
                        tab.filterAllChat = !tab.filterAllChat;

                        ConfigManager.saveServer(serverKey, sc);
                        ConfigManager.saveTab(serverKey, tab);

                        init();
                    }
            ));

            y += 26;

            addRenderableWidget(new DarkButton(
                    cx - 100,
                    y,
                    200,
                    20,
                    Component.translatable("multichatwindows.dependencies.open"),
                    () -> Minecraft.getInstance().setScreen(new DependenciesScreen(this, serverKey, tabIndex))
            ));
        }

        

        addRenderableWidget(new DarkButton(
                cx - 100,
                height - 28,
                200,
                20,
                Component.translatable("multichatwindows.back"),
                () -> Minecraft.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                font,
                Component.translatable("multichatwindows.tab.edit.title"),
                width / 2,
                14,
                0xFFFFFFFF
        );

        if (tab != null && "all".equalsIgnoreCase(tab.id)) {
            com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                    font,
                    Component.translatable("multichatwindows.all_tab.locked"),
                    width / 2,
                    34,
                    0xFFB0B0B0
            );
        }
    }

    private void applyNameField() {
        if (nameField == null || tab == null || sc == null || "all".equalsIgnoreCase(tab.id)) {
            return;
        }
        String value = nameField.getValue();
        tab.name = value == null || value.isBlank() ? Component.translatable("multichatwindows.tab.default_name").getString() : value;
        ConfigManager.saveServer(serverKey, sc);
        ConfigManager.saveTab(serverKey, tab);
        WindowService.rebuildForCurrentServer();
    }

    @Override
    public void removed() {
        applyNameField();
        super.removed();
    }
}