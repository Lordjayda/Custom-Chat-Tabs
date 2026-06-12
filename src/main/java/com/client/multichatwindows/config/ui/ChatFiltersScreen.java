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

        addDrawableChild(new DarkButton(
                width - 82,
                8,
                34,
                20,
                Text.literal("✔"),
                () -> {
                    for (ChatFilterRule rule : sc.chatFilters) {
                        if (rule != null) rule.enabled = true;
                    }
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));

        addDrawableChild(new DarkButton(
                width - 44,
                8,
                34,
                20,
                Text.literal("❌"),
                () -> {
                    for (ChatFilterRule rule : sc.chatFilters) {
                        if (rule != null) rule.enabled = false;
                    }
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));

        int max = Math.max(0, sc.chatFilters.size() * rowH - (bottom - top));
        scrollY = Math.max(0, Math.min(scrollY, max));

        int y = top - scrollY;

        for (int i = 0; i < sc.chatFilters.size(); i++) {
            ChatFilterRule r = sc.chatFilters.get(i);
            if (r == null) continue;
            final int idx = i;

            if (y + 20 >= top && y <= bottom) {
                addDrawableChild(new DarkButton(
                        cx - 148,
                        y,
                        34,
                        20,
                        Text.literal(r.enabled ? "✔" : "❌"),
                        () -> {
                            r.enabled = !r.enabled;
                            ConfigManager.saveServer(serverKey, sc);
                            init();
                        }
                ));

                addDrawableChild(new DarkButton(
                        cx - 110,
                        y,
                        70,
                        20,
                        Text.translatable(r.player ? "multichatwindows.dep.player" : "multichatwindows.dep.msg"),
                        () -> {
                            r.player = !r.player;
                            ConfigManager.saveServer(serverKey, sc);
                            init();
                        }
                ));

                TextFieldWidget tf = new TextFieldWidget(
                        textRenderer,
                        cx - 35,
                        y,
                        125,
                        20,
                        Text.translatable("multichatwindows.dep.value")
                );
                tf.setText(r.value == null ? "" : r.value);
                tf.setChangedListener(s -> r.value = s);
                addDrawableChild(tf);

                addDrawableChild(new DarkButton(
                        cx + 94,
                        y,
                        36,
                        20,
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
                cx - 100,
                height - 80,
                200,
                20,
                Text.translatable("multichatwindows.chatfilters.add"),
                () -> {
                    ChatFilterRule rule = new ChatFilterRule();
                    rule.enabled = true;
                    sc.chatFilters.add(rule);
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));

        

        addDrawableChild(new DarkButton(
                cx - 100,
                height - 28,
                200,
                20,
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
                Text.literal("✔/❌ top-right = enable/disable all | per row = toggle one filter"),
                width / 2, 28, 0xFFB0B0B0);
    }
}
