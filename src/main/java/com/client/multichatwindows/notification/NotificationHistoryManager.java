package com.client.multichatwindows.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NotificationHistoryManager {
    private static final List<NotificationEntry> HISTORY = new ArrayList<>();

    private NotificationHistoryManager() {
    }

    public static void add(NotificationEntry entry) {
        if (entry == null) {
            return;
        }
        HISTORY.add(0, entry);
        while (HISTORY.size() > 200) {
            HISTORY.remove(HISTORY.size() - 1);
        }
    }

    public static List<NotificationEntry> all() {
        return Collections.unmodifiableList(HISTORY);
    }
}
