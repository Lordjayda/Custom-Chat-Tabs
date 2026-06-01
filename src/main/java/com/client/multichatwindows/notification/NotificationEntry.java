package com.client.multichatwindows.notification;

import net.minecraft.text.Text;

public class NotificationEntry {
    public final String id;
    public final String serverKey;
    public final String screenId;
    public final String screenName;
    public final String reason;
    public final String message;
    public final Text formattedMessage;
    public final long timeMs;

    public NotificationEntry(String id, String serverKey, String screenId, String screenName, String reason, String message, long timeMs) {
        this(id, serverKey, screenId, screenName, reason, message, Text.literal(message == null ? "" : message), timeMs);
    }

    public NotificationEntry(String id, String serverKey, String screenId, String screenName, String reason, String message, Text formattedMessage, long timeMs) {
        this.id = id == null ? "" : id;
        this.serverKey = serverKey == null ? "" : serverKey;
        this.screenId = screenId == null ? "" : screenId;
        this.screenName = screenName == null ? "" : screenName;
        this.reason = reason == null ? "" : reason;
        this.message = message == null ? "" : message;
        this.formattedMessage = formattedMessage == null ? Text.literal(this.message) : formattedMessage.copy();
        this.timeMs = timeMs;
    }
}
