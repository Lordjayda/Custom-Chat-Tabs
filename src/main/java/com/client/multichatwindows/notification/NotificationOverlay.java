package com.client.multichatwindows.notification;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.hud.ChatWindow;
import com.client.multichatwindows.hud.WindowService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class NotificationOverlay {
    private static final List<NotificationEntry> ACTIVE = new ArrayList<>();
    private static final long LIFE_MS = 5500L;

    private NotificationOverlay() {
    }

    public static void push(NotificationEntry entry) {
        if (entry == null) {
            return;
        }

        ACTIVE.add(0, entry);
        NotificationHistoryManager.add(entry);

        ServerConfig serverConfig = ConfigManager.getOrCreateServer(WindowService.currentServerKey());
        int maxVisible = Math.max(1, Math.min(10, serverConfig.notificationMaxVisible));
        while (ACTIVE.size() > maxVisible) {
            ACTIVE.remove(ACTIVE.size() - 1);
        }
    }

    public static void push(String serverKey, String screenId, String screenName, String reason, String message) {
        Component formatted = findFormattedMessage(screenId, message);
        push(new NotificationEntry("", serverKey, screenId, screenName, reason, message, formatted, System.currentTimeMillis()));
    }

    public static void push(String serverKey, String screenId, String screenName, String reason, Component message) {
        String plain = message == null ? "" : message.getString();
        push(new NotificationEntry("", serverKey, screenId, screenName, reason, plain, message == null ? Component.empty() : message.copy(), System.currentTimeMillis()));
    }

    public static void render(GuiGraphicsExtractor ctx, Font textRenderer, int screenWidth) {
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(WindowService.currentServerKey());
        long now = System.currentTimeMillis();

        Iterator<NotificationEntry> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().timeMs > LIFE_MS) {
                iterator.remove();
            }
        }

        int notificationWidth = Math.max(120, Math.min(500, serverConfig.notificationWidth));
        float textScale = Math.max(0.5f, Math.min(3.0f, serverConfig.notificationTextScale));
        int notificationHeight = Math.max(34, (int) Math.ceil(textRenderer.lineHeight * textScale) * 3 + 14);
        int spacing = Math.max(4, (int) (6 * textScale));
        int x = serverConfig.notificationX < 0 ? screenWidth - notificationWidth - 12 : serverConfig.notificationX;
        int y = Math.max(0, serverConfig.notificationY);
        int maxVisible = Math.max(1, Math.min(10, serverConfig.notificationMaxVisible));
        int rendered = 0;

        for (NotificationEntry entry : ACTIVE) {
            if (rendered >= maxVisible) {
                break;
            }
            renderSingle(ctx, textRenderer, entry, x, y, notificationWidth, notificationHeight, textScale, now);
            y += notificationHeight + spacing;
            rendered++;
        }
    }

    private static void renderSingle(GuiGraphicsExtractor ctx, Font textRenderer, NotificationEntry entry, int x, int y, int width, int height, float textScale, long now) {
        float lifeLeft = 1.0f - ((now - entry.timeMs) / (float) LIFE_MS);
        int alpha = Math.max(60, Math.min(230, (int) (lifeLeft * 230)));
        int background = (alpha << 24) | 0x101010;
        int border = 0xFFFFFFFF;

        ctx.fill(x, y, x + width, y + height, background);
        ctx.fill(x, y, x + width, y + 1, border);
        ctx.fill(x, y + height - 1, x + width, y + height, border);
        ctx.fill(x, y, x + 1, y + height, border);
        ctx.fill(x + width - 1, y, x + width, y + height, border);

        Component title = Component.literal("🔔 ").append(Component.literal(safe(entry.screenName)));
        Component message = entry.formattedMessage == null ? Component.literal(safe(entry.message)) : entry.formattedMessage.copy();

        int scaledTextWidth = Math.max(20, (int) ((width - 12) / textScale));
        List<FormattedCharSequence> titleLines = textRenderer.split(title, scaledTextWidth);
        List<FormattedCharSequence> messageLines = textRenderer.split(message, scaledTextWidth);

        ctx.pose().pushMatrix();
        ctx.pose().scale(textScale, textScale);

        int sx = (int) ((x + 6) / textScale);
        int drawY = (int) ((y + 6) / textScale);
        int maxTextLines = Math.max(1, (int) ((height - 12) / (textRenderer.lineHeight * textScale)));
        int drawn = 0;

        if (!titleLines.isEmpty() && drawn < maxTextLines) {
            com.client.multichatwindows.util.GuiDrawHelper.text(ctx, textRenderer, titleLines.get(0), sx, drawY, 0xFFFFFFFF);
            drawY += textRenderer.lineHeight + 2;
            drawn++;
        }

        for (FormattedCharSequence line : messageLines) {
            if (drawn >= maxTextLines) {
                break;
            }
            com.client.multichatwindows.util.GuiDrawHelper.text(ctx, textRenderer, line, sx, drawY, 0xFFFFFFFF);
            drawY += textRenderer.lineHeight + 2;
            drawn++;
        }

        ctx.pose().popMatrix();
    }

    private static Component findFormattedMessage(String screenId, String plain) {
        if (plain == null) {
            plain = "";
        }

        Component exact = findFormattedMessageInWindow(screenId, plain);
        if (exact != null) {
            return exact;
        }

        for (ChatWindow window : WindowService.allWindows()) {
            if (window == null) {
                continue;
            }

            Component candidate = findFormattedMessageInWindow(window.id, plain);
            if (candidate != null) {
                return candidate;
            }
        }

        return Component.literal(plain);
    }

    private static Component findFormattedMessageInWindow(String screenId, String plain) {
        if (screenId == null || plain == null) {
            return null;
        }

        for (ChatWindow window : WindowService.allWindows()) {
            if (window == null || !screenId.equals(window.id)) {
                continue;
            }

            for (int i = window.entries.size() - 1; i >= 0; i--) {
                ChatWindow.Entry entry = window.entries.get(i);
                if (entry != null && entry.original != null && plain.equals(entry.original.getString())) {
                    return entry.original.copy();
                }
            }
        }

        return null;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
