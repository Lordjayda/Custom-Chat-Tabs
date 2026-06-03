package com.client.multichatwindows.config.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class TooltipHelper {
    private TooltipHelper() {
    }

    public static void renderIfHovered(
            DrawContext ctx,
            TextRenderer textRenderer,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            Text text
    ) {
        if (text == null || text.getString().isBlank()) {
            return;
        }

        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return;
        }

        String tooltip = text.getString();
        int tooltipWidth = Math.min(textRenderer.getWidth(tooltip) + 10, 360);
        int tooltipHeight = 18;
        int tx = Math.min(mouseX + 10, ctx.getScaledWindowWidth() - tooltipWidth - 4);
        int ty = Math.min(mouseY + 10, ctx.getScaledWindowHeight() - tooltipHeight - 4);

        ctx.fill(tx, ty, tx + tooltipWidth, ty + tooltipHeight, 0xEE101010);
        ctx.fill(tx, ty, tx + tooltipWidth, ty + 1, 0xFFFFFFFF);
        ctx.fill(tx, ty + tooltipHeight - 1, tx + tooltipWidth, ty + tooltipHeight, 0xFFFFFFFF);
        ctx.fill(tx, ty, tx + 1, ty + tooltipHeight, 0xFFFFFFFF);
        ctx.fill(tx + tooltipWidth - 1, ty, tx + tooltipWidth, ty + tooltipHeight, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, tooltip, tx + 5, ty + 5, 0xFFFFFFFF);
    }
}
