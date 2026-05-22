package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ChatFilterRule;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ChatFiltersScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    private int scrollY = 0;
    private ServerConfig sc;

    public ChatFiltersScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.chatfilters.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();
        sc = ConfigManager.getOrCreateServer(serverKey);
        if (sc.chatFilters == null) sc.chatFilters = new java.util.ArrayList<>();

        int cx = width / 2;
        int top = 54;
        int bottom = height - 110;
        int rowH = 24;

        int max = Math.max(0, sc.chatFilters.size() * rowH - (bottom - top));
        scrollY = Math.max(0, Math.min(scrollY, max));

        int y = top - scrollY;

        for (int i = 0; i < sc.chatFilters.size(); i++) {
            ChatFilterRule r = sc.chatFilters.get(i);
            final int idx = i;

            if (y + 20 >= top && y <= bottom) {
                addDrawableChild(new DarkButton(
                        cx - 100, y, 70, 20,
                        Text.translatable(r.player ? "multichatwindows.dep.player" : "multichatwindows.dep.msg"),
                        () -> {
                            r.player = !r.player;
                            ConfigManager.saveServer(serverKey, sc);
                            init();
                        }
                ));

                TextFieldWidget tf = new TextFieldWidget(
                        textRenderer, cx - 25, y, 125, 20,
                        Text.translatable("multichatwindows.dep.value")
                );
                tf.setText(r.value == null ? "" : r.value);
                tf.setChangedListener(s -> r.value = s);
                addDrawableChild(tf);

                addDrawableChild(new DarkButton(
                        cx + 104, y, 36, 20,
                        Text.translatable("multichatwindows.delete"),
                        () -> {
                            sc.chatFilters.remove(idx);
                            ConfigManager.saveServer(serverKey, sc);
                            init();
                        }
                ));
            }

            y += rowH;
        }

        addDrawableChild(new DarkButton(
                cx - 100, height - 80, 200, 20,
                Text.translatable("multichatwindows.chatfilters.add"),
                () -> {
                    sc.chatFilters.add(new ChatFilterRule());
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));

        addDrawableChild(new DarkButton(
                cx - 100, height - 54, 200, 20,
                Text.translatable("multichatwindows.save"),
                () -> {
                    ConfigManager.saveServer(serverKey, sc);
                    MinecraftClient.getInstance().setScreen(parent);
                }
        ));

        addDrawableChild(new DarkButton(
                cx - 100, height - 28, 200, 20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    protected boolean onScroll(int delta) {
        scrollY += delta;
        init();
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("multichatwindows.chatfilters.title"),
                width / 2, 14, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("multichatwindows.chatfilters.hint"),
                width / 2, 28, 0xFFB0B0B0);
    }
}