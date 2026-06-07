package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class StyleMenuScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int tabIndex;
    private ServerConfig sc;
    private TabConfig tab;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget wField;
    private TextFieldWidget hField;
    private TextFieldWidget opField;
    private TextFieldWidget textScaleField;

    public StyleMenuScreen(Screen parent, String serverKey, int tabIndex) {
        super(Text.translatable("multichatwindows.style.menu"));
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
        int y = 42;

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

        addDrawableChild(new DarkButton(
                cx - 104,
                y,
                208,
                20,
                Text.translatable(tab.useVanilla
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

        addDrawableChild(new DarkButton(
                left,
                y,
                100,
                20,
                Text.translatable("multichatwindows.outline.open"),
                () -> MinecraftClient.getInstance().setScreen(new OutlineMenuScreen(this, serverKey, tabIndex))
        ));
        addDrawableChild(new DarkButton(
                right,
                y,
                100,
                20,
                Text.translatable("multichatwindows.style.edit_pos_size"),
                () -> MinecraftClient.getInstance().setScreen(new RealTimeEditorScreen(this, serverKey, tabIndex))
        ));
        y += 28;

        boolean editable = !tab.useVanilla;
        xField.setEditable(editable);
        yField.setEditable(editable);
        wField.setEditable(editable);
        hField.setEditable(editable);
        opField.setEditable(editable);
        textScaleField.setEditable(editable);

        
        addDrawableChild(new DarkButton(
                cx - 104,
                height - 28,
                208,
                20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    private TextFieldWidget field(int x, int y, int w, String key, int value) {
        return field(x, y, w, key, String.valueOf(value));
    }

    private TextFieldWidget field(int x, int y, int w, String key, float value) {
        return field(x, y, w, key, String.valueOf(value));
    }

    private TextFieldWidget field(int x, int y, int w, String key, String value) {
        TextFieldWidget tf = new TextFieldWidget(
                textRenderer,
                x,
                y,
                w,
                20,
                Text.translatable(key)
        );
        tf.setText(value == null ? "" : value);
        addDrawableChild(tf);
        return tf;
    }

    private void apply() {
        if (tab == null || tab.useVanilla) return;
        tab.x = parseInt(xField.getText(), tab.x);
        tab.y = parseInt(yField.getText(), tab.y);
        tab.width = Math.max(60, parseInt(wField.getText(), tab.width));
        tab.height = Math.max(40, parseInt(hField.getText(), tab.height));
        tab.opacity = Math.max(0.0f, Math.min(1.0f, parseFloat(opField.getText(), tab.opacity)));
        tab.textScale = parseTextScale(textScaleField.getText(), tab.textScale);
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
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.style.menu"),
                width / 2,
                14,
                0xFFFFFFFF
        );

        if (tab != null && tab.useVanilla) {
            ctx.drawTextWithShadow(
                    textRenderer,
                    Text.translatable("multichatwindows.vanilla.info"),
                    20,
                    70,
                    0xFFB0B0B0
            );
        }
    }
}
