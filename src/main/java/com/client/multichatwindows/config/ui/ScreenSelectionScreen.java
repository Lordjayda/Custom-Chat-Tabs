package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ScreenSelectionScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int notifIndex;

    private int scrollY = 0;

    public ScreenSelectionScreen(Screen parent, String serverKey, int notifIndex) {
        super(Text.translatable("multichatwindows.notifications.screens.title"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.notifIndex = notifIndex;
    }

    @Override
    protected void init() {
        clearChildren();
        ServerConfig sc = ConfigManager.getOrCreateServer(serverKey);
        if (sc.notifications == null || notifIndex < 0 || notifIndex >= sc.notifications.size()) {
            addDrawableChild(new DarkButton(
                    width / 2 - 100, height - 28, 200, 20,
                    Text.translatable("multichatwindows.back"),
                    () -> MinecraftClient.getInstance().setScreen(parent)
            ));
            return;
        }

        NotificationConfig n = sc.notifications.get(notifIndex);
        if (n.targetScreens == null) n.targetScreens = new java.util.ArrayList<>();

        int cx = width / 2;
        int top = 54;
        int bottom = height - 80;
        int rowH = 24;

        int max = Math.max(0, sc.tabs.size() * rowH - (bottom - top));
        scrollY = Math.max(0, Math.min(scrollY, max));

        int y = top - scrollY;

        for (TabConfig tab : sc.tabs) {
            if (tab == null) continue;
            final String tabId = tab.id;

            boolean selected = n.targetScreens.contains(tabId);
            String label = (selected ? "✓ " : "✗ ") + (tab.name == null ? tabId : tab.name);

            if (y + 20 >= top && y <= bottom) {
                addDrawableChild(new DarkButton(
                        cx - 100, y, 200, 20,
                        Text.literal(label),
                        () -> {
                            if (n.targetScreens.contains(tabId)) {
                                n.targetScreens.removeIf(s -> s != null && s.equals(tabId));
                            } else {
                                n.targetScreens.add(tabId);
                            }
                            ConfigManager.saveServer(serverKey, sc);
                            init();
                        }
                ));
            }

            y += rowH;
        }

        addDrawableChild(new DarkButton(
                cx - 100, height - 28, 200, 20,
                Text.translatable("multichatwindows.back"),
                () -> {
                    ConfigManager.saveServer(serverKey, sc);
                    MinecraftClient.getInstance().setScreen(parent);
                }
        ));
    }

    @Override
    protected boolean onScroll(int delta) {
        scrollY += delta;
        init();
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("multichatwindows.notifications.screens.title"),
                width / 2, 14, 0xFFFFFFFF);
    }
}