package com.client.multichatwindows.config.model;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {
    public int version = 7;
    public String serverKey = "";

    public List<TabConfig> tabs = new ArrayList<>();

    // 1.2.0 additions
    public List<NotificationConfig> notifications = new ArrayList<>();
    public List<ChatFilterRule> chatFilters = new ArrayList<>();
}