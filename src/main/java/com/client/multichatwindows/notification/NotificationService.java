package com.client.multichatwindows.notification;

import com.client.multichatwindows.config.model.DependencyRule;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.util.EventLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public final class NotificationService {
    private NotificationService() {}

    // sehr simples Anti-Spam (pro Notification-ID)
    private static final Map<String, Long> lastFireMs = new HashMap<>();
    private static final long COOLDOWN_MS = 400;

    public static void onMessageRouted(
            String serverKey,
            ServerConfig sc,
            String tabId,
            String tabName,
            String player,
            String plain
    ) {
        if (sc == null || sc.notifications == null || sc.notifications.isEmpty()) return;

        for (NotificationConfig n : sc.notifications) {
            if (n == null || !n.enabled) continue;

            if (n.screenDependent) {
                if (n.targetScreens == null || !n.targetScreens.contains(tabId)) {
                    continue;
                }
            }

            boolean depOk = true;
            if (n.dependencies != null && !n.dependencies.isEmpty()) {
                depOk = false;
                for (DependencyRule r : n.dependencies) {
                    if (r != null && r.matches(player, plain)) {
                        depOk = true;
                        break;
                    }
                }
            }
            if (!depOk) continue;

            boolean match = false;
            String reason;

            if (n.keyword != null && !n.keyword.isBlank()) {
                match = n.keywordMatches(plain);
                reason = "keyword:" + n.keyword.trim();
            } else {
                match = n.anyMessage;
                reason = "anyMessage";
            }

            if (!match) continue;

            long now = System.currentTimeMillis();
            String key = (n.id == null ? "" : n.id);
            Long last = lastFireMs.get(key);
            if (last != null && (now - last) < COOLDOWN_MS) continue;
            lastFireMs.put(key, now);

            // LOG (wichtig!)
            EventLog.notified(serverKey, tabName, reason, plain);

            // Toast (kein Chat!)
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) return;

            Text title = Text.translatable("multichatwindows.notifications.toast.title");
            Text desc;
            if (n.keyword != null && !n.keyword.isBlank()) {
                desc = Text.translatable("multichatwindows.notifications.toast.keyword", n.keyword.trim(), tabName);
            } else {
                desc = Text.translatable("multichatwindows.notifications.toast.message", tabName);
            }

            SystemToast.show(mc.getToastManager(), SystemToast.Type.NARRATOR_TOGGLE, title, desc);

            // kleiner Sound (nutzt ein Event, das du bereits im Projekt nutzt)
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}