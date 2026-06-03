package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class TabsScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    public TabsScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.servermenu.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();
        ServerConfig sc = ConfigManager.getOrCreateServer(serverKey);

        int cx = width / 2;
        int y = 54;

        addDrawableChild(new DarkButton(
                cx - 110, y, 220, 20,
                Text.translatable("multichatwindows.screens.open"),
                () -> MinecraftClient.getInstance().setScreen(new ScreensMenuScreen(this, serverKey))
        ));
        y += 26;

        addDrawableChild(new DarkButton(
                cx - 110, y, 220, 20,
                Text.translatable("multichatwindows.notifications.open"),
                () -> MinecraftClient.getInstance().setScreen(new NotificationsScreen(this, serverKey))
        ));
        y += 26;

        addDrawableChild(new DarkButton(
                cx - 110, y, 220, 20,
                Text.translatable("multichatwindows.chatfilters.open"),
                () -> MinecraftClient.getInstance().setScreen(new ChatFiltersScreen(this, serverKey))
        ));
        y += 26;

        addDrawableChild(new DarkButton(
                cx - 110, y, 220, 20,
                Text.literal("Timestamp Style"),
                () -> MinecraftClient.getInstance().setScreen(new TimestampMenuScreen(this, serverKey))
        ));

        addDrawableChild(new DarkButton(
                cx - 110, height - 54, 220, 20,
                Text.translatable("multichatwindows.save"),
                () -> {
                    ConfigManager.saveServer(serverKey, sc);
                    WindowService.rebuildForCurrentServer();
                    MinecraftClient.getInstance().setScreen(parent);
                }
        ));

        addDrawableChild(new DarkButton(
                cx - 110, height - 28, 220, 20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.servermenu.title"),
                width / 2,
                14,
                0xFFFFFFFF
        );
        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.server.current", Text.literal(serverKey)),
                width / 2,
                28,
                0xFFB0B0B0
        );
    }
}
