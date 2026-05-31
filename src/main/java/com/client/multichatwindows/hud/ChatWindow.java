package com.client.multichatwindows.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatWindow {

    public static final int PADDING_X = 2;
    public static final int PADDING_Y = 2;
    public static final int MAX_MESSAGES = 10000;

    public static final List<ChatWindow> INSTANCES = new ArrayList<>();

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
    public final List<Entry> entries = new ArrayList<>();

    private Text lastOriginalMessage = null;
    private String lastSender = null;

    public ChatWindow(String id) {
        this.id = id;
        if (!INSTANCES.contains(this)) {
            INSTANCES.add(this);
        }
    }

    public void push(Text msg) {
        push(msg, false);
    }

    public void push(Text msg, boolean duplicateIgnored) {
        if (msg == null) {
            return;
        }

        String sender = extractSender(msg);

        if (!entries.isEmpty()
                && isSameMessage(lastOriginalMessage, msg)
                && equalsSafe(lastSender, sender)) {

            Entry last = entries.get(entries.size() - 1);
            last.repeatCount++;

            rebuildRenderedLines();
            clampScrollOffset();

            lastOriginalMessage = msg.copy();
            lastSender = sender;
            return;
        }

        entries.add(new Entry(msg.copy(), 1));

        if (entries.size() > MAX_MESSAGES) {
            int remove = entries.size() - MAX_MESSAGES;
            entries.subList(0, remove).clear();
        }

        lastOriginalMessage = msg.copy();
        lastSender = sender;

        rebuildRenderedLines();
        clampScrollOffset();
    }

    private void rebuildRenderedLines() {
        lines.clear();

        for (Entry entry : entries) {
            lines.add(buildRenderText(entry));
        }
    }

    public List<RowRef> visibleRows() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) {
            return List.of();
        }

        TextRenderer tr = mc.textRenderer;
        List<RowRef> all = allRows(tr);

        if (all.isEmpty()) {
            return List.of();
        }

        float scale = clamp(textScale);

        int visibleLines = Math.max(
                1,
                (h - PADDING_Y * 2) / scaledLineHeight(tr, scale)
        );

        int maxScroll = Math.max(0, all.size() - visibleLines);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int endExclusive = all.size() - scrollOffset;
        int startInclusive = Math.max(0, endExclusive - visibleLines);

        return new ArrayList<>(all.subList(startInclusive, endExclusive));
    }

    public int totalRowCount() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) {
            return Math.max(0, lines.size());
        }

        return allRows(mc.textRenderer).size();
    }

    public int visibleRowCapacity() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) {
            return 1;
        }

        return Math.max(1, (h - PADDING_Y * 2) / scaledLineHeight(mc.textRenderer, clamp(textScale)));
    }

    public int maxScrollOffset() {
        return Math.max(0, totalRowCount() - visibleRowCapacity());
    }

    public void clampScrollOffset() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
    }

    private List<RowRef> allRows(TextRenderer tr) {
        int maxWidth = Math.max(
                20,
                (int) ((w - PADDING_X * 2) / clamp(textScale))
        );

        List<RowRef> rows = new ArrayList<>();

        for (Entry entry : entries) {
            Text renderText = buildRenderText(entry);
            List<OrderedText> wrapped = tr.wrapLines(renderText, maxWidth);

            if (wrapped == null || wrapped.isEmpty()) {
                OrderedText ordered = OrderedText.styledForwardsVisitedString(
                        renderText.getString(),
                        renderText.getStyle()
                );

                rows.add(new RowRef(
                        entry.original,
                        ordered,
                        buildGlyphRuns(tr, ordered)
                ));
            } else {
                for (OrderedText ordered : wrapped) {
                    rows.add(new RowRef(
                            entry.original,
                            ordered,
                            buildGlyphRuns(tr, ordered)
                    ));
                }
            }
        }

        return rows;
    }

    private static Text buildRenderText(Entry entry) {
        Text base = entry.original.copy();

        if (entry.repeatCount <= 1) {
            return base;
        }

        return base.copy().append(
                Text.literal(" (x" + entry.repeatCount + ")")
                        .styled(s -> s.withColor(0x55FF55))
        );
    }

    private static List<GlyphRun> buildGlyphRuns(TextRenderer tr, OrderedText ordered) {
        List<GlyphRun> runs = new ArrayList<>();

        if (ordered == null) {
            return runs;
        }

        final int[] cursorX = {0};
        final Style[] currentStyle = {null};
        final int[] runStart = {0};

        ordered.accept((index, style, codePoint) -> {
            OrderedText single = OrderedText.styled(codePoint, style);
            int width = Math.max(0, tr.getWidth(single));

            if (!Objects.equals(currentStyle[0], style)) {
                if (currentStyle[0] != null && cursorX[0] > runStart[0]) {
                    runs.add(new GlyphRun(
                            runStart[0],
                            cursorX[0],
                            currentStyle[0]
                    ));
                }

                currentStyle[0] = style;
                runStart[0] = cursorX[0];
            }

            cursorX[0] += width;
            return true;
        });

        if (currentStyle[0] != null && cursorX[0] > runStart[0]) {
            runs.add(new GlyphRun(
                    runStart[0],
                    cursorX[0],
                    currentStyle[0]
            ));
        }

        return runs;
    }

    public boolean contains(double mouseX, double mouseY) {
        return enabled
                && mouseX >= x && mouseX <= x + w
                && mouseY >= y && mouseY <= y + h;
    }

    public RowRef rowAt(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null || !contains(mouseX, mouseY)) {
            return null;
        }

        List<RowRef> rows = visibleRows();
        if (rows.isEmpty()) {
            return null;
        }

        float scale = clamp(textScale);
        int lineHeight = scaledLineHeight(mc.textRenderer, scale);
        double renderedTop = renderedFirstLineY(rows.size(), lineHeight, scale);

        for (int i = 0; i < rows.size(); i++) {
            double rowTop = renderedTop + i * lineHeight;
            double rowBottom = rowTop + lineHeight;

            if (mouseY >= rowTop && mouseY < rowBottom) {
                return rows.get(i);
            }
        }

        return null;
    }

    public Style styleAt(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) {
            return null;
        }

        RowRef row = rowAt(mouseX, mouseY);
        if (row == null) {
            return null;
        }

        float scale = clamp(textScale);
        double renderedLeft = renderedTextLeft(scale);
        int localX = (int) Math.floor((mouseX - renderedLeft) / scale);

        if (localX < 0) {
            return null;
        }

        Style vanillaStyle = resolveStyleWithTextHandler(mc.textRenderer, row.ordered, localX);
        if (vanillaStyle != null) {
            return vanillaStyle;
        }

        for (GlyphRun run : row.glyphRuns) {
            if (localX >= run.startX && localX < run.endX) {
                return run.style;
            }
        }

        return null;
    }

    public boolean handleClick(double mouseX, double mouseY) {
        Style style = styleAt(mouseX, mouseY);
        if (style == null) {
            return false;
        }

        ClickEvent click = style.getClickEvent();
        if (click == null) {
            return false;
        }

        String value = extractClickValue(click);
        if (value == null || value.isEmpty()) {
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return false;
        }

        switch (click.getAction()) {
            case OPEN_URL:
                return openUrl(value);

            case RUN_COMMAND:
                if (mc.player != null && mc.player.networkHandler != null) {
                    mc.player.networkHandler.sendChatCommand(
                            value.replaceFirst("^/", "")
                    );
                    return true;
                }
                return false;

            case SUGGEST_COMMAND:
                if (mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
                    mc.inGameHud.getChatHud().addToMessageHistory(value);
                    return true;
                }
                return false;

            case COPY_TO_CLIPBOARD:
                mc.keyboard.setClipboard(value);
                return true;

            default:
                return false;
        }
    }

    public Text getHoverText(double mouseX, double mouseY) {
        Style style = styleAt(mouseX, mouseY);
        if (style == null) {
            return null;
        }

        HoverEvent hover = style.getHoverEvent();
        if (hover == null) {
            return null;
        }

        Text text = tryExtractHoverTextViaMethod(hover);
        if (text != null) {
            return text;
        }

        text = tryExtractHoverTextViaFields(hover);
        if (text != null) {
            return text;
        }

        return null;
    }

    public static boolean openUrl(String value) {
        String url = normalizeUrl(value);
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            URI uri = URI.create(url);
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(uri);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Object os = net.minecraft.util.Util.getOperatingSystem();
            for (Method method : os.getClass().getMethods()) {
                if (!method.getName().equals("open") || method.getParameterCount() != 1) {
                    continue;
                }

                Class<?> type = method.getParameterTypes()[0];
                if (type == URI.class) {
                    method.invoke(os, URI.create(url));
                    return true;
                }
                if (type == String.class) {
                    method.invoke(os, url);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            net.minecraft.util.Util.getOperatingSystem().open(url);
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    private static String normalizeUrl(String value) {
        if (value == null) {
            return null;
        }

        String url = value.trim();
        if (url.isEmpty()) {
            return null;
        }

        if (url.startsWith("<") && url.endsWith(">") && url.length() > 2) {
            url = url.substring(1, url.length() - 1).trim();
        }

        if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            url = "https://" + url;
        }

        return url;
    }

    public static String extractClickValue(ClickEvent click) {
        if (click == null) {
            return null;
        }

        String methodValue = extractClickValueViaNamedMethods(click);
        if (isUsableClickValue(methodValue)) {
            return methodValue;
        }

        String fieldValue = extractClickValueViaNamedFields(click);
        if (isUsableClickValue(fieldValue)) {
            return fieldValue;
        }

        String fallbackValue = extractClickValueViaToString(click);
        if (isUsableClickValue(fallbackValue)) {
            return fallbackValue;
        }

        return null;
    }

    private static String extractClickValueViaNamedMethods(ClickEvent click) {
        String[] preferredNames = new String[]{
                "getCommand",
                "command",
                "getUrl",
                "url",
                "getUri",
                "uri",
                "getValue",
                "value",
                "getFile",
                "file",
                "getPath",
                "path"
        };

        for (String name : preferredNames) {
            String value = extractClickValueFromMethod(click, name);
            if (isUsableClickValue(value)) {
                return value;
            }
        }

        try {
            for (Method method : click.getClass().getDeclaredMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }

                String name = method.getName();
                if (isRejectedClickMethodName(name)) {
                    continue;
                }

                method.setAccessible(true);
                Object value = method.invoke(click);
                String converted = clickValueToString(value);
                if (isUsableClickValue(converted)) {
                    return converted;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            for (Method method : click.getClass().getMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }

                String name = method.getName();
                if (isRejectedClickMethodName(name)) {
                    continue;
                }

                Object value = method.invoke(click);
                String converted = clickValueToString(value);
                if (isUsableClickValue(converted)) {
                    return converted;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String extractClickValueFromMethod(ClickEvent click, String methodName) {
        try {
            Method method = click.getClass().getDeclaredMethod(methodName);
            if (method.getParameterCount() != 0) {
                return null;
            }

            method.setAccessible(true);
            Object value = method.invoke(click);
            return clickValueToString(value);
        } catch (Exception ignored) {
        }

        try {
            Method method = click.getClass().getMethod(methodName);
            if (method.getParameterCount() != 0) {
                return null;
            }

            Object value = method.invoke(click);
            return clickValueToString(value);
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String extractClickValueViaNamedFields(ClickEvent click) {
        Class<?> current = click.getClass();

        while (current != null) {
            String[] preferredNames = new String[]{
                    "command",
                    "url",
                    "uri",
                    "value",
                    "file",
                    "path"
            };

            for (String name : preferredNames) {
                try {
                    Field field = current.getDeclaredField(name);
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    field.setAccessible(true);
                    Object value = field.get(click);
                    String converted = clickValueToString(value);
                    if (isUsableClickValue(converted)) {
                        return converted;
                    }
                } catch (Exception ignored) {
                }
            }

            current = current.getSuperclass();
        }

        current = click.getClass();

        while (current != null) {
            try {
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    String name = field.getName();
                    if (isRejectedClickFieldName(name)) {
                        continue;
                    }

                    field.setAccessible(true);
                    Object value = field.get(click);
                    String converted = clickValueToString(value);
                    if (isUsableClickValue(converted)) {
                        return converted;
                    }
                }
            } catch (Exception ignored) {
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private static String extractClickValueViaToString(ClickEvent click) {
        String raw = String.valueOf(click);
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        String[] keys = new String[]{
                "command",
                "url",
                "uri",
                "value",
                "file",
                "path"
        };

        for (String key : keys) {
            String value = extractKeyFromToString(raw, key);
            if (isUsableClickValue(value)) {
                return value;
            }
        }

        return null;
    }

    private static String extractKeyFromToString(String raw, String key) {
        String[] needles = new String[]{
                key + "=",
                key + ":",
                key + "='",
                key + "=\""
        };

        for (String needle : needles) {
            int idx = raw.indexOf(needle);
            if (idx < 0) {
                continue;
            }

            int start = idx + needle.length();

            if (needle.endsWith("'")) {
                int end = raw.indexOf("'", start);
                if (end > start) {
                    return raw.substring(start, end);
                }
                continue;
            }

            if (needle.endsWith("\"")) {
                int end = raw.indexOf("\"", start);
                if (end > start) {
                    return raw.substring(start, end);
                }
                continue;
            }

            while (start < raw.length() && Character.isWhitespace(raw.charAt(start))) {
                start++;
            }

            if (start < raw.length() && raw.charAt(start) == '\'') {
                start++;
                int end = raw.indexOf("'", start);
                if (end > start) {
                    return raw.substring(start, end);
                }
            }

            if (start < raw.length() && raw.charAt(start) == '"') {
                start++;
                int end = raw.indexOf("\"", start);
                if (end > start) {
                    return raw.substring(start, end);
                }
            }

            int end = raw.length();

            int comma = raw.indexOf(",", start);
            if (comma >= 0) {
                end = Math.min(end, comma);
            }

            int bracket = raw.indexOf("]", start);
            if (bracket >= 0) {
                end = Math.min(end, bracket);
            }

            int paren = raw.indexOf(")", start);
            if (paren >= 0) {
                end = Math.min(end, paren);
            }

            if (end > start) {
                return raw.substring(start, end).trim();
            }
        }

        return null;
    }

    private static boolean isRejectedClickMethodName(String name) {
        if (name == null) {
            return true;
        }

        return name.equals("getAction")
                || name.equals("action")
                || name.equals("getClass")
                || name.equals("hashCode")
                || name.equals("toString")
                || name.equals("equals")
                || name.equals("getCodec")
                || name.equals("codec");
    }

    private static boolean isRejectedClickFieldName(String name) {
        if (name == null) {
            return true;
        }

        return name.equalsIgnoreCase("action")
                || name.equalsIgnoreCase("codec")
                || name.equalsIgnoreCase("CODEC")
                || name.equalsIgnoreCase("UNVALIDATED_CODEC");
    }

    private static boolean isUsableClickValue(String value) {
        if (value == null) {
            return false;
        }

        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return false;
        }

        return !isActionName(cleaned);
    }

    private static boolean isActionName(String value) {
        if (value == null) {
            return false;
        }

        String cleaned = value.trim().toLowerCase();

        return cleaned.equals("open_url")
                || cleaned.equals("open_file")
                || cleaned.equals("run_command")
                || cleaned.equals("suggest_command")
                || cleaned.equals("change_page")
                || cleaned.equals("copy_to_clipboard")
                || cleaned.equals("show_dialog")
                || cleaned.equals("custom");
    }

    private static String clickValueToString(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String string) {
            return string;
        }

        if (value instanceof URI || value instanceof URL || value instanceof File || value instanceof Path) {
            return value.toString();
        }

        String className = value.getClass().getName();
        if (className.contains("ClickEvent")
                || className.contains("Action")
                || className.contains("Codec")
                || className.contains("MapCodec")
                || className.contains("DataResult")) {
            return null;
        }

        return value.toString();
    }

    private Text tryExtractHoverTextViaMethod(HoverEvent hover) {
        Text result = null;

        try {
            Method getAction = hover.getClass().getMethod("getAction");
            Object action = getAction.invoke(hover);

            for (Method m : hover.getClass().getMethods()) {
                if (!m.getName().equals("getValue")) {
                    continue;
                }
                if (m.getParameterCount() != 1) {
                    continue;
                }

                Object value = m.invoke(hover, action);
                if (value instanceof Text text) {
                    result = text;
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    private Text tryExtractHoverTextViaFields(HoverEvent hover) {
        Text result = null;

        try {
            for (Field f : hover.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                f.setAccessible(true);
                Object v = f.get(hover);
                if (v instanceof Text text) {
                    result = text;
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    private static Style resolveStyleWithTextHandler(TextRenderer textRenderer, OrderedText ordered, int localX) {
        if (textRenderer == null || ordered == null) {
            return null;
        }

        try {
            Object handler = textRenderer.getTextHandler();
            if (handler == null) {
                return null;
            }

            for (Method method : handler.getClass().getMethods()) {
                if (!method.getName().equals("getStyleAt")) {
                    continue;
                }
                if (method.getParameterCount() != 2) {
                    continue;
                }

                Class<?>[] types = method.getParameterTypes();
                if (!types[0].isAssignableFrom(OrderedText.class) && !OrderedText.class.isAssignableFrom(types[0])) {
                    continue;
                }
                if (types[1] != int.class && types[1] != Integer.class) {
                    continue;
                }

                Object value = method.invoke(handler, ordered, localX);
                if (value instanceof Style style) {
                    return style;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private double renderedTextLeft(float scale) {
        return ((int) ((x + PADDING_X) / scale)) * scale;
    }

    private double renderedFirstLineY(int visibleRowCount, int lineHeight, float scale) {
        int bottomY = y + h - PADDING_Y - lineHeight;
        int firstY = bottomY - Math.max(0, visibleRowCount - 1) * lineHeight;
        return ((int) (firstY / scale)) * scale;
    }

    public static int scaledLineHeight(TextRenderer tr, float scale) {
        return Math.max(1, (int) Math.ceil(tr.fontHeight * scale));
    }

    private static float clamp(float scale) {
        return Math.max(0.5f, Math.min(3.0f, scale));
    }

    private String extractSender(Text text) {
        if (text == null) {
            return null;
        }

        String raw = text.getString();
        int idx = raw.indexOf(':');

        if (idx <= 0) {
            return null;
        }

        String pre = raw.substring(0, idx).trim();
        pre = pre.replace("<", "")
                .replace(">", "")
                .replace("[", "")
                .replace("]", "");

        if (pre.isEmpty()) {
            return null;
        }

        String[] parts = pre.split("\\s+");
        return parts.length == 0 ? pre : parts[parts.length - 1];
    }

    private boolean isSameMessage(Text a, Text b) {
        if (a == null || b == null) {
            return false;
        }

        return a.getString().equals(b.getString());
    }

    private boolean equalsSafe(Object a, Object b) {
        return Objects.equals(a, b);
    }

    public static final class Entry {
        public final Text original;
        public int repeatCount;

        public Entry(Text original, int repeatCount) {
            this.original = original == null ? Text.empty() : original;
            this.repeatCount = Math.max(1, repeatCount);
        }
    }

    public static final class RowRef {
        public final Text source;
        public final OrderedText ordered;
        public final List<GlyphRun> glyphRuns;

        public RowRef(Text source, OrderedText ordered, List<GlyphRun> glyphRuns) {
            this.source = source == null ? Text.empty() : source;
            this.ordered = ordered;
            this.glyphRuns = glyphRuns == null ? List.of() : glyphRuns;
        }
    }

    public static final class GlyphRun {
        public final int startX;
        public final int endX;
        public final Style style;

        public GlyphRun(int startX, int endX, Style style) {
            this.startX = startX;
            this.endX = endX;
            this.style = style == null ? Style.EMPTY : style;
        }
    }
}