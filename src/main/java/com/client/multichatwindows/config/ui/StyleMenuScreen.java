package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StyleMenuScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int tabIndex;
    private ServerConfig sc;
    private TabConfig tab;
    private EditBox xField;
    private EditBox yField;
    private EditBox wField;
    private EditBox hField;
    private EditBox opField;
    private EditBox textScaleField;

    public StyleMenuScreen(Screen parent, String serverKey, int tabIndex) {
        super(Component.translatable("multichatwindows.style.menu"));
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
        int y = 42;

        if (tab == null) {
            addRenderableWidget(new DarkButton(
                    cx - 100,
                    height - 28,
                    200,
                    20,
                    Component.translatable("multichatwindows.back"),
                    () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent)
            ));
            return;
        }

        addRenderableWidget(new DarkButton(
                cx - 104,
                y,
                208,
                20,
                Component.translatable(tab.useVanilla
                        ? "multichatwindows.vanilla.on"
                        : "multichatwindows.vanilla.off"),
                () -> {
                    tab.useVanilla = !tab.useVanilla;
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));
        y += 28;

        if (tab.useVanilla) {
            y += 24;
        }

        int left = cx - 104;
        int right = cx + 4;

        xField = field(left, y, 100, "multichatwindows.style.x", tab.x);
        yField = field(right, y, 100, "multichatwindows.style.y", tab.y);
        y += 24;

        wField = field(left, y, 100, "multichatwindows.style.w", tab.width);
        hField = field(right, y, 100, "multichatwindows.style.h", tab.height);
        y += 24;

        opField = field(left, y, 100, "multichatwindows.style.opacity", tab.opacity);
        textScaleField = field(right, y, 100, "multichatwindows.style.text_scale", Math.round(clampTextScale(tab.textScale) * 100.0f) + "%");
        y += 28;

        addRenderableWidget(new DarkButton(
                left,
                y,
                100,
                20,
                Component.translatable("multichatwindows.outline.open"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(new OutlineMenuScreen(this, serverKey, tabIndex))
        ));
        addRenderableWidget(new DarkButton(
                right,
                y,
                100,
                20,
                Component.translatable("multichatwindows.style.edit_pos_size"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(new RealTimeEditorScreen(this, serverKey, tabIndex))
        ));
        y += 28;

        boolean editable = !tab.useVanilla;
        xField.setEditable(editable);
        yField.setEditable(editable);
        wField.setEditable(editable);
        hField.setEditable(editable);
        opField.setEditable(editable);
        textScaleField.setEditable(editable);

        
        addRenderableWidget(new DarkButton(
                cx - 104,
                height - 28,
                208,
                20,
                Component.translatable("multichatwindows.back"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent)
        ));
    }

    private EditBox field(int x, int y, int w, String key, int value) {
        return field(x, y, w, key, String.valueOf(value));
    }

    private EditBox field(int x, int y, int w, String key, float value) {
        return field(x, y, w, key, String.valueOf(value));
    }

    private EditBox field(int x, int y, int w, String key, String value) {
        EditBox tf = new EditBox(
                font,
                x,
                y,
                w,
                20,
                Component.translatable(key)
        );
        tf.setValue(value == null ? "" : value);
        addRenderableWidget(tf);
        return tf;
    }

    private void apply() {
        if (tab == null || tab.useVanilla) return;
        tab.x = parseInt(xField.getValue(), tab.x);
        tab.y = parseInt(yField.getValue(), tab.y);
        tab.width = Math.max(60, parseInt(wField.getValue(), tab.width));
        tab.height = Math.max(40, parseInt(hField.getValue(), tab.height));
        tab.opacity = Math.max(0.0f, Math.min(1.0f, parseFloat(opField.getValue(), tab.opacity)));
        tab.textScale = parseTextScale(textScaleField.getValue(), tab.textScale);
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String s, float fallback) {
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float parseTextScale(String value, float fallback) {
        if (value == null) {
            return clampTextScale(fallback);
        }

        String raw = value.trim().replace(',', '.');
        if (raw.isEmpty()) {
            return clampTextScale(fallback);
        }

        boolean percent = raw.endsWith("%");
        if (percent) {
            raw = raw.substring(0, raw.length() - 1).trim();
        }

        try {
            float parsed = Float.parseFloat(raw);

            if (percent || parsed > 2.0f) {
                parsed = parsed / 100.0f;
            }

            return clampTextScale(parsed);
        } catch (Exception ignored) {
            return clampTextScale(fallback);
        }
    }

    private static float clampTextScale(float value) {
        return Math.max(0.5f, Math.min(2.0f, value));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                font,
                Component.translatable("multichatwindows.style.menu"),
                width / 2,
                14,
                0xFFFFFFFF
        );

        if (tab != null && tab.useVanilla) {
            com.client.multichatwindows.util.GuiDrawHelper.text(ctx, 
                    font,
                    Component.translatable("multichatwindows.vanilla.info"),
                    20,
                    70,
                    0xFFB0B0B0
            );
        }
    }
}
