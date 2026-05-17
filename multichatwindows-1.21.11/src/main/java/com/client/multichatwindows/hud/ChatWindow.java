package com.client.multichatwindows.hud;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ChatWindow {
    public final String id;

    public String displayName;
    public boolean enabled;
    public boolean useVanilla;

    public int x;
    public int y;
    public int w;
    public int h;

    public float opacity;
    public float textScale = 1.0f;

    public int scrollOffset = 0;

    public boolean outlineEnabled = false;
    public int outlineWidth = 1;
    public int outlineColor = 0xFFFFFF;

    public final List<Text> lines = new ArrayList<>();

    public ChatWindow(String id) {
        this.id = id;
    }

    public void push(Text msg) {
        if (msg == null) return;

        lines.add(msg);

        if (lines.size() > 200) {
            lines.subList(0, lines.size() - 200).clear();
        }

        scrollOffset = 0;
	}
}
	