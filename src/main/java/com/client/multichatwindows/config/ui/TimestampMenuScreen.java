package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TimestampMenuScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    private ServerConfig sc;
    private EditBox formatField;
    private EditBox colorField;

    public TimestampMenuScreen(Screen parent, String serverKey) {
        super(Component.literal("Timestamp Style"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearWidgets();
        sc = ConfigManager.getOrCreateServer(serverKey);

        if (sc.timestampFormat == null || sc.timestampFormat.isBlank()) {
            sc.timestampFormat = "HH:mm";
        }
        if (sc.timestampColor == null || sc.timestampColor.isBlank()) {
            sc.timestampColor = "AAAAAA";
        }

        int cx = width / 2;
        int y = 50;

        addRenderableWidget(new DarkButton(
                cx - 100,
                y,
                200,
                20,
                Component.literal("Timestamps: " + (sc.timestampsEnabled ? "ON" : "OFF")),
                () -> {
                    sc.timestampsEnabled = !sc.timestampsEnabled;
                    save();
                    init();
                }
        ));

        y += 30;

        formatField = new EditBox(
                font,
                cx - 100,
                y,
                200,
                20,
                Component.literal("Timestamp Format")
        );
        formatField.setValue(sc.timestampFormat == null ? "HH:mm" : sc.timestampFormat);
        formatField.setResponder(s -> {
            sc.timestampFormat = s == null || s.isBlank() ? "HH:mm" : s.trim();
            save();
        });
        addRenderableWidget(formatField);

        y += 30;

        colorField = new EditBox(
                font,
                cx - 100,
                y,
                200,
                20,
                Component.literal("Timestamp Hex Color")
        );
        colorField.setValue(sc.timestampColor == null ? "AAAAAA" : sc.timestampColor);
        colorField.setResponder(s -> {
            sc.timestampColor = sanitizeHex(s);
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
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                font,
                Component.literal("Timestamp Style"),
                width / 2,
                14,
                0xFFFFFFFF
        );

        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                font,
                Component.literal("Server: " + serverKey),
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

        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                font,
                Component.literal("[15:42] Preview message"),
                cx,
                previewY + 4,
                argb
        );
    }
}
