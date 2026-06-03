package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.DependencyRule;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import com.client.multichatwindows.hud.WindowService;
import com.client.multichatwindows.util.I18nUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

public class ScreensMenuScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;

    private int scrollY = 0;
    private ServerConfig sc;

    private static final int ROW_H = 24;
    private static final int TOP_Y = 54;
    private static final int BOTTOM_PAD = 110;
    private static final int EDIT_BTN_W = 140;
    private static final int SMALL_BTN_W = 36;
    private static final int BTN_W = 200;

    private static final int SCROLLBAR_W = 4;
    private static final int SCROLLBAR_X_PAD = 10;
    private static final int SCROLLBAR_MIN_HANDLE_H = 12;

    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public ScreensMenuScreen(Screen parent, String serverKey) {
        super(Text.translatable("multichatwindows.screens.title"));
        this.parent = parent;
        this.serverKey = serverKey;
    }

    @Override
    protected void init() {
        clearChildren();
        sc = ConfigManager.getOrCreateServer(serverKey);

        int cx = width / 2;

        int bottom = height - BOTTOM_PAD;
        int viewHeight = bottom - TOP_Y;

        int contentHeight = Math.max(0, (sc.tabs == null ? 0 : sc.tabs.size()) * ROW_H);
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollY = clamp(scrollY, 0, maxScroll);

        int y = TOP_Y - scrollY;

        if (sc.tabs != null) {
            for (int i = 0; i < sc.tabs.size(); i++) {
                TabConfig t = sc.tabs.get(i);
                if (t == null) continue;

                final int idx = i;
                final String tabId = t.id;
                boolean isAll = "all".equalsIgnoreCase(t.id);

                if (y + 20 >= TOP_Y && y <= bottom) {
                    addDrawableChild(new DarkButton(
                            cx - 120,
                            y,
                            EDIT_BTN_W,
                            20,
                            Text.translatable("multichatwindows.tab.button", I18nUtil.tKeyOrLiteral(t.name)),
                            () -> MinecraftClient.getInstance().setScreen(new TabEditScreen(this, serverKey, idx))
                    ));

                    addDrawableChild(new DarkButton(
                            cx + 24,
                            y,
                            SMALL_BTN_W,
                            20,
                            Text.literal("⧉"),
                            () -> {
                                if (isAll) return;
                                duplicateTab(idx);
                            }
                    ));

                    addDrawableChild(new DarkButton(
                            cx + 64,
                            y,
                            SMALL_BTN_W,
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

                y += ROW_H;
            }
        }

        addDrawableChild(new DarkButton(
                cx - 100,
                height - 80,
                BTN_W,
                20,
                Text.translatable("multichatwindows.screens.add"),
                () -> {
                    TabConfig t = new TabConfig();
                    t.id = "tab" + System.currentTimeMillis();
                    t.name = nextNewScreenName(sc);
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
                BTN_W,
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
                BTN_W,
                20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    private void duplicateTab(int index) {
        if (sc == null || sc.tabs == null || index < 0 || index >= sc.tabs.size()) {
            return;
        }

        TabConfig source = sc.tabs.get(index);
        if (source == null || "all".equalsIgnoreCase(source.id)) {
            return;
        }

        TabConfig copy = copyTab(source);
        copy.id = uniqueTabId(sc, source.id);
        copy.name = uniqueTabName(sc, source.name);
        copy.x = source.x + 12;
        copy.y = source.y + 12;

        sc.tabs.add(index + 1, copy);
        ConfigManager.saveServer(serverKey, sc);
        WindowService.rebuildForCurrentServer();
        init();
    }

    private static TabConfig copyTab(TabConfig source) {
        TabConfig copy = new TabConfig();
        copy.id = source.id;
        copy.name = source.name;
        copy.enabled = source.enabled;
        copy.x = source.x;
        copy.y = source.y;
        copy.width = source.width;
        copy.height = source.height;
        copy.opacity = source.opacity;
        copy.textScale = source.textScale;
        copy.useVanilla = source.useVanilla;
        copy.filterAllChat = source.filterAllChat;
        copy.outlineEnabled = source.outlineEnabled;
        copy.outlineWidth = source.outlineWidth;
        copy.outlineColor = source.outlineColor;

        copy.dependencies.clear();
        if (source.dependencies != null) {
            for (DependencyRule rule : source.dependencies) {
                if (rule == null) continue;

                DependencyRule cloned = new DependencyRule();
                cloned.player = rule.player;
                cloned.value = rule.value;
                copy.dependencies.add(cloned);
            }
        }

        return copy;
    }

    private static String uniqueTabId(ServerConfig sc, String baseId) {
        String base = baseId == null || baseId.isBlank() ? "tab" : baseId.trim();
        String candidate = base + "_copy";
        int suffix = 2;

        while (tabIdExists(sc, candidate)) {
            candidate = base + "_copy" + suffix;
            suffix++;
        }

        return candidate;
    }

    private static boolean tabIdExists(ServerConfig sc, String id) {
        if (sc == null || sc.tabs == null || id == null) {
            return false;
        }

        for (TabConfig tab : sc.tabs) {
            if (tab != null && id.equalsIgnoreCase(tab.id)) {
                return true;
            }
        }

        return false;
    }

    private static String uniqueTabName(ServerConfig sc, String sourceName) {
        String base = sourceName == null || sourceName.isBlank() ? "Screen" : sourceName.trim();
        String candidate = base + " Copy";
        int suffix = 2;

        while (tabNameExists(sc, candidate)) {
            candidate = base + " Copy " + suffix;
            suffix++;
        }

        return candidate;
    }

    private static boolean tabNameExists(ServerConfig sc, String name) {
        if (sc == null || sc.tabs == null || name == null) {
            return false;
        }

        for (TabConfig tab : sc.tabs) {
            if (tab != null && name.equalsIgnoreCase(tab.name)) {
                return true;
            }
        }

        return false;
    }

    private String nextNewScreenName(ServerConfig sc) {
        String prefix = I18nUtil.tKeyOrLiteral("multichatwindows.default_new_screen").getString();

        if (prefix == null || prefix.isBlank() || prefix.equals("multichatwindows.default_new_screen")) {
            prefix = "New Screen";
        }

        Set<String> used = new HashSet<>();
        if (sc.tabs != null) {
            for (TabConfig tab : sc.tabs) {
                if (tab != null && tab.name != null) {
                    used.add(tab.name.trim().toLowerCase());
                }
            }
        }

        for (int i = 1; i < 10000; i++) {
            String candidate = prefix + " " + i;
            if (!used.contains(candidate.toLowerCase())) {
                return candidate;
            }
        }

        return prefix + " " + System.currentTimeMillis();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleScroll(mouseX, mouseY, verticalAmount)
                || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return handleScroll(mouseX, mouseY, amount);
    }

    private boolean handleScroll(double mouseX, double mouseY, double amount) {
        int bottom = height - BOTTOM_PAD;
        if (mouseY < TOP_Y || mouseY > bottom) {
            return false;
        }

        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return false;
        }

        int before = scrollY;
        scrollY = clamp(scrollY + (amount < 0 ? 24 : -24), 0, maxScroll);

        if (before != scrollY) {
            init();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0 && isMouseOverScrollbar(mouseX, mouseY)) {
            int[] handle = scrollbarHandle();
            if (handle != null) {
                draggingScrollbar = true;
                dragGrabOffsetY = (int) mouseY - handle[0];
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        this.lastMouseX = (int) mouseX;
        this.lastMouseY = (int) mouseY;

        if (draggingScrollbar && button == 0) {
            dragScrollbarTo((int) mouseY - dragGrabOffsetY);
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingScrollbar = false;
        return super.mouseReleased(click);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        lastMouseX = (int) mouseX;
        lastMouseY = (int) mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    private void dragScrollbarTo(int handleTop) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            scrollY = 0;
            return;
        }

        int bottom = height - BOTTOM_PAD;
        int trackTop = TOP_Y;
        int trackBottom = bottom;
        int trackH = Math.max(1, trackBottom - trackTop);

        int[] handle = scrollbarHandle();
        int handleH = handle == null ? SCROLLBAR_MIN_HANDLE_H : Math.max(1, handle[1] - handle[0]);
        int movable = Math.max(1, trackH - handleH);

        int clampedTop = clamp(handleTop, trackTop, trackBottom - handleH);
        float t = (clampedTop - trackTop) / (float) movable;

        scrollY = clamp(Math.round(t * maxScroll), 0, maxScroll);
        init();
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int[] handle = scrollbarHandle();
        if (handle == null) return false;

        int barX = scrollbarX();
        return mouseX >= barX
                && mouseX <= barX + SCROLLBAR_W + 2
                && mouseY >= handle[0]
                && mouseY <= handle[1];
    }

    private int scrollbarX() {
        return width - SCROLLBAR_X_PAD;
    }

    private int maxScroll() {
        if (sc == null) {
            sc = ConfigManager.getOrCreateServer(serverKey);
        }

        int bottom = height - BOTTOM_PAD;
        int viewHeight = bottom - TOP_Y;
        int contentHeight = Math.max(0, (sc.tabs == null ? 0 : sc.tabs.size()) * ROW_H);

        return Math.max(0, contentHeight - viewHeight);
    }

    private int[] scrollbarHandle() {
        if (sc == null) {
            sc = ConfigManager.getOrCreateServer(serverKey);
        }

        int maxScroll = maxScroll();
        if (maxScroll <= 0) return null;

        int bottom = height - BOTTOM_PAD;
        int trackTop = TOP_Y;
        int trackBottom = bottom;
        int trackH = Math.max(1, trackBottom - trackTop);

        int contentHeight = Math.max(1, (sc.tabs == null ? 0 : sc.tabs.size()) * ROW_H);
        int viewHeight = Math.max(1, trackH);

        int handleH = Math.max(
                SCROLLBAR_MIN_HANDLE_H,
                (int) (trackH * (viewHeight / (float) contentHeight))
        );

        handleH = Math.min(handleH, trackH);

        int movable = Math.max(1, trackH - handleH);
        int handleTop = trackTop + Math.round((scrollY / (float) maxScroll) * movable);

        return new int[]{handleTop, handleTop + handleH};
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.screens.title"),
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

        renderScrollbar(ctx);
    }

    private void renderScrollbar(DrawContext ctx) {
        int[] handle = scrollbarHandle();
        if (handle == null) return;

        int bottom = height - BOTTOM_PAD;
        int barX = scrollbarX();
        int trackColor = 0x55303030;
        int handleColor = (isMouseOverScrollbar(lastMouseX, lastMouseY) || draggingScrollbar)
                ? 0xFFE0E0E0
                : 0xFFB0B0B0;

        ctx.fill(barX, TOP_Y, barX + SCROLLBAR_W, bottom, trackColor);
        ctx.fill(barX, handle[0], barX + SCROLLBAR_W, handle[1], handleColor);
    }
}
