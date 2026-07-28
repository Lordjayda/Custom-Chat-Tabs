package com.client.multichatwindows.hud;

import com.mojang.logging.LogUtils;
import com.mojang.authlib.GameProfile;
import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ChatFilterRule;
import com.client.multichatwindows.config.model.DependencyRule;
import com.client.multichatwindows.config.model.GlobalConfig;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import com.client.multichatwindows.notification.NotificationEntry;
import com.client.multichatwindows.notification.NotificationOverlay;
import com.client.multichatwindows.notification.NotificationSoundPlayer;
import com.client.multichatwindows.util.DebugLog;
import com.client.multichatwindows.util.EventLog;
import com.client.multichatwindows.util.I18nUtil;
import org.slf4j.Logger;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;

public final class WindowService {
    private static final Logger MCW_ROUTE_LOGGER = LogUtils.getLogger();
    private static final Map<String, ChatWindow> WINDOWS = new LinkedHashMap<>();
    private static final int DISABLED_BACKLOG_LIMIT = 10000;
    private static final List<Component> DISABLED_BACKLOG = new ArrayList<>();
    private static final Set<Component> DISABLED_BACKLOG_IDENTITIES = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Component> ROUTED_COMPONENT_IDENTITIES = Collections.newSetFromMap(new IdentityHashMap<>());
    private static String lastServerKey = "";

    private WindowService() {
    }

    public static boolean isGloballyEnabled() {
        return ConfigManager.global().enabled;
    }

    public static void captureWhileDisabled(Component message) {
        if (message == null || isGloballyEnabled()) {
            return;
        }

        synchronized (DISABLED_BACKLOG) {
            if (!DISABLED_BACKLOG_IDENTITIES.add(message)) {
                return;
            }

            logRouteDecisionToMinecraftLog("tabs: minecraft chat", message.getString());
            DISABLED_BACKLOG.add(message.copy());
            while (DISABLED_BACKLOG.size() > DISABLED_BACKLOG_LIMIT) {
                DISABLED_BACKLOG.remove(0);
            }
        }
    }

    public static void importDisabledBacklogAndClearVanillaChat() {
        List<Component> pending;
        synchronized (DISABLED_BACKLOG) {
            if (DISABLED_BACKLOG.isEmpty()) {
                clearVanillaChatMessages();
                return;
            }

            pending = new ArrayList<>(DISABLED_BACKLOG);
            DISABLED_BACKLOG.clear();
            DISABLED_BACKLOG_IDENTITIES.clear();
        }

        for (Component message : pending) {
            routeIncoming(message, false);
        }

        clearVanillaChatMessages();
    }

