package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.ChatCopyMenu;
import com.client.multichatwindows.hud.WindowService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Style;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected EditBox input;

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void mcw$renderCopyMenu(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return;
        }

        ChatCopyMenu.render(context, client.font);

        Style hoverStyle = WindowService.styleAt(mouseX, mouseY);
        if (hoverStyle != null && hoverStyle.getHoverEvent() != null) {
            com.client.multichatwindows.util.GuiDrawHelper.hover(context, client.font, hoverStyle, mouseX, mouseY);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"), require = 0)
    private void mcw$removed(CallbackInfo ci) {
        ChatCopyMenu.close();
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$mouseClickedClickObject(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        handleClick(click.x(), click.y(), click.button(), cir);
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$mouseClickedLegacy(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        handleClick(mouseX, mouseY, button, cir);
    }

    private void handleClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (ChatCopyMenu.isVisible()) {
            if (ChatCopyMenu.handleClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                cir.cancel();
            }
            return;
        }

        if (button == 1) {
            Minecraft client = Minecraft.getInstance();
            int screenWidth = client == null ? 320 : client.getWindow().getGuiScaledWidth();
            int screenHeight = client == null ? 240 : client.getWindow().getGuiScaledHeight();

            if (ChatCopyMenu.open(mouseX, mouseY, screenWidth, screenHeight)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        if (button == 0 && WindowService.handleClickAt(mouseX, mouseY, this::mcw$setChatInputText)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    private boolean mcw$setChatInputText(String value) {
        if (value == null) {
            return false;
        }

        String suggestion = value;

        if (input != null) {
            input.setValue(suggestion);
            mcw$moveCursorToEnd(input, suggestion.length());
            input.setFocused(true);
            return true;
        }

        return false;
    }

    private void mcw$moveCursorToEnd(EditBox widget, int length) {
        if (widget == null) {
            return;
        }

        try {
            for (Method method : widget.getClass().getMethods()) {
                if (method.getName().equals("setCursorToEnd") && method.getParameterCount() == 0) {
                    method.invoke(widget);
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            for (Method method : widget.getClass().getMethods()) {
                if (method.getName().equals("setCursorToEnd") && method.getParameterCount() == 1) {
                    Class<?> type = method.getParameterTypes()[0];
                    if (type == boolean.class || type == Boolean.class) {
                        method.invoke(widget, false);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            for (Method method : widget.getClass().getMethods()) {
                if ((method.getName().equals("setCursor") || method.getName().equals("setCursorPosition")) && method.getParameterCount() == 1) {
                    Class<?> type = method.getParameterTypes()[0];
                    if (type == int.class || type == Integer.class) {
                        method.invoke(widget, length);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}