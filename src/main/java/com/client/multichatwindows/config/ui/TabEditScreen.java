package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TabEditScreen extends ScrollableDarkScreen {

    private final Screen parent;
    private final String serverKey;
    private final int tabIndex;

    private ServerConfig sc;
    private TabConfig tab;
    private TextFieldWidget nameField;

    public TabEditScreen(Screen parent, String serverKey, int tabIndex) {
        super(Text.translatable("multichatwindows.tab.edit.title"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.tabIndex = tabIndex;
    }

    @Override
    protected void init() {
        clearChildren();

        sc = ConfigManager.getOrCreateServer(serverKey);
        tab = (tabIndex >= 0 && tabIndex < sc.tabs.size()) ? sc.tabs.get(tabIndex) : null;

        int cx = width / 2;
        int y = 46;

        if (tab == null) {
            addDrawableChild(new DarkButton(
                    cx - 100,
                    height - 28,
                    200,
                    20,
                    Text.translatable("multichatwindows.back"),
                    () -> MinecraftClient.getInstance().setScreen(parent)
            ));
            return;
        }

        boolean isAll = "all".equalsIgnoreCase(tab.id);

        nameField = new TextFieldWidget(
                textRenderer,
                cx - 100,
                y,
                200,
                20,
                Text.translatable("multichatwindows.tab.name")
        );

        nameField.setText(tab.name);
        nameField.setEditable(!isAll);
        nameField.setChangedListener(value -> {
            if (!isAll) {
                tab.name = value == null || value.isBlank() ? Text.translatable("multichatwindows.tab.default_name").getString() : value;
                ConfigManager.saveServer(serverKey, sc);
                ConfigManager.saveTab(serverKey, tab);
                WindowService.rebuildForCurrentServer();
            }
        });
        addDrawableChild(nameField);

        y += 26;

        addDrawableChild(new DarkButton(
                cx - 100,
                y,
                200,
                20,
                Text.translatable(tab.enabled
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

        addDrawableChild(new DarkButton(
                cx - 100,
                y,
                200,
                20,
                Text.translatable("multichatwindows.style.open"),
                () -> MinecraftClient.getInstance().setScreen(new StyleMenuScreen(this, serverKey, tabIndex))
        ));

        y += 26;

        if (!isAll) {
            addDrawableChild(new DarkButton(
                    cx - 100,
                    y,
                    200,
                    20,
                    Text.translatable(tab.filterAllChat
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

            addDrawableChild(new DarkButton(
                    cx - 100,
                    y,
                    200,
                    20,
                    Text.translatable("multichatwindows.dependencies.open"),
                    () -> MinecraftClient.getInstance().setScreen(new DependenciesScreen(this, serverKey, tabIndex))
            ));
        }

        

        addDrawableChild(new DarkButton(
                cx - 100,
                height - 28,
                200,
                20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.tab.edit.title"),
                width / 2,
                14,
                0xFFFFFFFF
        );

        if (tab != null && "all".equalsIgnoreCase(tab.id)) {
            ctx.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.translatable("multichatwindows.all_tab.locked"),
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
        String value = nameField.getText();
        tab.name = value == null || value.isBlank() ? Text.translatable("multichatwindows.tab.default_name").getString() : value;
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