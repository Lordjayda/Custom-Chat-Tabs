package com.client.multichatwindows.mixin.client;

import com.client.multichatwindows.hud.ChatCopyMenu;
import com.client.multichatwindows.hud.WindowService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void mcw$renderCopyMenu(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }

        ChatCopyMenu.render(context, client.textRenderer);

        Style hoverStyle = WindowService.styleAt(mouseX, mouseY);
        if (hoverStyle != null && hoverStyle.getHoverEvent() != null) {
            context.drawHoverEvent(client.textRenderer, hoverStyle, mouseX, mouseY);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"), require = 0)
    private void mcw$removed(CallbackInfo ci) {
        ChatCopyMenu.close();
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcw$mouseClickedClickObject(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
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
            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = client == null ? 320 : client.getWindow().getScaledWidth();
            int screenHeight = client == null ? 240 : client.getWindow().getScaledHeight();

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

        if (chatField != null) {
            chatField.setText(suggestion);
            mcw$moveCursorToEnd(chatField, suggestion.length());
            chatField.setFocused(true);
            return true;
        }

        return false;
    }

    private void mcw$moveCursorToEnd(TextFieldWidget widget, int length) {
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