package com.client.multichatwindows.config.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.lang.reflect.Method;

public class DarkScreen extends Screen {
    protected DarkScreen(Text title) {
        super(title);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderDarkBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        ServerHistoryTrimButton.render(this, ctx, width, height, mouseX, mouseY);
        ConfigUiAutoSave.renderInputTooltip(this, ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
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

    protected void renderDarkBackground(DrawContext ctx) {
        if (ConfigBackgroundRenderer.render(ctx, width, height)) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getCurrentServerEntry() != null && tryRenderInGameBlur(ctx)) {
            ctx.fill(0, 0, width, height, 0x99000000);
            return;
        }
        ctx.fill(0, 0, width, height, 0xFF0F0F10);
        ctx.fill(0, 0, width, height, 0x55000000);
    }

    private boolean tryRenderInGameBlur(DrawContext ctx) {
        try {
            Method method = Screen.class.getDeclaredMethod("renderInGameBackground", DrawContext.class);
            method.setAccessible(true);
            method.invoke(this, ctx);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
