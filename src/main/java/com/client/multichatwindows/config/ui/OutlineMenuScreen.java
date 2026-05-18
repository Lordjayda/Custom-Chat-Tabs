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

public class OutlineMenuScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int tabIndex;
    private ServerConfig sc;
    private TabConfig tab;
    private TextFieldWidget colorField;

    public OutlineMenuScreen(Screen parent, String serverKey, int tabIndex) {
        super(Text.translatable("multichatwindows.outline.title"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.tabIndex = tabIndex;
    }

    @Override
    protected void init() {
        clearChildren();
        sc = ConfigManager.getOrCreateServer(serverKey);
        tab = (tabIndex >= 0 && tabIndex < sc.tabs.size()) ? sc.tabs.get(tabIndex) : null;
        int cx = width / 2, y = 46;
        if (tab == null) {
            addDrawableChild(new DarkButton(cx - 100, height - 28, 200, 20, Text.translatable("multichatwindows.back"), () -> MinecraftClient.getInstance().setScreen(parent)));
            return;
        }
        addDrawableChild(new DarkButton(cx - 100, y, 200, 20, Text.translatable(tab.outlineEnabled ? "multichatwindows.outline.enabled.on" : "multichatwindows.outline.enabled.off"), () -> { tab.outlineEnabled = !tab.outlineEnabled; save(); init(); }));
        y += 30;
        addDrawableChild(new PixelSlider(cx - 100, y, 160, 20, 1, 20, Math.max(1, Math.min(20, tab.outlineWidth)), v -> { tab.outlineWidth = v; save(); }));
        y += 28;
        colorField = new TextFieldWidget(textRenderer, cx - 100, y, 200, 20, Text.translatable("multichatwindows.outline.color"));
        colorField.setText(tab.outlineColor == null ? "FFFFFF" : tab.outlineColor);
        colorField.setChangedListener(s -> { tab.outlineColor = sanitizeHex(s); save(); });
        addDrawableChild(colorField);
        addDrawableChild(new DarkButton(cx - 100, height - 54, 200, 20, Text.translatable("multichatwindows.save"), () -> { tab.outlineColor = sanitizeHex(colorField.getText()); save(); MinecraftClient.getInstance().setScreen(parent); }));
        addDrawableChild(new DarkButton(cx - 100, height - 28, 200, 20, Text.translatable("multichatwindows.back"), () -> MinecraftClient.getInstance().setScreen(parent)));
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
        if (s.startsWith("#")) s = s.substring(1);
        s = s.replaceAll("[^0-9a-fA-F]", "");
        if (s.length() > 6) s = s.substring(0, 6);
        if (s.isEmpty()) s = "FFFFFF";
        while (s.length() < 6) s = "0" + s;
        return s.toUpperCase();
    }

    private static int parseColor(String hex) { try { return Integer.parseInt(sanitizeHex(hex), 16) & 0xFFFFFF; } catch (Exception e) { return 0xFFFFFF; } }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.outline.title"), width / 2, 14, 0xFFFFFFFF);
        if (tab == null) return;
        int cx = width / 2;
        ctx.drawTextWithShadow(textRenderer, Text.translatable("multichatwindows.outline.width", tab.outlineWidth), cx - 100, 76, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.outline.preview"), cx, 110, 0xFFFFFFFF);
        int previewX1 = cx - 60, previewY1 = 126, previewX2 = cx + 60, previewY2 = 154;
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
