package com.client.multichatwindows.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;

public final class HudOverlay {
    private static final long FADE_START_MS = 7000L;
    private static final long FADE_END_MS = 8500L;

    private HudOverlay() {
    }

    public static void init() {
        HudRenderCallback.EVENT.register(HudOverlay::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.options.hudHidden) return;

        TextRenderer tr = mc.textRenderer;

        for (ChatWindow w : WindowService.allWindows()) {
            if (w.enabled) {
                drawWindow(ctx, tr, mc, w);
            }
        }
    }

    private static void drawWindow(DrawContext ctx, TextRenderer tr, MinecraftClient mc, ChatWindow w) {
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
                ? mc.options.getChatOpacity().getValue().floatValue()
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

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale(scale, scale);

            ctx.drawTextWithShadow(
                    tr,
                    row.ordered,
                    (int) ((x1 + ChatWindow.PADDING_X) / scale),
                    (int) (yy / scale),
                    (lineAlpha << 24) | 0x00FFFFFF
            );

            ctx.getMatrices().popMatrix();

            yy -= lineHeight;
        }
    }

    private static boolean isExpanded(MinecraftClient mc) {
        return mc.currentScreen instanceof ChatScreen;
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
}
