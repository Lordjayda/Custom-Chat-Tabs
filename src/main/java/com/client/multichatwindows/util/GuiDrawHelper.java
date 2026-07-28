package com.client.multichatwindows.util;

import com.client.multichatwindows.mixin.client.GuiGraphicsExtractorAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class GuiDrawHelper {
    private GuiDrawHelper() {
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color);
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y, int color) {
        graphics.text(font, text, x, y, color);
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        graphics.text(font, text, x, y, color);
    }

    public static void centered(GuiGraphicsExtractor graphics, Font font, Component text, int centerX, int y, int color) {
        graphics.centeredText(font, text, centerX, y, color);
    }

    public static void centered(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int centerX, int y, int color) {
        graphics.centeredText(font, text, centerX, y, color);
    }

    public static void centered(GuiGraphicsExtractor graphics, Font font, String text, int centerX, int y, int color) {
        graphics.centeredText(font, text, centerX, y, color);
    }

    public static void hover(GuiGraphicsExtractor graphics, Font font, Style style, int mouseX, int mouseY) {
        if (graphics == null || font == null || style == null) {
            return;
        }
        ((GuiGraphicsExtractorAccessor) graphics).multichatwindows$componentHoverEffect(font, style, mouseX, mouseY);
    }
}
