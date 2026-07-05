package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ChatFilterRule;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ChatFiltersScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    private int scrollY = 0;
    private ServerConfig sc;

    public ChatFiltersScreen(Screen parent, String serverKey) {
        super(Component.translatable("multichatwindows.chatfilters.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearWidgets();
        sc = ConfigManager.getOrCreateServer(serverKey);
        if (sc.chatFilters == null) sc.chatFilters = new java.util.ArrayList<>();

        int cx = width / 2;
        int top = 54;
        int bottom = height - 110;
        int rowH = 24;

        addRenderableWidget(new DarkButton(
                width - 82,
                8,
                34,
                20,
                Component.literal("✔"),
                () -> {
                    for (ChatFilterRule rule : sc.chatFilters) {
                        if (rule != null) rule.enabled = true;
                    }
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));

        addRenderableWidget(new DarkButton(
                width - 44,
                8,
                34,
                20,
                Component.literal("❌"),
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
                addRenderableWidget(new DarkButton(
                        cx - 148,
                        y,
                        34,
                        20,
                        Component.literal(r.enabled ? "✔" : "❌"),
                        () -> {
                            r.enabled = !r.enabled;
                            ConfigManager.saveServer(serverKey, sc);
                            init();
                        }
                ));

                addRenderableWidget(new DarkButton(
                        cx - 110,
                        y,
                        70,
                        20,
                        Component.translatable(r.player ? "multichatwindows.dep.player" : "multichatwindows.dep.msg"),
                        () -> {
                            r.player = !r.player;
                            ConfigManager.saveServer(serverKey, sc);
                            init();
                        }
                ));

                EditBox tf = new EditBox(
                        font,
                        cx - 35,
                        y,
                        125,
                        20,
                        Component.translatable("multichatwindows.dep.value")
                );
                tf.setValue(r.value == null ? "" : r.value);
                tf.setResponder(s -> r.value = s);
                addRenderableWidget(tf);

                addRenderableWidget(new DarkButton(
                        cx + 94,
                        y,
                        36,
                        20,
                        Component.translatable("multichatwindows.delete"),
                        () -> {
                            sc.chatFilters.remove(idx);
                            ConfigManager.saveServer(serverKey, sc);
                            init();
                        }
                ));
            }

            y += rowH;
        }

        addRenderableWidget(new DarkButton(
                cx - 100,
                height - 80,
                200,
                20,
                Component.translatable("multichatwindows.chatfilters.add"),
                () -> {
                    ChatFilterRule rule = new ChatFilterRule();
                    rule.enabled = true;
                    sc.chatFilters.add(rule);
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));

        

        addRenderableWidget(new DarkButton(
                cx - 100,
                height - 28,
                200,
                20,
                Component.translatable("multichatwindows.back"),
                () -> Minecraft.getInstance().setScreen(parent)
        ));
    }

    @Override
    protected boolean onScroll(int delta) {
        scrollY += delta;
        init();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font,
                Component.translatable("multichatwindows.chatfilters.title"),
                width / 2, 14, 0xFFFFFFFF);
        com.client.multichatwindows.util.GuiDrawHelper.centered(ctx, font,
                Component.literal("✔/❌ top-right = enable/disable all | per row = toggle one filter"),
                width / 2, 28, 0xFFB0B0B0);
    }
}
