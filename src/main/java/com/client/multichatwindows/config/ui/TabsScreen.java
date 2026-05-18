package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import com.client.multichatwindows.hud.WindowService;
import com.client.multichatwindows.util.I18nUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class TabsScreen extends ScrollableDarkScreen {

    private final Screen parent;
    private final String serverKey;

    private int scrollY = 0;

    public TabsScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.tabs.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();

        ServerConfig sc = ConfigManager.getOrCreateServer(serverKey);

        int cx = width / 2;
        int top = 54;
        int bottom = height - 110;
        int rowH = 24;

        int max = Math.max(0, sc.tabs.size() * rowH - (bottom - top));
        scrollY = Math.max(0, Math.min(scrollY, max));

        int y = top - scrollY;

        for (int i = 0; i < sc.tabs.size(); i++) {
            TabConfig t = sc.tabs.get(i);

            final int idx = i;
            final String tabId = t.id;

            boolean isAll = "all".equalsIgnoreCase(t.id);

            if (y + 20 >= top && y <= bottom) {
                addDrawableChild(new DarkButton(
                        cx - 100,
                        y,
                        160,
                        20,
                        Text.translatable(
                                "multichatwindows.tab.button",
                                I18nUtil.tKeyOrLiteral(t.name)
                        ),
                        () -> MinecraftClient.getInstance().setScreen(new TabEditScreen(this, serverKey, idx))
                ));

                addDrawableChild(new DarkButton(
                        cx + 64,
                        y,
                        36,
                        20,
                        Text.translatable(isAll ? "multichatwindows.locked" : "multichatwindows.delete"),
                        () -> {
                            if (isAll) return;

                            sc.tabs.removeIf(tab -> tab != null && tabId.equals(tab.id));
                            ConfigManager.deleteTab(serverKey, tabId);
                            ConfigManager.saveServer(serverKey, sc);

                            WindowService.rebuildForCurrentServer();

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
                Text.translatable("multichatwindows.tabs.add"),
                () -> {
                    TabConfig t = new TabConfig();
                    t.id = "tab" + System.currentTimeMillis();
                    t.name = "multichatwindows.tab.default_name";
                    t.enabled = true;
                    t.useVanilla = false;

                    sc.tabs.add(t);

                    ConfigManager.saveServer(serverKey, sc);
                    WindowService.rebuildForCurrentServer();

                    init();
                }
        ));

        addDrawableChild(new DarkButton(
                cx - 100,
                height - 54,
                200,
                20,
                Text.translatable("multichatwindows.save"),
                () -> {
                    ConfigManager.saveServer(serverKey, sc);
                    WindowService.rebuildForCurrentServer();

                    MinecraftClient.getInstance().setScreen(parent);
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

        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.tabs.title"),
                width / 2,
                14,
                0xFFFFFFFF
        );

        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.server.current", Text.literal(serverKey)),
                width / 2,
                28,
                0xFFB0B0B0
        );
    }
}