package com.client.multichatwindows.config.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class DarkButton extends ClickableWidget {

    @FunctionalInterface
    public interface PressAction {
        void onPress();
    }

    private final PressAction onPress;

    public DarkButton(int x, int y, int w, int h, Text message, PressAction onPress) {
        super(x, y, w, h, message);
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
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

        ctx.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                getMessage(),
                getX() + width / 2,
                getY() + (height - 8) / 2,
                0xFFFFFFFF
        );
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (!active || !visible) return;

        MinecraftClient.getInstance()
                .getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        if (onPress != null) {
            onPress.onPress();
        }
    }

    @Override
    protected void appendClickableNarrations(
            net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder
    ) {
    }
}