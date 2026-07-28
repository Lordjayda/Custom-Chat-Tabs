package com.client.multichatwindows.config.ui;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class DarkScreen extends Screen {
    protected DarkScreen(Component title) {
        super(title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        renderDarkBackground(ctx);
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ServerHistoryTrimButton.render(this, ctx, width, height, mouseX, mouseY);
        ConfigUiAutoSave.renderInputTooltip(this, ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (ServerHistoryTrimButton.mouseClicked(this, width, height, click.x(), click.y(), click.button())) {
            return true;
        }
        boolean result = super.mouseClicked(click, doubled);
        ConfigUiAutoSave.commit(this);
        return result;
    }

    @Override
    public void removed() {
        ConfigUiAutoSave.commit(this);
        super.removed();
    }

    protected void renderDarkBackground(GuiGraphicsExtractor ctx) {
        if (ConfigBackgroundRenderer.render(ctx, width, height)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null && tryRenderInGameBlur(ctx)) {
            ctx.fill(0, 0, width, height, 0x99000000);
            return;
        }
        ctx.fill(0, 0, width, height, 0xFF0F0F10);
        ctx.fill(0, 0, width, height, 0x55000000);
    }

    private boolean tryRenderInGameBlur(GuiGraphicsExtractor ctx) {
        try {
            Method method = Screen.class.getDeclaredMethod("renderInGameBackground", GuiGraphicsExtractor.class);
            method.setAccessible(true);
            method.invoke(this, ctx);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
