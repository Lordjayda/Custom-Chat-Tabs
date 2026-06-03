package com.client.multichatwindows.config.model;

import java.util.ArrayList;
import java.util.List;

public class TabConfig {
    public String id = "";
    public String name = "";
    public boolean enabled = true;

    public int x = 5;
    public int y = 5;
    public int width = 320;
    public int height = 180;

    public float opacity = 0.8f;
    public float textScale = 1.0f;

    public boolean useVanilla = true;
    public boolean filterAllChat = false;

    public boolean outlineEnabled = false;
    public int outlineWidth = 1;
    public String outlineColor = "FFFFFF";

    public List<DependencyRule> dependencies = new ArrayList<>();
}
