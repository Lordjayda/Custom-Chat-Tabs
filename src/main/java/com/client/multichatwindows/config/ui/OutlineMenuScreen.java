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

public class OutlineMenuScreen extends ScrollableDarkScreen {

    private final Screen parent;
    private final String serverKey;
    private final int tabIndex;

    private ServerConfig sc;
    private TabConfig tab;

    private EditBox colorField;

    public OutlineMenuScreen(Screen parent, String serverKey, int tabIndex) {
        super(Component.translatable("multichatwindows.outline.title"));
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

        addRenderableWidget(new DarkButton(
                cx - 100,
                y,
                200,
                20,
                Component.translatable(tab.outlineEnabled
                        ? "multichatwindows.outline.enabled.on"
                        : "multichatwindows.outline.enabled.off"),
                () -> {
                    tab.outlineEnabled = !tab.outlineEnabled;
                    save();
                    init();
                }
        ));

        y += 30;

        addRenderableWidget(new PixelSlider(
                cx - 100,
                y,
                160,
                20,
                1,
                20,
                Math.max(1, Math.min(20, tab.outlineWidth)),
                v -> {
                    tab.outlineWidth = v;
                    save();
                }
        ));

        y += 28;

        colorField = new EditBox(
                font,
                cx - 100,
                y,
                200,
                20,
                Component.translatable("multichatwindows.outline.color")
        );

        colorField.setValue(tab.outlineColor == null ? "FFFFFF" : tab.outlineColor);

        colorField.setResponder(s -> {
            tab.outlineColor = sanitizeHex(s);
            save();
        });

        addRenderableWidget(colorField);

        

        addRenderableWidget(new DarkButton(
                cx - 100,
                height - 28,
                200,
                20,
                Component.translatable("multichatwindows.back"),
                () -> Minecraft.getInstance().setScreen(parent)
        ));
    }

    private void save() {
        if (tab == null) return;

        tab.outlineWidth = Math.max(1, Math.min(20, tab.outlineWidth));
        tab.outlineColor = sanitizeHex(tab.outlineColor);

        ConfigManager.saveServer(serverKey, sc);
        ConfigManager.saveTab(serverKey, tab);

        WindowService.rebuildForCurrentServer();
    }

    private static String sanitizeHex(String raw) {
        if (raw == null) return "FFFFFF";

        String s = raw.trim();

        if (s.startsWith("#")) {
            s = s.substring(1);
        }

        s = s.replaceAll("[^0-9a-fA-F]", "");

        if (s.length() > 6) {
            s = s.substring(0, 6);
        }

        if (s.isEmpty()) {
            s = "FFFFFF";
        }

        while (s.length() < 6) {
            s = "0" + s;
        }

        return s.toUpperCase();
    }

    private static int parseColor(String hex) {
        try {
            return Integer.parseInt(sanitizeHex(hex), 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return 0xFFFFFF;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                font,
                Component.translatable("multichatwindows.outline.title"),
                width / 2,
                14,
                0xFFFFFFFF
        );

        if (tab == null) return;

        int cx = width / 2;

        com.client.multichatwindows.util.GuiDrawHelper.text(ctx, 
                font,
                Component.translatable("multichatwindows.outline.width", tab.outlineWidth),
                cx - 100,
                76,
                0xFFFFFFFF
        );

        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                font,
                Component.translatable("multichatwindows.outline.preview"),
                cx,
                110,
                0xFFFFFFFF
        );

        int previewX1 = cx - 60;
        int previewY1 = 126;
        int previewX2 = cx + 60;
        int previewY2 = 154;

        ctx.fill(previewX1, previewY1, previewX2, previewY2, 0xFF111111);

        int color = 0xFF000000 | parseColor(tab.outlineColor);
        int ow = Math.max(1, Math.min(20, tab.outlineWidth));

        for (int i = 0; i < ow; i++) {
            ctx.fill(previewX1 - i, previewY1 - i, previewX2 + i, previewY1 - i + 1, color);
            ctx.fill(previewX1 - i, previewY2 + i - 1, previewX2 + i, previewY2 + i, color);
            ctx.fill(previewX1 - i, previewY1 - i, previewX1 - i + 1, previewY2 + i, color);
            ctx.fill(previewX2 + i - 1, previewY1 - i, previewX2 + i, previewY2 + i, color);
        }
    }
}
