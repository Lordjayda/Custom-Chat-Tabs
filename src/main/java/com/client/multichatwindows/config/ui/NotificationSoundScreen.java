package com.client.multichatwindows.config.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class NotificationSoundScreen extends ScrollableDarkScreen {
    private final Screen parent;

    public NotificationSoundScreen(Screen parent, String serverKey, int notificationIndex) {
        super(Text.translatable("multichatwindows.notifications.sound.removed"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();

        int centerX = width / 2;

        addDrawableChild(new DarkButton(
                centerX - 130,
                height - 28,
                260,
                20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.notifications.sound.removed"),
                width / 2,
                14,
                0xFFFFFFFF
        );
    }
}
