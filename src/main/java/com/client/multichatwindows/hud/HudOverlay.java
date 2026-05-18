package com.client.multichatwindows.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public final class HudOverlay {
    private HudOverlay() {}
    public static void init() { HudRenderCallback.EVENT.register(HudOverlay::render); }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        for (ChatWindow w : WindowService.allWindows()) if (w.enabled) drawWindow(ctx, tr, mc, w);
    }

    private static void drawWindow(DrawContext ctx, TextRenderer tr, MinecraftClient mc, ChatWindow w) {
        float opacity = w.useVanilla ? mc.options.getChatOpacity().getValue().floatValue() : w.opacity;
        int alpha = Math.max(0, Math.min(255, (int) (opacity * 255)));
        int bg = alpha << 24;
        int x1 = w.x, y1 = w.y, x2 = w.x + w.w, y2 = w.y + w.h;
        ctx.fill(x1, y1, x2, y2, bg);

        if (w.outlineEnabled && w.outlineWidth > 0) {
            int color = (0xFF << 24) | (w.outlineColor & 0x00FFFFFF);
            int ow = Math.max(1, Math.min(20, w.outlineWidth));
            for (int i = 0; i < ow; i++) {
                ctx.fill(x1 - i, y1 - i, x2 + i, y1 - i + 1, color);
                ctx.fill(x1 - i, y2 + i - 1, x2 + i, y2 + i, color);
                ctx.fill(x1 - i, y1 - i, x1 - i + 1, y2 + i, color);
                ctx.fill(x2 + i - 1, y1 - i, x2 + i, y2 + i, color);
            }
        }

        int padding = 2;
        float scale = Math.max(0.5f, Math.min(3.0f, w.textScale));
        int scaledMaxTextWidth = Math.max(20, (int) ((w.w - padding * 2) / scale));
        List<OrderedText> wrapped = new ArrayList<>();
        for (Text msg : w.lines) wrapped.addAll(tr.wrapLines(msg, scaledMaxTextWidth));

        int lineHeight = Math.max(1, (int) Math.ceil(tr.fontHeight * scale));
        int availableLines = Math.max(1, (w.h - padding * 2) / lineHeight);
        int maxScroll = Math.max(0, wrapped.size() - availableLines);
        w.scrollOffset = Math.max(0, Math.min(w.scrollOffset, maxScroll));
        int endExclusive = wrapped.size() - w.scrollOffset;
        int start = Math.max(0, endExclusive - availableLines);
        int yy = y2 - padding - lineHeight;

        for (int i = endExclusive - 1; i >= start; i--) {
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale(scale, scale);
            ctx.drawTextWithShadow(tr, wrapped.get(i), (int) ((x1 + padding) / scale), (int) (yy / scale), 0xFFFFFFFF);
            ctx.getMatrices().popMatrix();
            yy -= lineHeight;
        }
    }
}
