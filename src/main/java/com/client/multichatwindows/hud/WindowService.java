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
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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
        int max = target.maxScrollOffset();
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

        ChatWindow.RowRef row = rowAt(mouseX, mouseY);
        return row == null ? null : row.source;
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
        String serverKey = currentServerKey();
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        String plain = message == null ? "" : message.getString();
        ChatWindow window = WINDOWS.get(screenId);
        if (window == null) {
            window = WINDOWS.get("all");
        }
        if (window != null) {
            window.push(withTimestamp(serverConfig, message), plain);
            notifyIfConfigured(serverKey, screenId, window.displayName, plain);
        }
    }

    public static boolean routeIncoming(Text message) {
        GlobalConfig globalConfig = ConfigManager.global();
        if (!globalConfig.enabled) {
            return false;
        }

        String plain = message == null ? "" : message.getString();
        if (shouldPassThroughVanilla(plain)) {
            return false;
        }

        ensureForCurrentServer();
        String serverKey = currentServerKey();
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        String player = guessPlayerName(plain);
        boolean serverMessage = isUnfilterableServerMessage(plain);
        String stackKey = serverMessage ? "server:" + plain : plain;

        boolean filterMatch = false;
        String filterReason = "";

        if (!serverMessage && serverConfig.chatFilters != null) {
            for (ChatFilterRule rule : serverConfig.chatFilters) {
                if (rule != null && rule.matches(player, plain)) {
                    filterMatch = true;
                    filterReason = rule.describe();
                    break;
                }
            }
        }

        if (!serverMessage && !serverConfig.chatFilterAllMode && filterMatch) {
            breakStackForAllWindows();
            EventLog.filtered(serverKey, filterReason, plain);
            return true;
        }

        boolean filterFromAll = false;
        boolean routedToCustomTab = false;

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
                        window.push(withTimestamp(serverConfig, message), stackKey);
                        routedToCustomTab = true;
                        DebugLog.write(plain, window.displayName, rule.describe());
                        notifyIfConfigured(serverKey, tab.id, window.displayName, plain);
                    }
                    if (!serverMessage && tab.filterAllChat) {
                        filterFromAll = true;
                        breakStackForWindow("all");
                    }
                    break;
                }
            }
        }

        boolean shouldPushToAll = serverMessage || (!filterFromAll && (!serverConfig.chatFilterAllMode || filterMatch || !routedToCustomTab));
        if (shouldPushToAll) {
            ChatWindow all = WINDOWS.get("all");
            if (all != null) {
                all.push(withTimestamp(serverConfig, message), stackKey);
                notifyIfConfigured(serverKey, "all", all.displayName, plain);
            }
        } else if (!serverMessage) {
            breakStackForWindow("all");
        }

        if (!serverMessage && serverConfig.chatFilterAllMode && !filterMatch && !routedToCustomTab) {
            breakStackForWindow("all");
            EventLog.filtered(serverKey, "allMode:notMatched", plain);
        }

        return true;
    }

    private static boolean isUnfilterableServerMessage(String plain) {
        if (plain == null || plain.isBlank()) {
            return false;
        }

        String trimmed = plain.trim();
        if (trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0) {
            return true;
        }

        return trimmed.startsWith("[System]")
                || trimmed.startsWith("[Server]")
                || trimmed.startsWith("[Proxy]")
                || trimmed.startsWith("[Console]");
    }

    private static boolean shouldPassThroughVanilla(String plain) {
        // Debug/F3 messages should be routed into MultiChatWindows custom windows.
        // Keep this method as a safe extension point, but do not bypass routing here.
        return false;
    }

    private static Text withTimestamp(ServerConfig serverConfig, Text message) {
        Text base = message == null ? Text.empty() : message.copy();
        String basePlain = base.getString();
        if (basePlain == null || basePlain.isBlank()) {
            return base;
        }
        if (serverConfig == null || !serverConfig.timestampsEnabled) {
            return base;
        }

        String format = serverConfig.timestampFormat == null || serverConfig.timestampFormat.isBlank()
                ? "HH:mm"
                : serverConfig.timestampFormat.trim();

        String time;
        try {
            time = LocalTime.now().format(DateTimeFormatter.ofPattern(format));
        } catch (Exception ignored) {
            time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        int color = parseColor(serverConfig.timestampColor == null ? "AAAAAA" : serverConfig.timestampColor);
        return Text.literal("[" + time + "] ")
                .styled(style -> style.withColor(color))
                .append(base);
    }

    private static void breakStackForWindow(String screenId) {
        ChatWindow window = WINDOWS.get(screenId);
        if (window != null) {
            window.breakStack();
        }
    }

    private static void breakStackForAllWindows() {
        for (ChatWindow window : WINDOWS.values()) {
            if (window != null) {
                window.breakStack();
            }
        }
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

    
    public static Style styleAt(double mouseX, double mouseY) {
        ensureForCurrentServer();

        ChatWindow window = windowAt(mouseX, mouseY);
        if (window == null) {
            return null;
        }

        return window.styleAt(mouseX, mouseY);
    }

    public static boolean handleClickAt(double mouseX, double mouseY, SuggestionSink suggestionSink) {
        ensureForCurrentServer();

        Style style = styleAt(mouseX, mouseY);
        if (style == null || style.getClickEvent() == null) {
            return false;
        }

        String value = ChatWindow.extractClickValue(style.getClickEvent());
        if (value == null || value.isEmpty()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }

        switch (style.getClickEvent().getAction()) {
            case OPEN_URL:
                return ChatWindow.openUrl(value);

            case RUN_COMMAND:
                if (client.player != null && client.player.networkHandler != null) {
                    client.player.networkHandler.sendChatCommand(value.replaceFirst("^/", ""));
                    return true;
                }
                return false;

            case SUGGEST_COMMAND:
                if (suggestionSink != null && suggestionSink.setSuggestion(value)) {
                    return true;
                }
                if (client.inGameHud != null && client.inGameHud.getChatHud() != null) {
                    client.inGameHud.getChatHud().addToMessageHistory(value);
                    return true;
                }
                return false;

            case COPY_TO_CLIPBOARD:
                client.keyboard.setClipboard(value);
                return true;

            default:
                return false;
        }
    }

    @FunctionalInterface
    public interface SuggestionSink {
        boolean setSuggestion(String value);
    }

    private static ChatWindow windowAt(double mouseX, double mouseY) {
        for (ChatWindow window : WINDOWS.values()) {
            if (!window.enabled) {
                continue;
            }

            boolean inside =
                    mouseX >= window.x && mouseX <= window.x + window.w &&
                    mouseY >= window.y && mouseY <= window.y + window.h;

            if (inside) {
                return window;
            }
        }

        return null;
    }

    private static ChatWindow.RowRef rowAt(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return null;
        }

        ChatWindow window = windowAt(mouseX, mouseY);
        if (window == null) {
            return null;
        }

        return window.rowAt(mouseX, mouseY);
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

        String cleaned = plain.replaceAll("§.", "").trim();
        if (cleaned.isEmpty()) {
            return null;
        }

        String prefix = null;
        int colon = cleaned.indexOf(':');
        if (colon > 0) {
            prefix = cleaned.substring(0, colon).trim();
        } else {
            String[] separators = new String[]{" » ", " > ", " -> ", " | ", " - "};
            for (String separator : separators) {
                int idx = cleaned.indexOf(separator);
                if (idx > 0) {
                    prefix = cleaned.substring(0, idx).trim();
                    break;
                }
            }
        }

        if (prefix == null || prefix.isEmpty()) {
            return null;
        }

        prefix = prefix.replace("<", " ")
                .replace(">", " ")
                .replace("[", " ")
                .replace("]", " ")
                .replace("(", " ")
                .replace(")", " ")
                .trim();

        if (prefix.isEmpty()) {
            return null;
        }

        String[] parts = prefix.split("\\s+");
        if (parts.length == 0) {
            return prefix;
        }

        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = parts[i].trim();
            if (candidate.isEmpty()) {
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

        return null;
    }
}
