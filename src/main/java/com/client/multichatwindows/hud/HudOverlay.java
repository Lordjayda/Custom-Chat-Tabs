package com.client.multichatwindows.hud;

import com.client.multichatwindows.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public final class HudOverlay {
    private static final long FADE_START_MS = 7000L;
    private static final long FADE_END_MS = 8500L;

    private HudOverlay() {
    }

    public static void init() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("multichatwindows", "hud_overlay"), HudOverlay::extractRenderState);
    }

    private static void extractRenderState(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.options.hideGui) return;
        if (!ConfigManager.global().enabled) return;

        Font tr = mc.font;

        for (ChatWindow w : WindowService.allWindows()) {
            if (w.enabled) {
                drawWindow(ctx, tr, mc, w);
            }
        }
    }

    private static void drawWindow(GuiGraphicsExtractor ctx, Font tr, Minecraft mc, ChatWindow w) {
        List<ChatWindow.RowRef> rows = w.visibleRows();
        if (rows.isEmpty() && !isExpanded(mc)) {
            return;
        }

        boolean expanded = isExpanded(mc);
        boolean scrolled = w.scrollOffset > 0;
        long now = System.currentTimeMillis();
        int strongestLineAlpha = strongestLineAlpha(rows, now, expanded, scrolled);

        if (!expanded && !scrolled && strongestLineAlpha <= 0) {
            return;
        }

        float opacity = w.useVanilla
                ? mc.options.chatOpacity().get().floatValue()
                : w.opacity;

        int alpha = Math.max(0, Math.min(255, (int) (opacity * 255)));
        if (!expanded && !scrolled) {
            alpha = alpha * strongestLineAlpha / 255;
        }

        int bg = (alpha << 24);

        int x1 = w.x;
        int y1 = w.y;
        int x2 = w.x + w.w;
        int y2 = w.y + w.h;

        if (alpha > 0) {
            ctx.fill(x1, y1, x2, y2, bg);
        }

        if ((expanded || scrolled || strongestLineAlpha > 0) && w.outlineEnabled && w.outlineWidth > 0) {
            int outlineAlpha = expanded || scrolled ? 255 : strongestLineAlpha;
            int color = (outlineAlpha << 24) | (w.outlineColor & 0x00FFFFFF);
            int ow = Math.max(1, Math.min(20, w.outlineWidth));

            for (int i = 0; i < ow; i++) {
                ctx.fill(x1 - i, y1 - i, x2 + i, y1 - i + 1, color);
                ctx.fill(x1 - i, y2 + i - 1, x2 + i, y2 + i, color);
                ctx.fill(x1 - i, y1 - i, x1 - i + 1, y2 + i, color);
                ctx.fill(x2 + i - 1, y1 - i, x2 + i, y2 + i, color);
            }
        }

        float scale = Math.max(0.5f, Math.min(3.0f, w.textScale));
        int lineHeight = ChatWindow.scaledLineHeight(tr, scale);
        int yy = y2 - ChatWindow.PADDING_Y - lineHeight;

        for (int i = rows.size() - 1; i >= 0; i--) {
            ChatWindow.RowRef row = rows.get(i);
            int lineAlpha = lineAlpha(row.createdAtMs, now, expanded, scrolled);
            if (lineAlpha <= 0) {
                yy -= lineHeight;
                continue;
            }

            ctx.pose().pushMatrix();
            ctx.pose().scale(scale, scale);

            drawOrderedTextWithStyleColors(
                    ctx,
                    tr,
                    row.ordered,
                    (int) ((x1 + ChatWindow.PADDING_X) / scale),
                    (int) (yy / scale),
                    lineAlpha
            );

            ctx.pose().popMatrix();

            yy -= lineHeight;
        }
    }

    private static void drawOrderedTextWithStyleColors(GuiGraphicsExtractor ctx, Font tr, FormattedCharSequence ordered, int x, int y, int alpha) {
        if (ordered == null || alpha <= 0) {
            return;
        }

        int safeAlpha = Math.max(0, Math.min(255, alpha));
        // Fast path: render the already wrapped/ordered row directly.
        // Avoid splitting each visible row into new String/List runs every frame.
        com.client.multichatwindows.util.GuiDrawHelper.text(ctx, tr, ordered, x, y, (safeAlpha << 24) | 0x00FFFFFF);
    }

    private static List<StyledTextRun> splitStyledRuns(FormattedCharSequence ordered) {
        List<StyledTextRun> runs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Style[] currentStyle = new Style[]{null};

        ordered.accept((index, style, codePoint) -> {
            Style safeStyle = style == null ? Style.EMPTY : style;
            if (currentStyle[0] != null && !Objects.equals(currentStyle[0], safeStyle)) {
                runs.add(new StyledTextRun(current.toString(), currentStyle[0]));
                current.setLength(0);
            }

            currentStyle[0] = safeStyle;
            current.appendCodePoint(codePoint);
            return true;
        });

        if (currentStyle[0] != null && current.length() > 0) {
            runs.add(new StyledTextRun(current.toString(), currentStyle[0]));
        }

        return runs;
    }

    private static int styleRgb(Style style) {
        if (style == null) {
            return 0x00FFFFFF;
        }

        TextColor color = style.getColor();
        if (color == null) {
            return 0x00FFFFFF;
        }

        return color.getValue() & 0x00FFFFFF;
    }

    private static boolean isExpanded(Minecraft mc) {
        return mc.screen instanceof ChatScreen;
    }

    private static int strongestLineAlpha(List<ChatWindow.RowRef> rows, long now, boolean expanded, boolean scrolled) {
        int strongest = 0;
        for (ChatWindow.RowRef row : rows) {
            strongest = Math.max(strongest, lineAlpha(row.createdAtMs, now, expanded, scrolled));
        }
        return strongest;
    }

    private static int lineAlpha(long createdAtMs, long now, boolean expanded, boolean scrolled) {
        if (expanded || scrolled || createdAtMs <= 0L) {
            return 255;
        }

        long age = Math.max(0L, now - createdAtMs);
        if (age >= FADE_END_MS) {
            return 0;
        }
        if (age <= FADE_START_MS) {
            return 255;
        }

        float t = (age - FADE_START_MS) / (float) (FADE_END_MS - FADE_START_MS);
        float inv = 1.0f - Math.max(0.0f, Math.min(1.0f, t));
        return Math.max(0, Math.min(255, (int) (255.0f * inv * inv)));
    }

    private record StyledTextRun(String text, Style style) {
    }

}
