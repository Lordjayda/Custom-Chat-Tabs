package com.client.multichatwindows.config.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class DarkButton extends AbstractWidget {

    @FunctionalInterface
    public interface PressAction {
        void onPress();
    }

    private final PressAction onPress;

    public DarkButton(int x, int y, int w, int h, Component message, PressAction onPress) {
        super(x, y, w, h, message);
        this.onPress = onPress;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        boolean hovered =
                mouseX >= getX()
                        && mouseX <= getX() + width
                        && mouseY >= getY()
                        && mouseY <= getY() + height;

        int bg = hovered ? 0xFF222222 : 0xFF121212;
        int border = 0xFFFFFFFF;

        ctx.fill(getX(), getY(), getX() + width, getY() + height, bg);

        ctx.fill(getX(), getY(), getX() + width, getY() + 1, border);
        ctx.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        ctx.fill(getX(), getY(), getX() + 1, getY() + height, border);
        ctx.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);

        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, 
                Minecraft.getInstance().font,
                getMessage(),
                getX() + width / 2,
                getY() + (height - 8) / 2,
                0xFFFFFFFF
        );
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        if (!active || !visible) return;

        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        if (onPress != null) {
            onPress.onPress();
        }
    }

    @Override
    protected void updateWidgetNarration(
            net.minecraft.client.gui.narration.NarrationElementOutput builder
    ) {
    }
}