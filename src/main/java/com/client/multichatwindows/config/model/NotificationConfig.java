package com.client.multichatwindows.config.model;

import java.util.ArrayList;
import java.util.List;

public class NotificationConfig {
    public String id = "";
    public boolean enabled = true;

    public boolean screenDependent = false;
    public List<String> targetScreens = new ArrayList<>();

    public boolean anyMessage = true;
    public String keyword = "";
    public List<DependencyRule> dependencies = new ArrayList<>();

    public boolean soundEnabled = true;
    public String soundName = "default_fsharp_hay";
    public String customSoundPath = "";
    public float volume = 1.0f;
    public float pitch = 1.4142135f;

    public boolean keywordMatches(String messagePlain) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        if (messagePlain == null) {
            return false;
        }
        return messagePlain.toLowerCase().contains(keyword.trim().toLowerCase());
    }
}
