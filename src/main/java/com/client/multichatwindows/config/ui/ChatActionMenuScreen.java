package com.client.multichatwindows.config.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ChatActionMenuScreen extends DarkScreen {
    private final Screen parent;
    private final Text message;

    public ChatActionMenuScreen(Screen parent, Text message) {
        super(Text.translatable("multichatwindows.chat_actions.title"));
        this.parent = parent;
        this.message = message;
    }

    @Override
    protected void init() {
        clearChildren();
        int centerX = width / 2;

        addDrawableChild(new DarkButton(
                centerX - 100,
                70,
                200,
                20,
                Text.translatable("multichatwindows.chat_actions.copy"),
                () -> {
                    MinecraftClient.getInstance().keyboard.setClipboard(message == null ? "" : message.getString());
                    MinecraftClient.getInstance().setScreen(parent);
                }
        ));

        addDrawableChild(new DarkButton(
                centerX - 100,
                96,
                200,
                20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.chat_actions.title"), width / 2, 14, 0xFFFFFFFF);

        String preview = message == null ? "" : message.getString();
        if (preview.length() > 70) {
            preview = preview.substring(0, 67) + "...";
        }
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(preview), width / 2, 42, 0xFFB0B0B0);
    }
}
