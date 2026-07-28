package com.client.multichatwindows.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

public final class MinecraftGuiAccess {
    private MinecraftGuiAccess() {
    }

    public static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return;
        }
        client.gui.setScreen(screen);
    }

    public static Screen currentScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return null;
        }

        Object gui = client.gui;
        Object result = invokeNoArg(gui, "currentScreen");
        if (result instanceof Screen screen) {
            return screen;
        }
        result = invokeNoArg(gui, "getCurrentScreen");
        if (result instanceof Screen screen) {
            return screen;
        }
        result = invokeNoArg(gui, "getScreen");
        if (result instanceof Screen screen) {
            return screen;
        }
        result = readField(gui, "currentScreen");
        if (result instanceof Screen screen) {
            return screen;
        }
        result = readField(gui, "screen");
        if (result instanceof Screen screen) {
            return screen;
        }

        for (Field field : allFields(gui.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(gui);
                if (value instanceof Screen screen) {
                    return screen;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        return null;
    }

    public static boolean isChatScreenOpen() {
        return currentScreen() instanceof ChatScreen;
    }

    public static boolean isHudHidden() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return false;
        }

        Object options = client.options;
        Object value = readField(options, "hideGui");
        if (value instanceof Boolean hidden) {
            return hidden;
        }
        value = invokeNoArg(options, "hideGui");
        if (value instanceof Boolean hidden) {
            return hidden;
        }
        value = invokeNoArg(options, "getHideGui");
        if (value instanceof Boolean hidden) {
            return hidden;
        }

        for (Method method : allMethods(options.getClass())) {
            if (method.getParameterCount() != 0 || (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class)) {
                continue;
            }
            String name = method.getName().toLowerCase(java.util.Locale.ROOT);
            if (!name.contains("hide") || !name.contains("gui")) {
                continue;
            }
            Object result = invoke(options, method, new Object[0]);
            if (result instanceof Boolean hidden) {
                return hidden;
            }
        }
        return false;
    }

    public static void clearChatMessages(boolean clearHistory) {
        Object chat = chatComponent();
        if (chat != null) {
            invokeCompatible(chat, "clearMessages", new Object[]{clearHistory});
        }
    }

    public static void addRecentChat(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Object chat = chatComponent();
        if (chat != null) {
            invokeCompatible(chat, "addRecentChat", new Object[]{message});
        }
    }

    private static Object chatComponent() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return null;
        }
        Object gui = client.gui;

        Object result = invokeNoArg(gui, "getChat");
        if (isLikelyChatComponent(result)) {
            return result;
        }
        result = invokeNoArg(gui, "chat");
        if (isLikelyChatComponent(result)) {
            return result;
        }
        result = invokeNoArg(gui, "getChatComponent");
        if (isLikelyChatComponent(result)) {
            return result;
        }
        result = readField(gui, "chat");
        if (isLikelyChatComponent(result)) {
            return result;
        }
        result = readField(gui, "chatComponent");
        if (isLikelyChatComponent(result)) {
            return result;
        }

        for (Field field : allFields(gui.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(gui);
                if (isLikelyChatComponent(value)) {
                    return value;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static boolean isLikelyChatComponent(Object value) {
        if (value == null) {
            return false;
        }
        for (Method method : allMethods(value.getClass())) {
            String name = method.getName();
            if (("clearMessages".equals(name) && method.getParameterCount() == 1)
                    || ("addRecentChat".equals(name) && method.getParameterCount() == 1)) {
                return true;
            }
        }
        return value.getClass().getName().toLowerCase(java.util.Locale.ROOT).contains("chat");
    }

    private static Object readField(Object target, String name) {
        if (target == null || name == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String name) {
        if (target == null || name == null) {
            return null;
        }
        for (Method method : allMethods(target.getClass())) {
            if (method.getParameterCount() == 0 && method.getName().equals(name)) {
                return invoke(target, method, new Object[0]);
            }
        }
        return null;
    }

    private static Object invokeCompatible(Object target, String name, Object[] args) {
        if (target == null || name == null || args == null) {
            return null;
        }
        for (Method method : allMethods(target.getClass())) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
                continue;
            }
            return invoke(target, method, args);
        }
        return null;
    }

    private static Object invoke(Object target, Method method, Object[] args) {
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static java.util.List<Field> allFields(Class<?> type) {
        java.util.ArrayList<Field> result = new java.util.ArrayList<>();
        Class<?> current = type;
        while (current != null) {
            result.addAll(java.util.Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return result;
    }

    private static java.util.List<Method> allMethods(Class<?> type) {
        java.util.ArrayList<Method> result = new java.util.ArrayList<>();
        Class<?> current = type;
        while (current != null) {
            result.addAll(java.util.Arrays.asList(current.getDeclaredMethods()));
            current = current.getSuperclass();
        }
        return result;
    }
}
