package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.notification.NotificationEntry;
import com.client.multichatwindows.notification.NotificationHistoryManager;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotificationHistoryScreen extends ScrollableDarkScreen {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Screen parent;
    private int scrollY = 0;

    public NotificationHistoryScreen(Screen parent) {
        super(Component.translatable("multichatwindows.notifications.history"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();

        List<NotificationEntry> entries = NotificationHistoryManager.all();
        int centerX = width / 2;
        int top = 48;
        int bottom = height - 60;
        int rowHeight = 42;
        int maxScroll = Math.max(0, entries.size() * rowHeight - (bottom - top));
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));

        int y = top - scrollY;
        for (NotificationEntry entry : entries) {
            if (y + 38 >= top && y <= bottom) {
                String message = entry.message == null ? "" : entry.message;
                if (message.length() > 54) {
                    message = message.substring(0, 51) + "...";
                }
                String label = FORMATTER.format(Instant.ofEpochMilli(entry.timeMs)) + " | " + entry.screenName + " | " + message;
                addRenderableWidget(new DarkButton(centerX - 180, y, 360, 38, Component.literal(label), () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent)));
            }
            y += rowHeight;
        }

        addRenderableWidget(new DarkButton(centerX - 100, height - 28, 200, 20, Component.translatable("multichatwindows.back"), () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent)));
    }

    @Override
    protected boolean onScroll(int delta) {
        scrollY += delta;
        init();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.notifications.history"), width / 2, 14, 0xFFFFFFFF);
    }
}
