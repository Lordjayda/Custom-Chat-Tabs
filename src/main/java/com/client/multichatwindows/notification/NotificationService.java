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

            if (notification.screenDependent) {
                if (notification.targetScreens == null || !notification.targetScreens.contains(tabId)) {
                    continue;
                }
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

            client.execute(() -> SystemToast.addOrUpdate(client.getToastManager(), SystemToast.SystemToastId.NARRATOR_TOGGLE, title, description));
        }
    }
}
