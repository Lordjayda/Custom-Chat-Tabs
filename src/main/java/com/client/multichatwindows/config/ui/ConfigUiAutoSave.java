package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.hud.WindowService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigUiAutoSave {
    private ConfigUiAutoSave() {
    }

    public static void commit(Screen screen) {
        if (screen == null) {
            return;
        }

        ServerConfig serverConfig = null;
        String serverKey = null;
        try {
            serverConfig = findFieldOfType(screen, ServerConfig.class);
            serverKey = findStringField(screen, "serverKey");
            invokeApplyMethods(screen, serverConfig);
        } catch (Throwable ignored) {
        }

        try {
            ConfigManager.saveGlobal();
        } catch (Throwable ignored) {
        }

        try {
            if (serverKey != null && serverConfig != null) {
                ConfigManager.saveServer(serverKey, serverConfig);
                WindowService.rebuildForCurrentServer();
            }
        } catch (Throwable ignored) {
        }
    }

    public static void renderInputTooltip(Screen screen, GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (screen == null || context == null) {
            return;
        }

        EditBox hovered = findHoveredTextField(screen, mouseX, mouseY);
        if (hovered == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return;
        }

        Component fieldLabel = hovered.getMessage();
        context.setTooltipForNextFrame(
                client.font,
                Component.translatable("multichatwindows.tooltip.input.autosave", fieldLabel),
                mouseX,
                mouseY
        );
    }

    private static void invokeApplyMethods(Object owner, ServerConfig serverConfig) {
        Class<?> type = owner.getClass();
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                String name = method.getName();
                if (!"apply".equals(name) && !"liveApply".equals(name)) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    Class<?>[] parameters = method.getParameterTypes();
                    if (parameters.length == 0) {
                        method.invoke(owner);
                    } else if (parameters.length == 1 && serverConfig != null && parameters[0].isAssignableFrom(ServerConfig.class)) {
                        method.invoke(owner, serverConfig);
                    }
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
    }

    private static EditBox findHoveredTextField(Object owner, int mouseX, int mouseY) {
        Class<?> type = owner.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(owner);
                    if (value instanceof EditBox widget && widget.isMouseOver(mouseX, mouseY)) {
                        return widget;
                    }
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static String findStringField(Object owner, String fieldName) throws IllegalAccessException {
        Class<?> type = owner.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!fieldName.equals(field.getName()) || field.getType() != String.class) {
                    continue;
                }
                field.setAccessible(true);
                return (String) field.get(owner);
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static <T> T findFieldOfType(Object owner, Class<T> fieldType) throws IllegalAccessException {
        Class<?> type = owner.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!fieldType.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(owner);
                if (fieldType.isInstance(value)) {
                    return fieldType.cast(value);
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }
}
