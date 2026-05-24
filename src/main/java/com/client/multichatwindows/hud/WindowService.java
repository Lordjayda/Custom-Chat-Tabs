package com.client.multichatwindows.hud;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ChatFilterRule;
import com.client.multichatwindows.config.model.DependencyRule;
import com.client.multichatwindows.config.model.GlobalConfig;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import com.client.multichatwindows.notification.NotificationEntry;
import com.client.multichatwindows.notification.NotificationOverlay;
import com.client.multichatwindows.util.DebugLog;
import com.client.multichatwindows.util.EventLog;
import com.client.multichatwindows.util.I18nUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WindowService {
    private static final Map<String, ChatWindow> WINDOWS = new LinkedHashMap<>();
    private static String lastServerKey = "";

    private WindowService() {
    }

    public static Collection<ChatWindow> allWindows() {
        ensureForCurrentServer();
        return WINDOWS.values();
    }

    public static String currentServerKey() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ServerInfo serverInfo = minecraftClient.getCurrentServerEntry();
        if (serverInfo == null) {
            return "singleplayer";
        }
        return serverInfo.address == null || serverInfo.address.isBlank() ? "unknown" : serverInfo.address;
    }

    public static void ensureForCurrentServer() {
        String key = currentServerKey();
        if (!key.equals(lastServerKey)) {
            rebuildForCurrentServer();
        }
    }

    public static void rebuildForCurrentServer() {
        String key = currentServerKey();
        lastServerKey = key;

        ServerConfig serverConfig = ConfigManager.getOrCreateServer(key);
        Map<String, ChatWindow> oldWindows = new LinkedHashMap<>(WINDOWS);
        WINDOWS.clear();

        for (TabConfig tab : serverConfig.tabs) {
            ChatWindow window = oldWindows.get(tab.id);
            if (window == null) {
                window = new ChatWindow(tab.id);
            }

            window.displayName = I18nUtil.tKeyOrLiteral(tab.name).getString();
            window.enabled = tab.enabled;
            window.useVanilla = tab.useVanilla;
            window.x = tab.x;
            window.y = tab.y;
            window.w = tab.width;
            window.h = tab.height;
            window.opacity = tab.opacity;
            window.textScale = Math.max(0.5f, Math.min(3.0f, tab.textScale));
            window.outlineEnabled = tab.outlineEnabled;
            window.outlineWidth = Math.max(1, Math.min(20, tab.outlineWidth));
            window.outlineColor = parseColor(tab.outlineColor);
            WINDOWS.put(tab.id, window);
        }
    }

    public static boolean scrollAt(double mouseX, double mouseY, double verticalAmount) {
        ensureForCurrentServer();
        ChatWindow target = null;

        for (ChatWindow window : WINDOWS.values()) {
            if (!window.enabled) {
                continue;
            }
            if (mouseX >= window.x && mouseX <= window.x + window.w && mouseY >= window.y && mouseY <= window.y + window.h) {
                target = window;
            }
        }

        if (target == null) {
            return false;
        }

        int delta = verticalAmount > 0 ? 3 : -3;
        int max = Math.max(0, target.lines.size());
        target.scrollOffset = Math.max(0, Math.min(max, target.scrollOffset + delta));
        return true;
    }

    public static void resetScrollOnChatClose() {
        for (ChatWindow window : WINDOWS.values()) {
            window.scrollOffset = 0;
        }
    }

    public static Text messageAt(double mouseX, double mouseY) {
        ensureForCurrentServer();

        for (ChatWindow window : WINDOWS.values()) {
            if (!window.enabled) {
                continue;
            }
            if (mouseX >= window.x && mouseX <= window.x + window.w && mouseY >= window.y && mouseY <= window.y + window.h) {
                if (!window.lines.isEmpty()) {
                    return window.lines.get(window.lines.size() - 1);
                }
            }
        }

        return null;
    }

    public static void jumpToRecent(String screenId, String plain) {
        ensureForCurrentServer();
        ChatWindow window = WINDOWS.get(screenId);
        if (window == null || plain == null) {
            return;
        }

        for (int i = window.lines.size() - 1; i >= 0; i--) {
            if (window.lines.get(i).getString().equals(plain)) {
                int fromBottom = window.lines.size() - 1 - i;
                window.scrollOffset = Math.max(0, fromBottom);
                return;
            }
        }
    }

    public static void importExternalMessage(String screenId, Text message) {
        ensureForCurrentServer();
        ChatWindow window = WINDOWS.get(screenId);
        if (window == null) {
            window = WINDOWS.get("all");
        }
        if (window != null) {
            window.push(message);
            notifyIfConfigured(currentServerKey(), screenId, window.displayName, message == null ? "" : message.getString());
        }
    }

    public static boolean routeIncoming(Text message) {
        GlobalConfig globalConfig = ConfigManager.global();
        if (!globalConfig.enabled) {
            return false;
        }

        ensureForCurrentServer();
        String serverKey = currentServerKey();
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        String plain = message == null ? "" : message.getString();
        String player = guessPlayerName(plain);

        boolean filterMatch = false;
        String filterReason = "";

        if (serverConfig.chatFilters != null) {
            for (ChatFilterRule rule : serverConfig.chatFilters) {
                if (rule != null && rule.matches(player, plain)) {
                    filterMatch = true;
                    filterReason = rule.describe();
                    break;
                }
            }
        }

        if (!serverConfig.chatFilterAllMode && filterMatch) {
            EventLog.filtered(serverKey, filterReason, plain);
            return true;
        }

        boolean filterFromAll = false;

        for (TabConfig tab : serverConfig.tabs) {
            if (tab == null || !tab.enabled) {
                continue;
            }
            if ("all".equalsIgnoreCase(tab.id)) {
                continue;
            }
            if (tab.dependencies == null || tab.dependencies.isEmpty()) {
                continue;
            }

            for (DependencyRule rule : tab.dependencies) {
                if (rule != null && rule.matches(player, plain)) {
                    ChatWindow window = WINDOWS.get(tab.id);
                    if (window != null) {
                        window.push(message);
                        DebugLog.write(plain, window.displayName, rule.describe());
                        notifyIfConfigured(serverKey, tab.id, window.displayName, plain);
                    }
                    if (tab.filterAllChat) {
                        filterFromAll = true;
                    }
                    break;
                }
            }
        }

        if (!filterFromAll && (!serverConfig.chatFilterAllMode || filterMatch)) {
            ChatWindow all = WINDOWS.get("all");
            if (all != null) {
                all.push(message);
                notifyIfConfigured(serverKey, "all", all.displayName, plain);
            }
        }

        if (serverConfig.chatFilterAllMode && !filterMatch) {
            EventLog.filtered(serverKey, "allMode:notMatched", plain);
        }

        return true;
    }

    private static void notifyIfConfigured(String serverKey, String screenId, String screenName, String plain) {
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        if (serverConfig.notifications == null || serverConfig.notifications.isEmpty()) {
            return;
        }

        for (NotificationConfig notification : serverConfig.notifications) {
            if (!shouldTrigger(notification, screenId, plain)) {
                continue;
            }

            String reason = notification.keyword != null && !notification.keyword.isBlank()
                    ? "keyword:" + notification.keyword.trim()
                    : "message";

            NotificationOverlay.push(new NotificationEntry(
                    notification.id,
                    serverKey,
                    screenId,
                    screenName,
                    reason,
                    plain,
                    System.currentTimeMillis()
            ));
        }
    }

    private static boolean shouldTrigger(NotificationConfig notification, String screenId, String plain) {
        if (notification == null || !notification.enabled) {
            return false;
        }

        if (notification.screenDependent) {
            if (notification.targetScreens == null || !notification.targetScreens.contains(screenId)) {
                return false;
            }
        }

        if (notification.keyword != null && !notification.keyword.isBlank()) {
            return plain != null && plain.toLowerCase().contains(notification.keyword.trim().toLowerCase());
        }

        return notification.anyMessage;
    }

    private static int parseColor(String hex) {
        if (hex == null) {
            return 0xFFFFFF;
        }

        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }

        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return 0xFFFFFF;
        }
    }

    private static String guessPlayerName(String plain) {
        if (plain == null) {
            return null;
        }

        int index = plain.indexOf(':');
        if (index <= 0) {
            return null;
        }

        String prefix = plain.substring(0, index).trim();
        prefix = prefix.replace("<", "")
                .replace("[", "")
                .replace(">", "")
                .replace("]", "")
                .replaceAll("§.", "")
                .trim();

        if (prefix.isEmpty()) {
            return null;
        }

        String[] parts = prefix.split("\\s+");
        return parts.length == 0 ? prefix : parts[parts.length - 1];
    }
}
