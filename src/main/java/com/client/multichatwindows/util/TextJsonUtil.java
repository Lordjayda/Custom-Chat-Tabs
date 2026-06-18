package com.client.multichatwindows.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class TextJsonUtil {
    private static final Gson GSON = new Gson();

    private TextJsonUtil() {
    }

    public static String toJson(Text text) {
        if (text == null) {
            return "{\"text\":\"\"}";
        }

        // Use our own tree dump as canonical storage/copy format so all siblings/colors
        // are preserved even when Minecraft's codec falls back to a plain text object.
        return GSON.toJson(toJsonObject(text));
    }

    public static Text fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Text.empty();
        }

        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json);
        } catch (Exception ignored) {
            return Text.literal(json);
        }

        // First try Minecraft's real codec with camelCase aliases. This is the best path
        // for restoring real HoverEvent/ClickEvent objects, including more complex hover payloads.
        Text codecText = tryCodecFromJson(GSON.toJson(withMinecraftAliases(parsed)));
        if (codecText != null && !codecText.getString().trim().equals(json.trim())) {
            return codecText;
        }

        // Fallback: restore color/siblings and simple show_text/run_command/suggest_command events manually.
        try {
            return fromJsonElement(parsed);
        } catch (Exception ignored) {
            return Text.literal(json);
        }
    }

    private static JsonObject toJsonObject(Text text) {
        JsonObject object = new JsonObject();
        object.addProperty("text", ownText(text));
        addStyle(object, text == null ? Style.EMPTY : text.getStyle());

        List<Text> siblings = text == null ? List.of() : text.getSiblings();
        if (siblings != null && !siblings.isEmpty()) {
            JsonArray extra = new JsonArray();
            for (Text sibling : siblings) {
                extra.add(toJsonObject(sibling));
            }
            object.add("extra", extra);
        }
        return object;
    }

    private static Text fromJsonElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Text.empty();
        }
        if (element.isJsonPrimitive()) {
            return Text.literal(element.getAsString());
        }
        if (element.isJsonArray()) {
            MutableText combined = Text.empty();
            for (JsonElement child : element.getAsJsonArray()) {
                combined.append(fromJsonElement(child));
            }
            return combined;
        }

        JsonObject object = element.getAsJsonObject();
        MutableText result = Text.literal(getString(object, "text", ""));
        result.setStyle(styleFromJson(object));

        JsonElement extra = object.get("extra");
        if (extra != null && extra.isJsonArray()) {
            for (JsonElement child : extra.getAsJsonArray()) {
                result.append(fromJsonElement(child));
            }
        }
        return result;
    }

    private static String ownText(Text text) {
        if (text == null) {
            return "";
        }
        String full = text.getString();
        StringBuilder childText = new StringBuilder();
        for (Text sibling : text.getSiblings()) {
            childText.append(sibling.getString());
        }
        String suffix = childText.toString();
        if (!suffix.isEmpty() && full.endsWith(suffix)) {
            return full.substring(0, full.length() - suffix.length());
        }
        return full;
    }

    private static void addStyle(JsonObject object, Style style) {
        if (style == null) {
            return;
        }

        TextColor color = style.getColor();
        if (color != null) {
            String name = color.getName();
            object.addProperty("color", name == null || name.isBlank() ? String.format("#%06X", color.getRgb() & 0xFFFFFF) : name);
        }
        object.addProperty("bold", style.isBold());
        object.addProperty("italic", style.isItalic());
        object.addProperty("underlined", style.isUnderlined());
        object.addProperty("strikethrough", style.isStrikethrough());
        object.addProperty("obfuscated", style.isObfuscated());
        if (style.getInsertion() != null) {
            object.addProperty("insertion", style.getInsertion());
        }

        ClickEvent click = style.getClickEvent();
        if (click != null) {
            JsonObject clickObject = new JsonObject();
            clickObject.addProperty("action", actionName(click.getAction()));
            String value = clickValue(click);
            if (value != null) {
                clickObject.addProperty("value", value);
                if (click.getAction() == ClickEvent.Action.SUGGEST_COMMAND || click.getAction() == ClickEvent.Action.RUN_COMMAND) {
                    clickObject.addProperty("command", value);
                }
            }
            object.add("click_event", clickObject);
            object.add("clickEvent", clickObject);
        }

        HoverEvent hover = style.getHoverEvent();
        if (hover != null) {
            JsonObject hoverObject = hoverToJson(hover);
            if (hoverObject != null) {
                object.add("hover_event", hoverObject);
                object.add("hoverEvent", hoverObject);
            }
        }
    }

    private static Style styleFromJson(JsonObject object) {
        Style style = Style.EMPTY;

        TextColor color = parseColor(getString(object, "color", null));
        if (color != null) {
            style = style.withColor(color);
        }
        if (getBoolean(object, "bold")) {
            style = style.withBold(true);
        }
        if (getBoolean(object, "italic")) {
            style = style.withItalic(true);
        }
        if (getBoolean(object, "underlined")) {
            style = style.withUnderline(true);
        }
        if (getBoolean(object, "strikethrough")) {
            style = style.withStrikethrough(true);
        }
        if (getBoolean(object, "obfuscated")) {
            style = style.withObfuscated(true);
        }
        String insertion = getString(object, "insertion", null);
        if (insertion != null) {
            style = style.withInsertion(insertion);
        }

        ClickEvent clickEvent = clickEventFromJson(getObject(object, "click_event", "clickEvent"));
        if (clickEvent != null) {
            style = style.withClickEvent(clickEvent);
        }

        HoverEvent hoverEvent = hoverEventFromJson(getObject(object, "hover_event", "hoverEvent"));
        if (hoverEvent != null) {
            style = style.withHoverEvent(hoverEvent);
        }
        return style;
    }

    private static TextColor parseColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        try {
            if (color.startsWith("#")) {
                return TextColor.fromRgb(Integer.parseInt(color.substring(1), 16));
            }
        } catch (Exception ignored) {
        }
        try {
            Formatting formatting = Formatting.byName(color);
            return formatting == null ? null : TextColor.fromFormatting(formatting);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonElement withMinecraftAliases(JsonElement element) {
        if (element == null || element.isJsonNull() || element.isJsonPrimitive()) {
            return element;
        }
        if (element.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                copy.add(withMinecraftAliases(child));
            }
            return copy;
        }

        JsonObject source = element.getAsJsonObject();
        JsonObject copy = new JsonObject();
        for (String key : source.keySet()) {
            copy.add(key, withMinecraftAliases(source.get(key)));
        }

        if (copy.has("click_event") && !copy.has("clickEvent")) {
            copy.add("clickEvent", normalizeClickForMinecraft(copy.get("click_event")));
        }
        if (copy.has("hover_event") && !copy.has("hoverEvent")) {
            copy.add("hoverEvent", normalizeHoverForMinecraft(copy.get("hover_event")));
        }
        return copy;
    }

    private static JsonElement normalizeClickForMinecraft(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return element;
        }
        JsonObject source = element.getAsJsonObject();
        JsonObject copy = new JsonObject();
        for (String key : source.keySet()) {
            copy.add(key, source.get(key));
        }
        if (!copy.has("value") && copy.has("command")) {
            copy.add("value", copy.get("command"));
        }
        return copy;
    }

    private static JsonElement normalizeHoverForMinecraft(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return element;
        }
        JsonObject source = element.getAsJsonObject();
        JsonObject copy = new JsonObject();
        for (String key : source.keySet()) {
            copy.add(key, withMinecraftAliases(source.get(key)));
        }
        if (!copy.has("contents") && copy.has("value")) {
            copy.add("contents", copy.get("value"));
        }
        return copy;
    }

    private static ClickEvent clickEventFromJson(JsonObject object) {
        if (object == null) {
            return null;
        }
        String action = getString(object, "action", "").toLowerCase(java.util.Locale.ROOT);
        String value = getString(object, "command", null);
        if (value == null) {
            value = getString(object, "value", null);
        }
        if (value == null) {
            return null;
        }

        String className = switch (action) {
            case "run_command", "runcommand" -> "net.minecraft.text.ClickEvent$RunCommand";
            case "suggest_command", "suggestcommand" -> "net.minecraft.text.ClickEvent$SuggestCommand";
            case "copy_to_clipboard", "copytoclipboard" -> "net.minecraft.text.ClickEvent$CopyToClipboard";
            case "open_url", "openurl" -> "net.minecraft.text.ClickEvent$OpenUrl";
            case "open_file", "openfile" -> "net.minecraft.text.ClickEvent$OpenFile";
            default -> null;
        };
        if (className == null) {
            return null;
        }
        try {
            Class<?> type = Class.forName(className);
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (constructor.getParameterCount() != 1) {
                    continue;
                }
                constructor.setAccessible(true);
                Object arg = convertConstructorArg(constructor.getParameterTypes()[0], value);
                if (arg != null) {
                    Object event = constructor.newInstance(arg);
                    if (event instanceof ClickEvent clickEvent) {
                        return clickEvent;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static HoverEvent hoverEventFromJson(JsonObject object) {
        if (object == null) {
            return null;
        }
        String action = getString(object, "action", "").toLowerCase(java.util.Locale.ROOT);
        JsonElement value = object.get("value");
        if (value == null) {
            value = object.get("contents");
        }
        if (value == null) {
            return null;
        }
        if (action.isBlank() || action.equals("show_text") || action.equals("showtext")) {
            return constructHoverEvent("net.minecraft.text.HoverEvent$ShowText", fromJsonElement(value));
        }
        return null;
    }

    private static HoverEvent constructHoverEvent(String className, Object value) {
        try {
            Class<?> type = Class.forName(className);
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].isInstance(value)) {
                    constructor.setAccessible(true);
                    Object event = constructor.newInstance(value);
                    if (event instanceof HoverEvent hoverEvent) {
                        return hoverEvent;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Object convertConstructorArg(Class<?> type, String value) {
        try {
            if (type == String.class) {
                return value;
            }
            if (type == URI.class) {
                return URI.create(value);
            }
            if (type == Path.class) {
                return Path.of(value);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String actionName(Object action) {
        if (action == null) {
            return "";
        }
        String name = action.toString().toLowerCase(java.util.Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
    }

    private static String clickValue(ClickEvent click) {
        String[] methodNames = {"getCommand", "command", "getUrl", "url", "getUri", "uri", "getValue", "value", "getFile", "file", "getPath", "path", "getText", "text"};
        for (String methodName : methodNames) {
            try {
                Method method = click.getClass().getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    Object value = method.invoke(click);
                    if (value != null) {
                        return String.valueOf(value);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        for (Field field : click.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(click);
                if (value instanceof String || value instanceof URI || value instanceof Path) {
                    return String.valueOf(value);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static JsonObject hoverToJson(HoverEvent hover) {
        JsonObject object = new JsonObject();
        Object action = invokeZeroArg(hover, "getAction");
        Object value = hoverValue(hover, action);
        if (value instanceof Text text) {
            object.addProperty("action", "show_text");
            object.add("value", toJsonObject(text));
            object.add("contents", toJsonObject(text));
        } else if (value != null) {
            object.addProperty("action", actionName(action));
            object.addProperty("value", String.valueOf(value));
            object.addProperty("contents", String.valueOf(value));
        } else {
            object.addProperty("action", actionName(action));
        }
        return object;
    }

    private static Object hoverValue(HoverEvent hover, Object action) {
        for (Method method : hover.getClass().getMethods()) {
            if (!method.getName().equals("getValue")) {
                continue;
            }
            try {
                if (method.getParameterCount() == 0) {
                    return method.invoke(hover);
                }
                if (method.getParameterCount() == 1 && action != null) {
                    return method.invoke(hover, action);
                }
            } catch (Exception ignored) {
            }
        }
        for (Field field : hover.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(hover);
                if (value instanceof Text) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Text tryCodecFromJson(String json) {
        try {
            Object codec = textCodec();
            Object ops = jsonOpsWithRegistries();
            if (codec == null || ops == null) {
                return null;
            }
            Method parse = findAnyMethod(codec.getClass(), "parse", 2);
            if (parse == null) {
                return null;
            }
            Object dataResult = parse.invoke(codec, ops, JsonParser.parseString(json));
            Object result = dataResultResult(dataResult);
            return result instanceof Text text ? text : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object textCodec() {
        try {
            Class<?> type = Class.forName("net.minecraft.text.TextCodecs");
            for (String name : new String[]{"CODEC", "TEXT_CODEC", "STRINGIFIED_CODEC"}) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null) {
                        return value;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Object jsonOpsWithRegistries() {
        Object jsonOps = jsonOps();
        Object lookup = registryLookup();
        if (jsonOps != null && lookup != null) {
            try {
                Class<?> registryOps = Class.forName("net.minecraft.registry.RegistryOps");
                for (Method method : registryOps.getMethods()) {
                    if (Modifier.isStatic(method.getModifiers()) && method.getName().equals("of") && method.getParameterCount() == 2) {
                        Class<?>[] params = method.getParameterTypes();
                        if (params[0].isInstance(jsonOps) && params[1].isInstance(lookup)) {
                            return method.invoke(null, jsonOps, lookup);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return jsonOps;
    }

    private static Object jsonOps() {
        try {
            Class<?> type = Class.forName("com.mojang.serialization.JsonOps");
            Field field = type.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object dataResultResult(Object dataResult) {
        if (dataResult == null) {
            return null;
        }
        try {
            Object optional = dataResult.getClass().getMethod("result").invoke(dataResult);
            if (optional instanceof Optional<?> opt && opt.isPresent()) {
                return opt.get();
            }
        } catch (Exception ignored) {
        }
        try {
            return dataResult.getClass().getMethod("getOrThrow").invoke(dataResult);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findAnyMethod(Class<?> type, String name, int params) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == params) {
                return method;
            }
        }
        return null;
    }

    private static Object registryLookup() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.world != null) {
                Object lookup = invokeZeroArg(client.world, "getRegistryManager");
                if (lookup != null) {
                    return lookup;
                }
            }
            if (client != null) {
                Object handler = invokeZeroArg(client, "getNetworkHandler");
                Object lookup = invokeZeroArg(handler, "getRegistryManager");
                if (lookup != null) {
                    return lookup;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static JsonObject getObject(JsonObject object, String first, String second) {
        JsonElement element = object.get(first);
        if (element == null) {
            element = object.get(second);
        }
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static boolean getBoolean(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsBoolean();
    }

    private static Object invokeZeroArg(Object target, String name) {
        if (target == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                try {
                    return method.invoke(target);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
