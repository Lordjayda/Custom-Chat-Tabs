package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TimestampMenuScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    private ServerConfig sc;
    private TextFieldWidget formatField;
    private TextFieldWidget colorField;

    public TimestampMenuScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.timestamp.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();
        sc = ConfigManager.getOrCreateServer(serverKey);

        if (sc.timestampFormat == null || sc.timestampFormat.isBlank()) {
            sc.timestampFormat = "HH:mm";
        }
        if (sc.timestampColor == null || sc.timestampColor.isBlank()) {
            sc.timestampColor = "AAAAAA";
        }

        int cx = width / 2;
        int y = 50;

        addDrawableChild(new DarkButton(
                cx - 100,
                y,
                200,
                20,
                Text.translatable(sc.timestampsEnabled ? "multichatwindows.timestamp.enabled.on" : "multichatwindows.timestamp.enabled.off"),
                () -> {
                    sc.timestampsEnabled = !sc.timestampsEnabled;
                    save();
                    init();
                }
        ));

        y += 30;

        formatField = new TextFieldWidget(
                textRenderer,
                cx - 100,
                y,
                200,
                20,
                Text.translatable("multichatwindows.timestamp.format")
        );
        formatField.setText(sc.timestampFormat == null ? "HH:mm" : sc.timestampFormat);
        formatField.setChangedListener(s -> {
            sc.timestampFormat = s == null || s.isBlank() ? "HH:mm" : s.trim();
            save();
        });
        addDrawableChild(formatField);

        y += 30;

        colorField = new TextFieldWidget(
                textRenderer,
                cx - 100,
                y,
                200,
                20,
                Text.translatable("multichatwindows.timestamp.color")
        );
        colorField.setText(sc.timestampColor == null ? "AAAAAA" : sc.timestampColor);
        colorField.setChangedListener(s -> {
            sc.timestampColor = sanitizeHex(s);
            save();
        });
        addDrawableChild(colorField);

        

        addDrawableChild(new DarkButton(
                cx - 100,
                height - 28,
                200,
                20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    private void save() {
        sc.timestampFormat = sc.timestampFormat == null || sc.timestampFormat.isBlank()
                ? "HH:mm"
                : sc.timestampFormat.trim();
        sc.timestampColor = sanitizeHex(sc.timestampColor);
        ConfigManager.saveServer(serverKey, sc);
        WindowService.rebuildForCurrentServer();
    }

    private static String sanitizeHex(String value) {
        if (value == null) return "AAAAAA";
        String s = value.trim().replace("#", "").replace("0x", "").replace("0X", "");
        s = s.replaceAll("[^0-9a-fA-F]", "");
        if (s.length() > 6) s = s.substring(0, 6);
        if (s.isBlank()) return "AAAAAA";
        while (s.length() < 6) s = s + "0";
        return s.toUpperCase();
    }

    private static int parseColor(String value) {
        try {
            return Integer.parseInt(sanitizeHex(value), 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return 0xAAAAAA;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.timestamp.title"),
                width / 2,
                14,
                0xFFFFFFFF
        );

        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Server: " + serverKey),
                width / 2,
                28,
                0xFFB0B0B0
        );

        int cx = width / 2;
        int previewY = 145;
        int color = parseColor(sc == null ? "AAAAAA" : sc.timestampColor);
        int argb = 0xFF000000 | color;

        ctx.fill(cx - 104, previewY - 6, cx + 104, previewY + 24, 0xAA101010);
        ctx.fill(cx - 105, previewY - 7, cx + 105, previewY - 6, argb);
        ctx.fill(cx - 105, previewY + 24, cx + 105, previewY + 25, argb);
        ctx.fill(cx - 105, previewY - 7, cx - 104, previewY + 25, argb);
        ctx.fill(cx + 104, previewY - 7, cx + 105, previewY + 25, argb);

        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("[15:42] Preview message"),
                cx,
                previewY + 4,
                argb
        );
    }
}
