package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.hud.WindowService;
import java.lang.reflect.Field;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

final class ServerHistoryTrimButton {
    private static final int KEEP_MESSAGES = 100;
    private static final int X = 8;
    private static final int W = 178;
    private static final int H = 20;

    private ServerHistoryTrimButton() {
    }

    static void render(Screen screen, DrawContext ctx, int width, int height, int mouseX, int mouseY) {
        String serverKey = findServerKey(screen);
        if (serverKey == null || serverKey.isBlank()) {
            return;
        }

        int y = y(height);
        boolean hovered = contains(height, mouseX, mouseY);
        int bg = hovered ? 0xFF242424 : 0xFF141414;
        int border = hovered ? 0xFFFFCC66 : 0xFFFFFFFF;

        ctx.fill(X, y, X + W, y + H, bg);
        ctx.fill(X, y, X + W, y + 1, border);
        ctx.fill(X, y + H - 1, X + W, y + H, border);
        ctx.fill(X, y, X + 1, y + H, border);
        ctx.fill(X + W - 1, y, X + W, y + H, border);

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        ctx.drawTextWithShadow(font, Text.literal("Trim tabs to 100 msgs"), X + 6, y + 6, 0xFFFFFFFF);
    }

    static boolean mouseClicked(Screen screen, int width, int height, double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(height, mouseX, mouseY)) {
            return false;
        }

        String serverKey = findServerKey(screen);
        if (serverKey == null || serverKey.isBlank()) {
            return false;
        }

        WindowService.trimServerMessagesToLast(serverKey, KEEP_MESSAGES);
        return true;
    }

    private static boolean contains(int height, double mouseX, double mouseY) {
        int y = y(height);
        return mouseX >= X && mouseX <= X + W && mouseY >= y && mouseY <= y + H;
    }

    private static int y(int height) {
        return Math.max(8, height - 28);
    }

    private static String findServerKey(Screen screen) {
        if (screen == null) {
            return null;
        }

        Class<?> type = screen.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField("serverKey");
                field.setAccessible(true);
                Object value = field.get(screen);
                if (value instanceof String string) {
                    return string;
                }
            } catch (ReflectiveOperationException ignored) {
            }
            type = type.getSuperclass();
        }
        return null;
    }
}
