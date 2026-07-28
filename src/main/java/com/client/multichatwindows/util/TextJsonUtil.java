package com.client.multichatwindows.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class TextJsonUtil {
    private static final Gson GSON = new Gson();

    private TextJsonUtil() {
    }

    public static String toJson(Component text) {
        if (text == null) {
            return "{\"text\":\"\"}";
        }

        // Prefer Minecraft's own serializers/codecs. They preserve real ClickEvent and
        // HoverEvent payloads much better than a manual tree dump, especially for
        // show_item/show_entity hover data.
        String nativeJson = tryNativeSerializationToJson(text);
        if (nativeJson != null && !nativeJson.isBlank()) {
            return nativeJson;
        }

        String codecJson = tryCodecToJson(text);
        if (codecJson != null && !codecJson.isBlank()) {
            return codecJson;
        }

        return GSON.toJson(toJsonObject(text));
    }

    public static Component fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Component.empty();
        }

        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json);
        } catch (Exception ignored) {
            return Component.literal(json);
        }

        Component nativeText = tryNativeSerializationFromJson(json);
        if (nativeText != null) {
            return nativeText;
        }

        Component codecText = tryCodecFromJson(GSON.toJson(withMinecraftAliases(parsed)));
        if (codecText != null) {
            return codecText;
        }

        // Fallback: restore color/siblings and simple show_text/run_command/suggest_command events manually.
        try {
            return fromJsonElement(parsed);
        } catch (Exception ignored) {
            return Component.literal(json);
        }
    }

    private static JsonObject toJsonObject(Component text) {
        JsonObject object = new JsonObject();
        object.addProperty("text", ownText(text));
        addStyle(object, text == null ? Style.EMPTY : text.getStyle());

        List<Component> siblings = text == null ? List.of() : text.getSiblings();
        if (siblings != null && !siblings.isEmpty()) {
            JsonArray extra = new JsonArray();
            for (Component sibling : siblings) {
                extra.add(toJsonObject(sibling));
            }
            object.add("extra", extra);
        }
        return object;
    }

    private static Component fromJsonElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Component.empty();
        }
        if (element.isJsonPrimitive()) {
            return Component.literal(element.getAsString());
        }
        if (element.isJsonArray()) {
            MutableComponent combined = Component.empty();
            for (JsonElement child : element.getAsJsonArray()) {
                combined.append(fromJsonElement(child));
            }
            return combined;
        }

        JsonObject object = element.getAsJsonObject();
        MutableComponent result = Component.literal(getString(object, "text", ""));
        result.setStyle(styleFromJson(object));

        JsonElement extra = object.get("extra");
        if (extra != null && extra.isJsonArray()) {
            for (JsonElement child : extra.getAsJsonArray()) {
                result.append(fromJsonElement(child));
            }
        }
        return result;
    }

    private static String ownText(Component text) {
        if (text == null) {
            return "";
        }
        String full = text.getString();
        StringBuilder childText = new StringBuilder();
        for (Component sibling : text.getSiblings()) {
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
            String name = color.serialize();
            object.addProperty("color", name == null || name.isBlank() ? String.format("#%06X", color.getValue() & 0xFFFFFF) : name);
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
            clickObject.addProperty("action", actionName(click.action()));
            String value = clickValue(click);
            if (value != null) {
                clickObject.addProperty("value", value);
                if (click.action() == ClickEvent.Action.SUGGEST_COMMAND || click.action() == ClickEvent.Action.RUN_COMMAND) {
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
            style = style.withUnderlined(true);
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
            ChatFormatting formatting = ChatFormatting.valueOf(color.toUpperCase(java.util.Locale.ROOT));
            return formatting == null ? null : TextColor.fromLegacyFormat(formatting);
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
            return constructHoverEvent("net.minecraft.network.chat.HoverEvent$ShowText", fromJsonElement(value));
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
        Object action = invokeZeroArg(hover, "action", "getAction");
        Object value = hoverValue(hover, action);
        if (value instanceof Component text) {
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
        for (String accessor : new String[]{"value", "item", "entity", "getValue"}) {
            for (Method method : hover.getClass().getMethods()) {
                if (!method.getName().equals(accessor)) {
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
        }
        for (Field field : hover.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(hover);
                if (value instanceof Component || value != null) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String tryNativeSerializationToJson(Component text) {
        try {
            Class<?> type = Class.forName("net.minecraft.network.chat.ComponentSerialization");
            for (Method method : type.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                String name = method.getName().toLowerCase(java.util.Locale.ROOT);
                if (!name.contains("tojson")) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                Object result = null;
                if (params.length == 1 && params[0].isInstance(text)) {
                    result = method.invoke(null, text);
                } else if (params.length == 2 && params[0].isInstance(text)) {
                    Object lookup = registryLookup();
                    if (lookup != null && params[1].isInstance(lookup)) {
                        result = method.invoke(null, text, lookup);
                    }
                }
                if (result instanceof String string && !string.isBlank()) {
                    return string;
                }
                if (result instanceof JsonElement element) {
                    return GSON.toJson(element);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Component tryNativeSerializationFromJson(String json) {
        try {
            Class<?> type = Class.forName("net.minecraft.network.chat.ComponentSerialization");
            JsonElement element = JsonParser.parseString(json);
            for (Method method : type.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                String name = method.getName().toLowerCase(java.util.Locale.ROOT);
                if (!name.contains("fromjson") && !name.contains("deserialize")) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                Object result = null;
                if (params.length == 1) {
                    if (params[0] == String.class) {
                        result = method.invoke(null, json);
                    } else if (params[0].isInstance(element)) {
                        result = method.invoke(null, element);
                    }
                } else if (params.length == 2) {
                    Object lookup = registryLookup();
                    if (lookup == null || !params[1].isInstance(lookup)) {
                        continue;
                    }
                    if (params[0] == String.class) {
                        result = method.invoke(null, json, lookup);
                    } else if (params[0].isInstance(element)) {
                        result = method.invoke(null, element, lookup);
                    }
                }
                if (result instanceof Component text) {
                    return text;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String tryCodecToJson(Component text) {
        try {
            Object codec = textCodec();
            Object ops = jsonOpsWithRegistries();
            if (codec == null || ops == null || text == null) {
                return null;
            }
            Method encodeStart = findAnyMethod(codec.getClass(), "encodeStart", 2);
            if (encodeStart == null) {
                return null;
            }
            Object dataResult = encodeStart.invoke(codec, ops, text);
            Object result = dataResultResult(dataResult);
            if (result instanceof JsonElement element) {
                return GSON.toJson(element);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Component tryCodecFromJson(String json) {
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
            return result instanceof Component text ? text : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object textCodec() {
        for (String className : new String[]{
                "net.minecraft.network.chat.ComponentSerialization",
                "net.minecraft.text.TextCodecs"
        }) {
            try {
                Class<?> type = Class.forName(className);
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
        }
        return null;
    }

    private static Object jsonOpsWithRegistries() {
        Object jsonOps = jsonOps();
        Object lookup = registryLookup();
        if (jsonOps != null && lookup != null) {
            try {
                Class<?> registryOps = Class.forName("net.minecraft.resources.RegistryOps");
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
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.level != null) {
                Object lookup = invokeZeroArg(client.level, "getRegistryManager");
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

    private static Object invokeZeroArg(Object target, String... methodNames) {
        if (target == null || methodNames == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    return method.invoke(target);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
