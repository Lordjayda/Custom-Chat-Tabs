package com.client.multichatwindows.config.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ChatActionMenuScreen extends DarkScreen {
    private final Screen parent;
    private final Component message;

    public ChatActionMenuScreen(Screen parent, Component message) {
        super(Component.translatable("multichatwindows.chat_actions.title"));
        this.parent = parent;
        this.message = message;
    }

    @Override
    protected void init() {
        clearWidgets();
        int centerX = width / 2;

        addRenderableWidget(new DarkButton(
                centerX - 100,
                70,
                200,
                20,
                Component.translatable("multichatwindows.chat_actions.copy"),
                () -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(message == null ? "" : message.getString());
                    com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent);
                }
        ));

        addRenderableWidget(new DarkButton(
                centerX - 100,
                96,
                200,
                20,
                Component.translatable("multichatwindows.back"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent)
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.translatable("multichatwindows.chat_actions.title"), width / 2, 14, 0xFFFFFFFF);

        String preview = message == null ? "" : message.getString();
        if (preview.length() > 70) {
            preview = preview.substring(0, 67) + "...";
        }
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font, Component.literal(preview), width / 2, 42, 0xFFB0B0B0);
    }
}
