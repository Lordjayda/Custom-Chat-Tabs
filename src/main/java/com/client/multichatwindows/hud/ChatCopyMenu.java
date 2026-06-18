package com.client.multichatwindows.hud;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.util.TextJsonUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Small in-chat copy menu for MultiChatWindows.
 * Uses Minecraft/Fabric translations via Text.translatable(...) labels.
 */
public final class ChatCopyMenu {
    private static boolean visible = false;
    private static int x;
    private static int y;
    private static int width = 54;

    private static final int ROW_HEIGHT = 9;
    private static final int PADDING_X = 3;
    private static final int PADDING_Y = 2;

    private static Text targetMessage;
    private static String targetPlain = "";
    private static String targetJson = "";
    private static String targetPlayer = "";
    private static String targetTab = "";

    private ChatCopyMenu() {
    }

    public static boolean open(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        Text message = WindowService.messageAt(mouseX, mouseY);
        if (message == null) {
            close();
            return false;
        }

        targetMessage = message;
        targetPlain = message.getString();
        targetJson = TextJsonUtil.toJson(message);
        targetPlayer = guessPlayerName(targetPlain);
        targetTab = findTabNameAt(mouseX, mouseY);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        width = 46;
        width = Math.max(width, textRenderer.getWidth(labelMessage()) + PADDING_X * 2);
        if (showJsonRow()) {
            width = Math.max(width, textRenderer.getWidth(labelJson()) + PADDING_X * 2);
        }
        if (!targetPlayer.isBlank()) {
            width = Math.max(width, textRenderer.getWidth(labelPlayer()) + PADDING_X * 2);
        }
        if (!targetTab.isBlank()) {
            width = Math.max(width, textRenderer.getWidth(labelTab()) + PADDING_X * 2);
        }
        width = Math.min(width, 96);

        int height = menuHeight();
        x = clamp((int) mouseX + 4, 2, Math.max(2, screenWidth - width - 2));
        y = clamp((int) mouseY - height / 2, 2, Math.max(2, screenHeight - height - 2));
        visible = true;
        return true;
    }

    public static boolean handleClick(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }

        boolean inside = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + menuHeight();
        if (!inside) {
            close();
            return false;
        }

        if (button != 0) {
            return true;
        }

