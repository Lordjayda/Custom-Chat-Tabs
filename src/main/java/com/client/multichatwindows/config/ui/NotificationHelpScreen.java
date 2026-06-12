package com.client.multichatwindows.config.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class NotificationHelpScreen extends ScrollableDarkScreen {
    private final Screen parent;

    public NotificationHelpScreen(Screen parent) {
        super(Text.translatable("multichatwindows.notifications.help"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        addDrawableChild(new DarkButton(
                width / 2 - 100, height - 28, 200, 20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("multichatwindows.notifications.help"),
                width / 2, 14, 0xFFFFFFFF);

        int y = 44;
        ctx.drawTextWithShadow(textRenderer,
                Text.translatable("multichatwindows.notifications.help.line1"),
                20, y, 0xFFFFFFFF);
        y += 14;
        ctx.drawTextWithShadow(textRenderer,
                Text.translatable("multichatwindows.notifications.help.line2"),
                20, y, 0xFFFFFFFF);
        y += 14;
        ctx.drawTextWithShadow(textRenderer,
                Text.translatable("multichatwindows.notifications.help.line3"),
                20, y, 0xFFFFFFFF);
    }
}