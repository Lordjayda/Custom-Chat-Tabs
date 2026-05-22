package com.client.multichatwindows.config.model;

import java.util.ArrayList;
import java.util.List;

public class NotificationConfig {
    public String id = "";
    public boolean enabled = true;

    // Wenn true, gilt die Notification nur auf targetScreens
    public boolean screenDependent = false;
    public List<String> targetScreens = new ArrayList<>();

    // Mode:
    // - anyMessage = true  => jede Nachricht auf dem (gefilterten) Screen triggert
    // - keyword != ""      => nur wenn Nachricht keyword enthält (case-insensitive)
    public boolean anyMessage = true;
    public String keyword = "";

    // Optional: reuse deiner bestehenden Dependency-Regeln (falls du später erweitern willst)
    public List<DependencyRule> dependencies = new ArrayList<>();

    public boolean keywordMatches(String messagePlain) {
        if (keyword == null || keyword.isBlank()) return false;
        if (messagePlain == null) return false;
        return messagePlain.toLowerCase().contains(keyword.trim().toLowerCase());
    }
}