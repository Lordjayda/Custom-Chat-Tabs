package com.client.multichatwindows.config.model;

public class ChatFilterRule {
    // true = Playername matchen, false = Message-Text matchen
    public boolean player = true;
    public String value = "";

    public boolean matches(String playerName, String messagePlain) {
        if (value == null || value.isBlank()) return false;
        String v = value.trim();
        if (player) {
            return playerName != null && playerName.equalsIgnoreCase(v);
        }
        return messagePlain != null && messagePlain.toLowerCase().contains(v.toLowerCase());
    }

    public String describe() {
        return (player ? "player:" : "msg:") + (value == null ? "" : value);
    }
}