    public static void clearVanillaChatMessages() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        com.client.multichatwindows.util.MinecraftGuiAccess.clearChatMessages(false);
    }

    public static void trimServerMessagesToLast(String serverKey, int keep) {
        String key = serverKey == null || serverKey.isBlank() ? currentServerKey() : serverKey;
        int safeKeep = Math.max(0, keep);
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(key);

        if (key.equals(currentServerKey())) {
            for (ChatWindow window : WINDOWS.values()) {
                if (window != null) {
                    window.trimToLastMessages(safeKeep);
                    rewriteHistoryFileFromWindow(key, window);
                }
            }
        }

        if (serverConfig.tabs != null) {
            for (TabConfig tab : serverConfig.tabs) {
                if (tab == null || tab.id == null || tab.id.isBlank()) {
                    continue;
                }
                if (key.equals(currentServerKey()) && WINDOWS.containsKey(tab.id)) {
                    continue;
                }
                trimHistoryFileToLastMessages(key, tab.id, safeKeep);
            }
        }
    }

    public static Collection<ChatWindow> allWindows() {
        ensureForCurrentServer();
        return WINDOWS.values();
    }

    public static String currentServerKey() {
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient == null) {
            return fallbackServerKey("singleplayer");
        }

        ServerData serverInfo = null;
        try {
            serverInfo = minecraftClient.getCurrentServer();
        } catch (Throwable ignored) {
        }
        if (serverInfo == null) {
            serverInfo = findServerData(minecraftClient);
        }

        String key = serverAddress(serverInfo);
        if (key == null || key.isBlank()) {
            return fallbackServerKey("singleplayer");
        }
        return key;
    }

    private static String fallbackServerKey(String fallback) {
        if (lastServerKey != null && !lastServerKey.isBlank()
                && !"singleplayer".equalsIgnoreCase(lastServerKey)
                && !"unknown".equalsIgnoreCase(lastServerKey)) {
            return lastServerKey;
        }
        try {
            List<String> servers = ConfigManager.listServers();
            if (servers != null && servers.size() == 1 && servers.get(0) != null && !servers.get(0).isBlank()) {
                return servers.get(0);
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static ServerData findServerData(Object target) {
        if (target == null) {
            return null;
        }

        for (String methodName : new String[]{"getCurrentServer", "getCurrentServerData", "currentServer", "serverData"}) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof ServerData serverData) {
                    return serverData;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value instanceof ServerData serverData) {
                        return serverData;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static String serverAddress(ServerData serverInfo) {
        if (serverInfo == null) {
            return "";
        }
        if (serverInfo.ip != null && !serverInfo.ip.isBlank()) {
            return serverInfo.ip;
        }
        for (String methodName : new String[]{"ip", "address", "getIp", "getAddress"}) {
            String value = stringFromObjectByMethods(serverInfo, methodName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        for (String fieldName : new String[]{"ip", "address", "serverIp", "serverIP"}) {
            try {
                Field field = serverInfo.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(serverInfo);
                if (value instanceof String string && !string.isBlank()) {
                    return string;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return "";
    }

    public static void ensureForCurrentServer() {
        String key = currentServerKey();
        if (!key.equals(lastServerKey)) {
            rebuildForCurrentServer();
        }
    }

    public static void rebuildForCurrentServer() {
        String key = currentServerKey();
        boolean serverChanged = !key.equals(lastServerKey);
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
            if (serverChanged) {
                window.clearMessages();
            }
            WINDOWS.put(tab.id, window);
        }

        loadHistoryForCurrentServer(key);
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

    public static Component messageAt(double mouseX, double mouseY) {
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

    public static void importExternalMessage(String screenId, Component message) {
        ensureForCurrentServer();
        String serverKey = currentServerKey();
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        String plain = message == null ? "" : message.getString();
        String stackKey = stackKeyFor(false, plain);
        ChatWindow window = WINDOWS.get(screenId);
        if (window == null) {
            window = WINDOWS.get("all");
        }
        if (window != null) {
            window.push(withTimestamp(serverConfig, message), stackKey);
            logRouteDecisionToMinecraftLog("tabs: " + window.displayName, plain);
            notifyIfConfigured(serverKey, screenId, window.displayName, plain);
        } else {
            logRouteDecisionToMinecraftLog("tabs: none", plain);
        }
    }

    public static boolean routeIncoming(Component message) {
        return routeIncoming(message, false);
    }

    public static boolean routeCommandFeedback(Component message) {
        return routeIncoming(message, true);
    }

    public static Component componentFromPlayerChatMessage(PlayerChatMessage message, GameProfile sender, ChatType.Bound bound) {
        Component fromMessage = componentFromObjectByMethods(message,
                "decoratedContent",
                "signedContent",
                "unsignedContent");
        if (fromMessage != null) {
            return fromMessage;
        }

        String content = stringFromObjectByMethods(message, "signedContent", "content");
        if (content == null || content.isBlank()) {
            content = message == null ? "" : String.valueOf(message);
        }

        String senderName = gameProfileName(sender);
        if (senderName != null && !senderName.isBlank() && !content.contains(senderName)) {
            return Component.literal("<" + senderName + "> " + content);
        }
        return Component.literal(content);
    }

    private static String gameProfileName(GameProfile profile) {
        if (profile == null) {
            return "";
        }
        String name = stringFromObjectByMethods(profile, "name", "getName");
        return name == null ? "" : name;
    }

    private static Component componentFromObjectByMethods(Object target, String... methodNames) {
        if (target == null || methodNames == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof Component component) {
                    return component;
                }
                if (value instanceof String string && !string.isBlank()) {
                    return Component.literal(string);
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next known accessor.
            }
        }
        return null;
    }

    private static String stringFromObjectByMethods(Object target, String... methodNames) {
        if (target == null || methodNames == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof String string) {
                    return string;
                }
                if (value instanceof Component component) {
                    return component.getString();
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next known accessor.
            }
        }
        return null;
    }

    public static boolean routeChatHudFeedback(Component message) {
        return routeIncoming(message, shouldBypassChatFiltersForChatHudFeedback());
    }

    public static boolean wasAlreadyRouted(Component message) {
        if (message == null) {
            return false;
        }
        synchronized (ROUTED_COMPONENT_IDENTITIES) {
            return ROUTED_COMPONENT_IDENTITIES.contains(message);
        }
    }

    private static boolean markRoutedComponent(Component message) {
        if (message == null) {
            return true;
        }
        synchronized (ROUTED_COMPONENT_IDENTITIES) {
            if (ROUTED_COMPONENT_IDENTITIES.contains(message)) {
                return false;
            }
            ROUTED_COMPONENT_IDENTITIES.add(message);
            if (ROUTED_COMPONENT_IDENTITIES.size() > 4096) {
                ROUTED_COMPONENT_IDENTITIES.clear();
                ROUTED_COMPONENT_IDENTITIES.add(message);
            }
            return true;
        }
    }

    private static boolean shouldBypassChatFiltersForChatHudFeedback() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        boolean fromMessageHandler = false;
        boolean fromCommand = false;

        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (className == null) {
                continue;
            }

            String lower = className.toLowerCase();
            if (lower.contains("messagehandler") || lower.contains("clientplaynetworkhandler")) {
                fromMessageHandler = true;
            }
            if (lower.contains("command") || lower.contains("clientcommandsource")) {
                fromCommand = true;
            }
        }

        return fromCommand && !fromMessageHandler;
    }

    private static boolean routeIncoming(Component message, boolean bypassChatFilters) {
        GlobalConfig globalConfig = ConfigManager.global();
        String plain = message == null ? "" : message.getString();
        if (message != null && !markRoutedComponent(message)) {
            return true;
        }
        if (!globalConfig.enabled) {
            logRouteDecisionToMinecraftLog("tabs: minecraft chat", plain);
            return false;
        }

        if (shouldPassThroughVanilla(plain)) {
            logRouteDecisionToMinecraftLog("tabs: minecraft chat", plain);
            return false;
        }

        ensureForCurrentServer();
        String serverKey = currentServerKey();
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);

        if (isChatClearPayload(plain)) {
            if (serverConfig.noChatClearing) {
                logRouteDecisionToMinecraftLog("blocked chat clear", plain);
                return true;
            }
            return false;
        }

        if (plain == null || plain.isBlank()) {
            return false;
        }

        String player = guessPlayerName(plain);
        boolean serverMessage = isUnfilterableServerMessage(plain);
        String stackKey = stackKeyFor(serverMessage, plain);

        boolean filterMatch = false;
        String filterReason = "";

        if (!bypassChatFilters && !serverMessage && serverConfig.chatFilters != null) {
            for (ChatFilterRule rule : serverConfig.chatFilters) {
                if (rule != null && rule.matches(player, plain)) {
                    filterMatch = true;
                    filterReason = rule.describe();
                    break;
                }
            }
        }

        if (!bypassChatFilters && !serverMessage && filterMatch) {
            // Filtered messages are hidden and intentionally ignored by stacking.
            // No ChatWindow.push(), no breakStack(), no lastStackKey change.
            EventLog.filtered(serverKey, filterReason, plain);
            logRouteDecisionToMinecraftLog("filtered", plain);
            return true;
        }

        boolean filterFromAll = false;
        boolean routedToCustomTab = false;
        List<String> shownTabs = new ArrayList<>();

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
                        saveHistoryForWindow(serverKey, window);
                        routedToCustomTab = true;
                        addShownTab(shownTabs, window.displayName);
                        DebugLog.write(plain, window.displayName, rule.describe());
                        notifyIfConfigured(serverKey, tab.id, window.displayName, plain);
                    }
                    if (!bypassChatFilters && !serverMessage && tab.filterAllChat) {
                        filterFromAll = true;
                    }
                    break;
                }
            }
        }

        boolean shouldPushToAll = serverMessage
                || bypassChatFilters
                || (!filterFromAll && (!serverConfig.chatFilterAllMode || !routedToCustomTab));
        if (shouldPushToAll) {
            ChatWindow all = WINDOWS.get("all");
            if (all != null) {
                all.push(withTimestamp(serverConfig, message), stackKey);
                saveHistoryForWindow(serverKey, all);
                addShownTab(shownTabs, all.displayName);
                notifyIfConfigured(serverKey, "all", all.displayName, plain);
            }
        }

        if (!bypassChatFilters && !serverMessage && serverConfig.chatFilterAllMode && !routedToCustomTab) {
            EventLog.filtered(serverKey, "allMode:notMatched", plain);
            logRouteDecisionToMinecraftLog("filtered", plain);
        } else {
            logRouteDecisionToMinecraftLog(formatShownTabs(shownTabs), plain);
        }

        return true;
    }


    private static void addShownTab(List<String> shownTabs, String tabName) {
        if (shownTabs == null) {
            return;
        }
        String clean = tabName == null || tabName.isBlank() ? "unknown" : tabName.trim();
        for (String existing : shownTabs) {
            if (existing != null && existing.equalsIgnoreCase(clean)) {
                return;
            }
        }
        shownTabs.add(clean);
    }

    private static String formatShownTabs(List<String> shownTabs) {
        if (shownTabs == null || shownTabs.isEmpty()) {
            return "tabs: none";
        }
        return "tabs: " + String.join(", ", shownTabs);
    }

    private static void logRouteDecisionToMinecraftLog(String prefix, String plain) {
        String cleanPrefix = prefix == null || prefix.isBlank() ? "tabs: none" : prefix.trim();
        String cleanMessage = plain == null ? "" : plain.replace('\n', ' ').replace('\r', ' ');
        MCW_ROUTE_LOGGER.info("[MCW] {}: {}", cleanPrefix, cleanMessage);
    }

    private static String stackKeyFor(boolean serverMessage, String plain) {
        String normalized = stripLeadingTimestampForStacking(plain == null ? "" : plain);
        return serverMessage ? "server:" + normalized : normalized;
    }

    private static String stripLeadingTimestampForStacking(String plain) {
        if (plain == null || plain.isBlank()) {
            return plain == null ? "" : plain;
        }

        String normalized = plain.trim();
        boolean changed;
        do {
            changed = false;

            String next = normalized.replaceFirst("^\\[[^\\]]{1,16}\\]\\s*", "");
            if (!next.equals(normalized)) {
                normalized = next.trim();
                changed = true;
                continue;
            }

            next = normalized.replaceFirst("^\\([^\\)]{1,16}\\)\\s*", "");
            if (!next.equals(normalized)) {
                normalized = next.trim();
                changed = true;
                continue;
            }

            next = normalized.replaceFirst("^(?:\\d{1,2}:\\d{2}(?::\\d{2})?|\\d{1,2}\\.\\d{2}(?:\\.\\d{2})?)(?:\\s*[AP]M)?\\s*[-|>]*\\s*", "");
            if (!next.equals(normalized)) {
                normalized = next.trim();
                changed = true;
            }
        } while (changed);

        return normalized;
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

    private static Component withTimestamp(ServerConfig serverConfig, Component message) {
        Component base = message == null ? Component.empty() : message.copy();
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
        Component timestamp = Component.literal("[" + time + "] ")
                .withStyle(style -> style.withColor(color));

        return Component.empty()
                .append(timestamp)
                .append(base);
    }

    private static boolean isChatClearPayload(String plain) {
        if (plain == null) {
            return false;
        }

        String withoutFormatting = stripMinecraftFormatting(plain);
        if (withoutFormatting.isBlank()) {
            return containsLineBreakOrClearEscape(withoutFormatting) || containsLineBreakOrClearEscape(plain);
        }

        if (isRepeatedBlankChatClear(withoutFormatting)) {
            return true;
        }

        String compact = withoutFormatting
                .replace("\\n", "\n")
                .replace("\\N", "\n")
                .replace("/n", "\n")
                .replace("/N", "\n");
        if (isRepeatedBlankChatClear(compact)) {
            return true;
        }

        String tokenized = withoutFormatting
                .replaceAll("(?i)(?:\\\\n|/n)", "")
                .replaceAll("\\s+", "");
        return withoutFormatting.matches("(?is)^(?:\\s*(?:\\\\n|/n|\\r|\\n)\\s*){2,}$")
                || tokenized.isEmpty() && containsLineBreakOrClearEscape(withoutFormatting);
    }

    private static boolean containsLineBreakOrClearEscape(String plain) {
        if (plain == null) {
            return false;
        }
        return plain.indexOf('\n') >= 0
                || plain.indexOf('\r') >= 0
                || plain.toLowerCase().contains("/n")
                || plain.toLowerCase().contains("\\n");
    }

    private static String stripMinecraftFormatting(String plain) {
        if (plain == null || plain.isEmpty()) {
            return "";
        }
        return plain.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    private static boolean isRepeatedBlankChatClear(String plain) {
        if (plain == null || plain.isEmpty()) {
            return false;
        }

        int newlineCount = 0;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == '\n' || c == '\r') {
                newlineCount++;
                continue;
            }
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }

        return newlineCount > 1;
    }

    private static void loadHistoryForCurrentServer(String serverKey) {
        if (!ConfigManager.global().chatHistoryEnabled) {
            return;
        }

        for (ChatWindow window : WINDOWS.values()) {
            if (window == null || !window.entries.isEmpty()) {
                continue;
            }

            List<String> historyLines = readHistoryLines(serverKey, window.id);
            window.restoreJsonHistory(historyLines);
            replayHistoryDebugAndEventLog(serverKey, window, historyLines);
        }
    }

    private static void replayHistoryDebugAndEventLog(String serverKey, ChatWindow window, List<String> historyLines) {
        if (!ConfigManager.global().chatHistoryEnabled || window == null || historyLines == null || historyLines.isEmpty()) {
            return;
        }

        for (String json : historyLines) {
            if (json == null || json.isBlank()) {
                continue;
            }

            Component restored = com.client.multichatwindows.util.TextJsonUtil.fromJson(json);
            String plain = restored == null ? "" : restored.getString();
            if (plain == null || plain.isBlank()) {
                continue;
            }

            DebugLog.write(plain, window.displayName, "history restore");
            EventLog.log("HISTORY", serverKey, window.id, "history restore", plain);
        }
    }

    private static void saveHistoryForWindow(String serverKey, ChatWindow window) {
        if (window == null || !ConfigManager.global().chatHistoryEnabled) {
            return;
        }

        try {
            java.nio.file.Path file = historyFile(serverKey, window.id);
            java.nio.file.Files.createDirectories(file.getParent());

            boolean fullRewrite = window.consumeHistoryFullRewriteRequired() || !java.nio.file.Files.exists(file);
            if (fullRewrite) {
                List<String> encodedLines = new ArrayList<>();
                for (String line : window.snapshotJsonHistory()) {
                    if (line == null || line.isBlank()) {
                        continue;
                    }
                    encodedLines.add("A:" + java.util.Base64.getEncoder().encodeToString(line.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                }

                java.nio.file.Files.write(
                        file,
                        encodedLines,
                        java.nio.charset.StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                        java.nio.file.StandardOpenOption.WRITE
                );
                return;
            }

            boolean replaceLatest = window.consumeHistoryReplaceLatestRequired();
            String latest = window.latestJsonHistoryLine();
            if (latest == null || latest.isBlank()) {
                return;
            }

            String encodedLine = (replaceLatest ? "R:" : "A:")
                    + java.util.Base64.getEncoder().encodeToString(latest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.nio.file.Files.write(
                    file,
                    java.util.List.of(encodedLine),
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
            if (window != null) {
                window.markHistoryFullRewriteRequired();
            }
        }
    }

    private static void rewriteHistoryFileFromWindow(String serverKey, ChatWindow window) {
        if (window == null || !ConfigManager.global().chatHistoryEnabled) {
            return;
        }

        try {
            java.nio.file.Path file = historyFile(serverKey, window.id);
            java.nio.file.Files.createDirectories(file.getParent());

            List<String> encodedLines = new ArrayList<>();
            for (String line : window.snapshotJsonHistory()) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                encodedLines.add("A:" + java.util.Base64.getEncoder().encodeToString(line.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }

            java.nio.file.Files.write(
                    file,
                    encodedLines,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE
            );
        } catch (Exception ignored) {
            window.markHistoryFullRewriteRequired();
        }
    }

    private static void trimHistoryFileToLastMessages(String serverKey, String windowId, int keep) {
        if (!ConfigManager.global().chatHistoryEnabled) {
            return;
        }

        try {
            List<String> lines = readHistoryLines(serverKey, windowId);
            int safeKeep = Math.max(0, keep);
            int start = Math.max(0, lines.size() - safeKeep);
            List<String> kept = lines.subList(start, lines.size());

            java.nio.file.Path file = historyFile(serverKey, windowId);
            java.nio.file.Files.createDirectories(file.getParent());

            List<String> encodedLines = new ArrayList<>();
            for (String line : kept) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                encodedLines.add("A:" + java.util.Base64.getEncoder().encodeToString(line.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }

            java.nio.file.Files.write(
                    file,
                    encodedLines,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE
            );
        } catch (Exception ignored) {
        }
    }

    private static List<String> readHistoryLines(String serverKey, String windowId) {
        try {
            java.nio.file.Path file = historyFile(serverKey, windowId);
            if (!java.nio.file.Files.exists(file)) {
                return List.of();
            }

            List<String> encodedLines = java.nio.file.Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8);
            List<String> decodedLines = new ArrayList<>();
            for (String encodedLine : encodedLines) {
                if (encodedLine == null || encodedLine.isBlank()) {
                    continue;
                }

                String trimmed = encodedLine.trim();
                boolean replaceLatest = false;
                if (trimmed.startsWith("A:") || trimmed.startsWith("R:")) {
                    replaceLatest = trimmed.startsWith("R:");
                    trimmed = trimmed.substring(2);
                }

                String decoded;
                try {
                    byte[] bytes = java.util.Base64.getDecoder().decode(trimmed);
                    decoded = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                    decoded = trimmed;
                }

                if (decoded == null || decoded.isBlank()) {
                    continue;
                }

                if (replaceLatest && !decodedLines.isEmpty()) {
                    decodedLines.set(decodedLines.size() - 1, decoded);
                } else {
                    decodedLines.add(decoded);
                }
            }
            return decodedLines;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static java.nio.file.Path historyFile(String serverKey, String windowId) {
        String safeServer = ConfigManager.safe(serverKey);
        String safeWindow = ConfigManager.safe(windowId == null ? "all" : windowId);
        return ConfigManager.basePath().resolve("history").resolve(safeServer).resolve(safeWindow + ".txt");
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

            NotificationSoundPlayer.playNotification(notification);

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

        if (handleVanillaTextClick(style)) {
            return true;
        }

        String value = ChatWindow.extractClickValue(style.getClickEvent());
        if (value == null || value.isEmpty()) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }

        switch (style.getClickEvent().action()) {
            case OPEN_URL:
                return ChatWindow.openUrl(value);

            case OPEN_FILE:
                return ChatWindow.openFile(value);

            case RUN_COMMAND:
                if (client.player != null && client.player.connection != null) {
                    if (value.startsWith("/")) {
                        client.player.connection.sendCommand(value.substring(1));
                    } else {
                        client.player.connection.sendChat(value);
                    }
                    return true;
                }
                return false;

            case SUGGEST_COMMAND:
                if (suggestionSink != null && suggestionSink.setSuggestion(value)) {
                    return true;
                }
                com.client.multichatwindows.util.MinecraftGuiAccess.addRecentChat(value);
                return true;

            case COPY_TO_CLIPBOARD:
                client.keyboardHandler.setClipboard(value);
                return true;

            default:
                return false;
        }
    }

    private static boolean handleVanillaTextClick(Style style) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || com.client.multichatwindows.util.MinecraftGuiAccess.currentScreen() == null || style == null) {
            return false;
        }

        Class<?> type = com.client.multichatwindows.util.MinecraftGuiAccess.currentScreen().getClass();
        while (type != null) {
            for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals("handleTextClick")) {
                    continue;
                }
                if (method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameterType = method.getParameterTypes()[0];
                if (!parameterType.isAssignableFrom(Style.class)) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    Object result = method.invoke(com.client.multichatwindows.util.MinecraftGuiAccess.currentScreen(), style);
                    return result instanceof Boolean value && value;
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }

        return false;
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
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
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
