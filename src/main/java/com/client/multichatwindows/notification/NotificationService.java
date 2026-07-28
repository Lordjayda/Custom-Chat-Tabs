package com.client.multichatwindows.notification;

import com.client.multichatwindows.config.model.DependencyRule;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.util.EventLog;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class NotificationService {
    private NotificationService() {
    }

    private static final Map<String, Long> lastFireMs = new HashMap<>();
    private static final long COOLDOWN_MS = 400;

    public static void onMessageRouted(
            String serverKey,
            ServerConfig serverConfig,
            String tabId,
            String tabName,
            String player,
            String plain
    ) {
        if (serverConfig == null || serverConfig.notifications == null || serverConfig.notifications.isEmpty()) {
            return;
        }

        for (NotificationConfig notification : serverConfig.notifications) {
            if (notification == null || !notification.enabled) {
                continue;
            }

            if (notification.screenDependent && !targetScreenMatches(notification, tabId, tabName)) {
                continue;
            }

            boolean dependencyOk = true;
            if (notification.dependencies != null && !notification.dependencies.isEmpty()) {
                dependencyOk = false;
                for (DependencyRule rule : notification.dependencies) {
                    if (rule != null && rule.matches(player, plain)) {
                        dependencyOk = true;
                        break;
                    }
                }
            }
            if (!dependencyOk) {
                continue;
            }

            boolean match;
            String reason;
            if (notification.keyword != null && !notification.keyword.isBlank()) {
                match = notification.keywordMatches(plain);
                reason = "keyword:" + notification.keyword.trim();
            } else {
                match = notification.anyMessage;
                reason = "anyMessage";
            }
            if (!match) {
                continue;
            }

            long now = System.currentTimeMillis();
            String key = notification.id == null ? "" : notification.id;
            Long last = lastFireMs.get(key);
            if (last != null && (now - last) < COOLDOWN_MS) {
                continue;
            }
            lastFireMs.put(key, now);

            EventLog.notified(serverKey, tabName, reason, plain);
            NotificationSoundPlayer.playNotification(notification);

            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                return;
            }

            Component title = Component.translatable("multichatwindows.notifications.toast.title");
            Component description;
            if (notification.keyword != null && !notification.keyword.isBlank()) {
                description = Component.translatable("multichatwindows.notifications.toast.keyword", notification.keyword.trim(), tabName);
            } else {
                description = Component.translatable("multichatwindows.notifications.toast.message", tabName);
            }

            client.execute(() -> {
                try {
                    client.gui.toastManager().addToast(new SystemToast(SystemToast.SystemToastId.NARRATOR_TOGGLE, title, description));
                } catch (Throwable ignored) {
                }
            });
        }
    }


    private static boolean targetScreenMatches(NotificationConfig notification, String tabId, String tabName) {
        if (notification == null || notification.targetScreens == null || notification.targetScreens.isEmpty()) {
            // Do not make legacy configs silently disable notifications just because the 26.2 screen lookup changed.
            return true;
        }
        for (String target : notification.targetScreens) {
            if (target == null || target.isBlank()) {
                continue;
            }
            String trimmed = target.trim();
            if ("all".equalsIgnoreCase(trimmed)
                    || equalsIgnoreCase(trimmed, tabId)
                    || equalsIgnoreCase(trimmed, tabName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
