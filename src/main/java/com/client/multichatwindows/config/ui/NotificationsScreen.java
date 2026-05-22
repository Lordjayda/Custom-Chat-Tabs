package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class NotificationsScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    private int scrollY = 0;
    private ServerConfig sc;

    public NotificationsScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.notifications.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();
        sc = ConfigManager.getOrCreateServer(serverKey);
        if (sc.notifications == null) sc.notifications = new java.util.ArrayList<>();

        int cx = width / 2;
        int top = 54;
        int bottom = height - 140;
        int rowH = 24;

        int max = Math.max(0, sc.notifications.size() * rowH - (bottom - top));
        scrollY = Math.max(0, Math.min(scrollY, max));

        int y = top - scrollY;

        for (int i = 0; i < sc.notifications.size(); i++) {
            NotificationConfig n = sc.notifications.get(i);
            final int idx = i;

            String label = (n.enabled ? "✓ " : "✗ ") + (n.keyword != null && !n.keyword.isBlank()
                    ? ("\"" + n.keyword.trim() + "\"")
                    : Text.translatable("multichatwindows.notifications.mode.any").getString());

            if (y + 20 >= top && y <= bottom) {
                addDrawableChild(new DarkButton(
                        cx - 100, y, 160, 20,
                        Text.literal(label),
                        () -> MinecraftClient.getInstance().setScreen(
                                new NotificationEditScreen(this, serverKey, idx)
                        )
                ));

                addDrawableChild(new DarkButton(
                        cx + 64, y, 36, 20,
                        Text.translatable("multichatwindows.delete"),
                        () -> {
                            sc.notifications.remove(idx);
                            ConfigManager.saveServer(serverKey, sc);
                            init();
                        }
                ));
            }

            y += rowH;
        }

        addDrawableChild(new DarkButton(
                cx - 100, height - 106, 200, 20,
                Text.translatable("multichatwindows.notifications.add"),
                () -> {
                    NotificationConfig n = new NotificationConfig();
                    n.id = "n" + System.currentTimeMillis();
                    n.enabled = true;
                    n.anyMessage = true;
                    n.keyword = "";
                    n.screenDependent = false;
                    sc.notifications.add(n);
                    ConfigManager.saveServer(serverKey, sc);
                    MinecraftClient.getInstance().setScreen(
                            new NotificationEditScreen(this, serverKey, sc.notifications.size() - 1)
                    );
                }
        ));

        addDrawableChild(new DarkButton(
                cx - 100, height - 80, 200, 20,
                Text.translatable("multichatwindows.notifications.help"),
                () -> MinecraftClient.getInstance().setScreen(new NotificationHelpScreen(this))
        ));

        addDrawableChild(new DarkButton(
                cx - 100, height - 54, 200, 20,
                Text.translatable("multichatwindows.save"),
                () -> {
                    ConfigManager.saveServer(serverKey, sc);
                    MinecraftClient.getInstance().setScreen(parent);
                }
        ));

        addDrawableChild(new DarkButton(
                cx - 100, height - 28, 200, 20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
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
                Text.translatable("multichatwindows.notifications.title"),
                width / 2, 14, 0xFFFFFFFF);
    }
}