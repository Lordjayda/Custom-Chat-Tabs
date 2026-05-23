package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.Language;
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

    // Scroll State
    private int scrollY = 0;

    // Cached server config for render helpers
    private ServerConfig sc;

    // Layout constants (match your existing style)
    private static final int ROW_H = 24;
    private static final int TOP_Y = 54;
    private static final int BOTTOM_PAD = 110; // room for buttons
    private static final int LIST_BTN_W = 160;
    private static final int BTN_W = 200;

    // Scrollbar visuals
    private static final int SCROLLBAR_W = 4;
    private static final int SCROLLBAR_X_PAD = 10;
    private static final int SCROLLBAR_MIN_HANDLE_H = 12;

    // Scrollbar interaction
    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0; // where inside the handle we grabbed
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

        // Screen list
        if (sc.tabs != null) {
            for (int i = 0; i < sc.tabs.size(); i++) {
                TabConfig t = sc.tabs.get(i);
                if (t == null) continue;

                final int idx = i;
                final String tabId = t.id;
                boolean isAll = "all".equalsIgnoreCase(t.id);

                if (y + 20 >= TOP_Y && y <= bottom) {
                    addDrawableChild(new DarkButton(
                            cx - 100,
                            y,
                            LIST_BTN_W,
                            20,
                            Text.translatable("multichatwindows.tab.button", I18nUtil.tKeyOrLiteral(t.name)),
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

                y += ROW_H;
            }
        }

        // Buttons (same style as your other screens)
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

    @Override
    protected boolean onScroll(int delta) {
        scrollY += delta;
        init();
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        lastMouseX = (int) click.x();
        lastMouseY = (int) click.y();

        // If scrollable and clicked on scrollbar area -> intercept
        if (isScrollable() && isPointInScrollbar(lastMouseX, lastMouseY)) {
            ScrollbarMetrics m = computeScrollbarMetrics();
            if (m == null) return true;

            // Clicking in track: jump scroll position to where clicked.
            // If clicked inside handle: start drag with offset.
            if (lastMouseY >= m.handleY && lastMouseY <= m.handleY + m.handleH) {
                draggingScrollbar = true;
                dragGrabOffsetY = lastMouseY - m.handleY;
            } else {
                // jump: center handle around click and start dragging from center
                draggingScrollbar = true;
                dragGrabOffsetY = m.handleH / 2;
                setScrollFromHandleTop(lastMouseY - dragGrabOffsetY, m);
            }

            init();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        lastMouseX = (int) click.x();
        lastMouseY = (int) click.y();

        if (draggingScrollbar && isScrollable()) {
            ScrollbarMetrics m = computeScrollbarMetrics();
            if (m != null) {
                int desiredHandleTop = lastMouseY - dragGrabOffsetY;
                setScrollFromHandleTop(desiredHandleTop, m);
                init();
            }
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingScrollbar = false;
        dragGrabOffsetY = 0;
        return super.mouseReleased(click);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        // keep last mouse position for hover-only show
        lastMouseX = mouseX;
        lastMouseY = mouseY;

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

        // Hover-only scrollbar (or while dragging)
        if (draggingScrollbar || shouldShowScrollbar(mouseX, mouseY)) {
            drawScrollbar(ctx);
        }
    }

    /**
     * Hover-only rule:
     * Show scrollbar when hovering list area OR hovering scrollbar itself.
     */
    private boolean shouldShowScrollbar(int mouseX, int mouseY) {
        if (!isScrollable()) return false;

        int cx = width / 2;
        int bottom = height - BOTTOM_PAD;

        boolean inListArea =
                mouseX >= (cx - 110) && mouseX <= (cx + 110) &&
                mouseY >= TOP_Y && mouseY <= bottom;

        return inListArea || isPointInScrollbar(mouseX, mouseY);
    }

    private boolean isScrollable() {
        if (sc == null) sc = ConfigManager.getOrCreateServer(serverKey);

        int bottom = height - BOTTOM_PAD;
        int viewHeight = bottom - TOP_Y;
        int total = sc.tabs == null ? 0 : sc.tabs.size();
        int contentHeight = total * ROW_H;
        return contentHeight > viewHeight;
    }

    /**
     * Scrollbar area rectangle (track).
     */
    private boolean isPointInScrollbar(int mouseX, int mouseY) {
        int bottom = height - BOTTOM_PAD;

        int barX1 = width - SCROLLBAR_X_PAD;
        int barX2 = barX1 + SCROLLBAR_W;

        return mouseX >= barX1 && mouseX <= barX2 && mouseY >= TOP_Y && mouseY <= bottom;
    }

    /**
     * Visible scrollbar (draw only).
     */
    private void drawScrollbar(DrawContext ctx) {
        ScrollbarMetrics m = computeScrollbarMetrics();
        if (m == null) return;

        // Track background (dark)
        ctx.fill(m.barX1, TOP_Y, m.barX2, m.bottom, 0xFF1A1A1A);

        // Handle (white)
        ctx.fill(m.barX1, m.handleY, m.barX2, m.handleY + m.handleH, 0xFFFFFFFF);
    }

    /**
     * Compute scrollbar geometry based on current scrollY.
     */
    private ScrollbarMetrics computeScrollbarMetrics() {
        if (sc == null) sc = ConfigManager.getOrCreateServer(serverKey);

        int bottom = height - BOTTOM_PAD;
        int viewHeight = bottom - TOP_Y;

        int total = sc.tabs == null ? 0 : sc.tabs.size();
        if (total <= 0) return null;

        int contentHeight = total * ROW_H;
        if (contentHeight <= viewHeight) return null;

        int maxScroll = Math.max(1, contentHeight - viewHeight);
        float scrollProgress = clamp01(scrollY / (float) maxScroll);

        int barX1 = width - SCROLLBAR_X_PAD;
        int barX2 = barX1 + SCROLLBAR_W;

        float ratio = viewHeight / (float) contentHeight;
        int handleH = Math.max(SCROLLBAR_MIN_HANDLE_H, (int) (viewHeight * ratio));

        int handleY = TOP_Y + (int) ((viewHeight - handleH) * scrollProgress);

        ScrollbarMetrics m = new ScrollbarMetrics();
        m.bottom = bottom;
        m.viewHeight = viewHeight;
        m.contentHeight = contentHeight;
        m.maxScroll = maxScroll;
        m.barX1 = barX1;
        m.barX2 = barX2;
        m.handleY = handleY;
        m.handleH = handleH;
        return m;
    }

    /**
     * Convert a desired handle top position into scrollY.
     */
    private void setScrollFromHandleTop(int desiredHandleTop, ScrollbarMetrics m) {
        int trackTop = TOP_Y;
        int trackRange = Math.max(1, m.viewHeight - m.handleH);

        int handleTopClamped = clamp(desiredHandleTop, trackTop, trackTop + trackRange);

        float progress = (handleTopClamped - trackTop) / (float) trackRange;
        progress = clamp01(progress);

        scrollY = (int) Math.round(progress * (m.contentHeight - m.viewHeight));
        scrollY = clamp(scrollY, 0, m.contentHeight - m.viewHeight);
    }

    /**
     * Naming logic:
     * New Screen -> New Screen 1 -> New Screen 2...
     * Neuer Screen -> Neuer Screen 1 -> Neuer Screen 2...
     *
     * Uses visible names (translation key or literal resolved).
     */
    private static String nextNewScreenName(ServerConfig sc) {
        Language lang = ConfigManager.global().language;
        String base = (lang == Language.DE_DE) ? "Neuer Screen" : "New Screen";

        Set<String> used = new HashSet<>();
        if (sc != null && sc.tabs != null) {
            for (TabConfig t : sc.tabs) {
                if (t == null) continue;
                String display = I18nUtil.tKeyOrLiteral(t.name).getString();
                used.add(display);
            }
        }

        if (!used.contains(base)) return base;

        int n = 1;
        while (used.contains(base + " " + n)) n++;
        return base + " " + n;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static final class ScrollbarMetrics {
        int bottom;
        int viewHeight;
        int contentHeight;
        int maxScroll;

        int barX1;
        int barX2;

        int handleY;
        int handleH;
    }
}