        int row = ((int) mouseY - y - PADDING_Y) / ROW_HEIGHT;
        copyRow(row);
        close();
        return true;
    }

    public static void render(DrawContext context, TextRenderer textRenderer) {
        if (!visible) {
            return;
        }

        int height = menuHeight();
        context.fill(x, y, x + width, y + height, 0xEE101010);
        context.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        context.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        context.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
        context.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);

        int drawY = y + PADDING_Y;
        context.drawTextWithShadow(textRenderer, labelMessage(), x + PADDING_X, drawY, 0xFFFFFFFF);
        drawY += ROW_HEIGHT;

        if (showJsonRow()) {
            context.drawTextWithShadow(textRenderer, labelJson(), x + PADDING_X, drawY, 0xFFFFFFFF);
            drawY += ROW_HEIGHT;
        }

        if (!targetPlayer.isBlank()) {
            context.drawTextWithShadow(textRenderer, labelPlayer(), x + PADDING_X, drawY, 0xFFFFFFFF);
            drawY += ROW_HEIGHT;
        }

        if (!targetTab.isBlank()) {
            context.drawTextWithShadow(textRenderer, labelTab(), x + PADDING_X, drawY, 0xFFFFFFFF);
        }
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void close() {
        visible = false;
        targetMessage = null;
        targetPlain = "";
        targetJson = "";
        targetPlayer = "";
        targetTab = "";
    }

    private static Text labelMessage() {
        return Text.translatable("multichatwindows.copy_menu.message");
    }

    private static Text labelJson() {
        return Text.literal("Json");
    }

    private static Text labelPlayer() {
        return Text.translatable("multichatwindows.copy_menu.player");
    }

    private static Text labelTab() {
        return Text.translatable("multichatwindows.copy_menu.tab");
    }

    private static boolean showJsonRow() {
        try {
            return ConfigManager.global().debug;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int menuHeight() {
        int rows = 1;
        if (showJsonRow()) rows++;
        if (!targetPlayer.isBlank()) rows++;
        if (!targetTab.isBlank()) rows++;
        return rows * ROW_HEIGHT + PADDING_Y * 2;
    }

    private static void copyRow(int row) {
        String value = targetPlain;
        int current = 0;

        if (row == current) {
            value = targetPlain;
        } else {
            current++;
            if (showJsonRow()) {
                if (row == current) {
                    value = targetJson;
                }
                current++;
            }
            if (!targetPlayer.isBlank()) {
                if (row == current) {
                    value = targetPlayer;
                }
                current++;
            }
            if (!targetTab.isBlank() && row == current) {
                value = targetTab;
            }
        }

        MinecraftClient.getInstance().keyboard.setClipboard(value == null ? "" : value);
    }

    private static String guessPlayerName(String plain) {
        if (plain == null) {
            return "";
        }

        String clean = stripLeadingTimestamp(plain.replaceAll("§.", "").trim());
        if (clean.isBlank()) {
            return "";
        }

        String prefix = null;
        int colon = clean.indexOf(':');
        if (colon > 0) {
            prefix = clean.substring(0, colon).trim();
        } else {
            String[] separators = new String[]{" » ", " > ", " -> ", " | ", " - "};
            for (String separator : separators) {
                int idx = clean.indexOf(separator);
                if (idx > 0) {
                    prefix = clean.substring(0, idx).trim();
                    break;
                }
            }
        }

        if (prefix == null || prefix.isBlank()) {
            return "";
        }

        prefix = prefix.replace("<", " ")
                .replace(">", " ")
                .replace("[", " ")
                .replace("]", " ")
                .replace("(", " ")
                .replace(")", " ")
                .trim();

        if (prefix.isBlank()) {
            return "";
        }

        String[] parts = prefix.split("\\s+");
        if (parts.length == 0) {
            return prefix;
        }

        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = parts[i].trim();
            if (candidate.isBlank()) {
                continue;
            }
            if (candidate.equalsIgnoreCase("system")
                    || candidate.equalsIgnoreCase("server")
                    || candidate.equalsIgnoreCase("chat")
                    || candidate.equalsIgnoreCase("global")
                    || candidate.equalsIgnoreCase("lobby")) {
                continue;
            }
            return candidate;
        }

        return "";
    }

    private static String stripLeadingTimestamp(String value) {
        if (value == null) {
            return "";
        }

        String clean = value.trim();
        clean = clean.replaceFirst("^\\[(?:\\d{1,2}:\\d{2}(?::\\d{2})?)\\]\\s*", "");
        clean = clean.replaceFirst("^\\[\\d{4}-\\d{2}-\\d{2}T[^\\]]+\\]\\s*", "");
        return clean.trim();
    }

    @SuppressWarnings("unchecked")
    private static String findTabNameAt(double mouseX, double mouseY) {
        try {
            Field windowsField = WindowService.class.getDeclaredField("WINDOWS");
            windowsField.setAccessible(true);
            Object raw = windowsField.get(null);
            if (!(raw instanceof Map<?, ?> windows)) {
                return "";
            }

            String last = "";
            for (Object window : windows.values()) {
                if (window == null) {
                    continue;
                }
                boolean enabled = getBoolean(window, "enabled", true);
                int wx = getInt(window, "x", 0);
                int wy = getInt(window, "y", 0);
                int ww = getInt(window, "w", 0);
                int wh = getInt(window, "h", 0);
                if (enabled && mouseX >= wx && mouseX <= wx + ww && mouseY >= wy && mouseY <= wy + wh) {
                    last = getString(window, "displayName", "");
                }
            }
            return last == null ? "" : last;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static int getInt(Object target, String fieldName, int fallback) {
        try {
            Object value = getField(target, fieldName);
            return value instanceof Number number ? number.intValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(Object target, String fieldName, boolean fallback) {
        try {
            Object value = getField(target, fieldName);
            return value instanceof Boolean bool ? bool : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String getString(Object target, String fieldName, String fallback) {
        try {
            Object value = getField(target, fieldName);
            return value == null ? fallback : String.valueOf(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Object getField(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
