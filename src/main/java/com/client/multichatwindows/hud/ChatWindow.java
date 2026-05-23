package com.client.multichatwindows.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ChatWindow {

    private static final int MAX_MESSAGES = 2000; // ✅ mehr History (Nachrichten, nicht Zeilen)
    private static final int PADDING = 2;

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

    /**
     * scrollOffset ist in WRAPPED-LINES gemessen (so wie im Render-Slice gearbeitet wird).
     */
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

        // User ist "live" unten, wenn offset == 0
        boolean atBottom = scrollOffset == 0;

        // Wenn NICHT unten: wir müssen den offset um die neuen WRAPPED-LINES erhöhen,
        // damit der sichtbare Ausschnitt exakt gleich bleibt.
        int addedWrapped = 0;
        if (!atBottom) {
            addedWrapped = countWrappedLines(msg);
            // Falls wrapLines aus irgendeinem Grund 0 liefert, mindestens 1 Zeile annehmen
            if (addedWrapped <= 0) addedWrapped = 1;
        }

        lines.add(msg);

        // ✅ History-Limit (Nachrichten)
        if (lines.size() > MAX_MESSAGES) {
            int removeCount = lines.size() - MAX_MESSAGES;

            // Wenn wir oben Nachrichten entfernen, müssen wir scrollOffset (wrapped-lines) ebenfalls korrigieren,
            // sonst springt es später.
            if (scrollOffset > 0) {
                int removedWrapped = 0;
                TextRenderer tr = MinecraftClient.getInstance().textRenderer;
                int maxTextWidth = calcMaxTextWidth();

                for (int i = 0; i < removeCount; i++) {
                    Text old = lines.get(i);
                    List<OrderedText> parts = tr.wrapLines(old, maxTextWidth);
                    removedWrapped += Math.max(1, parts.size());
                }

                scrollOffset = Math.max(0, scrollOffset - removedWrapped);
            }

            // Entfernen
            lines.subList(0, removeCount).clear();
        }

        if (atBottom) {
            // ✅ live unten bleiben
            scrollOffset = 0;
        } else {
            // ✅ sichtbaren Ausschnitt halten: offset in wrapped-lines erhöhen
            scrollOffset += addedWrapped;
        }
    }

    private int countWrappedLines(Text msg) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int maxTextWidth = calcMaxTextWidth();
        List<OrderedText> parts = tr.wrapLines(msg, maxTextWidth);
        return parts == null ? 1 : Math.max(1, parts.size());
    }

    private int calcMaxTextWidth() {
        float scale = Math.max(0.5f, Math.min(3.0f, this.textScale));
        // exakt wie beim Render: Breite minus padding, dann durch scale
        return Math.max(20, (int) ((this.w - PADDING * 2) / scale));
    }
}