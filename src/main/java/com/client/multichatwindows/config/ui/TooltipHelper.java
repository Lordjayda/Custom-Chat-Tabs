package com.client.multichatwindows.config.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class TooltipHelper {
    private TooltipHelper() {
    }

    public static void renderIfHovered(
            GuiGraphicsExtractor ctx,
            Font textRenderer,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            Component text
    ) {
        if (text == null || text.getString().isBlank()) {
            return;
        }

        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return;
        }

        String tooltip = text.getString();
        int tooltipWidth = Math.min(textRenderer.width(tooltip) + 10, 360);
        int tooltipHeight = 18;
        int tx = Math.min(mouseX + 10, ctx.guiWidth() - tooltipWidth - 4);
        int ty = Math.min(mouseY + 10, ctx.guiHeight() - tooltipHeight - 4);

        ctx.fill(tx, ty, tx + tooltipWidth, ty + tooltipHeight, 0xEE101010);
        ctx.fill(tx, ty, tx + tooltipWidth, ty + 1, 0xFFFFFFFF);
        ctx.fill(tx, ty + tooltipHeight - 1, tx + tooltipWidth, ty + tooltipHeight, 0xFFFFFFFF);
        ctx.fill(tx, ty, tx + 1, ty + tooltipHeight, 0xFFFFFFFF);
        ctx.fill(tx + tooltipWidth - 1, ty, tx + tooltipWidth, ty + tooltipHeight, 0xFFFFFFFF);
        com.client.multichatwindows.util.GuiDrawHelper.text(ctx, textRenderer, tooltip, tx + 5, ty + 5, 0xFFFFFFFF);
    }
}
