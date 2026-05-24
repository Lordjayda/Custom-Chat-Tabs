package com.client.multichatwindows.config.model;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {
    public int version = 13;
    public String serverKey = "";

    public boolean chatFilterAllMode = false;

    public List<TabConfig> tabs = new ArrayList<>();
    public List<NotificationConfig> notifications = new ArrayList<>();
    public List<ChatFilterRule> chatFilters = new ArrayList<>();

    public int notificationX = -1;
    public int notificationY = 12;
    public int notificationWidth = 220;
    public float notificationTextScale = 1.0f;
    public int notificationMaxVisible = 5;
}
