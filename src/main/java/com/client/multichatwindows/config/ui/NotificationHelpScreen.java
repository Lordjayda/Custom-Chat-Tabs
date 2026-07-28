package com.client.multichatwindows.config.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotificationHelpScreen extends ScrollableDarkScreen {
    private final Screen parent;

    public NotificationHelpScreen(Screen parent) {
        super(Component.translatable("multichatwindows.notifications.help"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        addRenderableWidget(new DarkButton(
                width / 2 - 100, height - 28, 200, 20,
                Component.translatable("multichatwindows.back"),
                () -> com.client.multichatwindows.util.MinecraftGuiAccess.setScreen(parent)
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font,
                Component.translatable("multichatwindows.notifications.help"),
                width / 2, 14, 0xFFFFFFFF);

        int y = 44;
        com.client.multichatwindows.util.GuiDrawHelper.text(ctx, font,
                Component.translatable("multichatwindows.notifications.help.line1"),
                20, y, 0xFFFFFFFF);
        y += 14;
        com.client.multichatwindows.util.GuiDrawHelper.text(ctx, font,
                Component.translatable("multichatwindows.notifications.help.line2"),
                20, y, 0xFFFFFFFF);
        y += 14;
        com.client.multichatwindows.util.GuiDrawHelper.text(ctx, font,
                Component.translatable("multichatwindows.notifications.help.line3"),
                20, y, 0xFFFFFFFF);
    }
}