package com.client.multichatwindows.config.model;

public class GlobalConfig {
    public int version = 8;
    public boolean enabled = true;
    public Language language = Language.EN_US;
    public boolean autoAddServer = true;
    public boolean debug = false;
    public boolean chatHistoryEnabled = false;

    public boolean configBackgroundEnabled = false;
    public String configBackgroundPath = "";
    public float configBackgroundOpacity = 0.35f;
}
