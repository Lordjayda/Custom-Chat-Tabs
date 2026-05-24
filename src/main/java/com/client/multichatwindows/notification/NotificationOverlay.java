package com.client.multichatwindows.notification;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        push(new NotificationEntry("", serverKey, screenId, screenName, reason, message, System.currentTimeMillis()));
    }

    public static void render(DrawContext ctx, TextRenderer textRenderer, int screenWidth) {
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
        int notificationHeight = Math.max(34, (int) Math.ceil(textRenderer.fontHeight * textScale) * 3 + 14);
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

    private static void renderSingle(DrawContext ctx, TextRenderer textRenderer, NotificationEntry entry, int x, int y, int width, int height, float textScale, long now) {
        float lifeLeft = 1.0f - ((now - entry.timeMs) / (float) LIFE_MS);
        int alpha = Math.max(60, Math.min(230, (int) (lifeLeft * 230)));
        int background = (alpha << 24) | 0x101010;
        int border = 0xFFFFFFFF;

        ctx.fill(x, y, x + width, y + height, background);
        ctx.fill(x, y, x + width, y + 1, border);
        ctx.fill(x, y + height - 1, x + width, y + height, border);
        ctx.fill(x, y, x + 1, y + height, border);
        ctx.fill(x + width - 1, y, x + width, y + height, border);

        String title = trim("🔔 " + safe(entry.screenName), Math.max(8, (int) ((width - 12) / (6 * textScale))));
        String message = trim(safe(entry.message), Math.max(8, (int) ((width - 12) / (6 * textScale))));

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(textScale, textScale);
        int sx = (int) ((x + 6) / textScale);
        int titleY = (int) ((y + 6) / textScale);
        int messageY = (int) ((y + 8 + textRenderer.fontHeight * textScale + 4) / textScale);
        ctx.drawTextWithShadow(textRenderer, title, sx, titleY, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, message, sx, messageY, 0xFFDDDDDD);
        ctx.getMatrices().popMatrix();
    }

    private static String trim(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
