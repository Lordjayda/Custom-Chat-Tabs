package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class NotificationBasicsScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int notificationIndex;
    private TextFieldWidget keywordField;

    public NotificationBasicsScreen(Screen parent, String serverKey, int notificationIndex) {
        super(Text.translatable("multichatwindows.notifications.trigger"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.notificationIndex = notificationIndex;
    }

    @Override
    protected void init() {
        clearChildren();
        ServerConfig serverConfig = ConfigManager.getOrCreateServer(serverKey);
        if (notificationIndex < 0 || notificationIndex >= serverConfig.notifications.size()) {
            addBackButton();
            return;
        }

        NotificationConfig notification = serverConfig.notifications.get(notificationIndex);
        int centerX = width / 2;
        int y = 54;

        addDrawableChild(new DarkButton(centerX - 120, y, 240, 22, Text.translatable(notification.enabled ? "multichatwindows.notifications.enabled.on" : "multichatwindows.notifications.enabled.off"), () -> {
            notification.enabled = !notification.enabled;
            ConfigManager.saveServer(serverKey, serverConfig);
            init();
        }));

        y += 32;
        addDrawableChild(new DarkButton(centerX - 120, y, 240, 22, Text.translatable((notification.keyword != null && !notification.keyword.isBlank()) ? "multichatwindows.notifications.mode.keyword" : "multichatwindows.notifications.mode.any"), () -> {
            if (notification.keyword != null && !notification.keyword.isBlank()) {
                notification.keyword = "";
                notification.anyMessage = true;
            } else {
                notification.keyword = "hi";
                notification.anyMessage = false;
            }
            ConfigManager.saveServer(serverKey, serverConfig);
            init();
        }));

        y += 32;
        keywordField = new TextFieldWidget(textRenderer, centerX - 120, y, 240, 22, Text.translatable("multichatwindows.notifications.keyword"));
        keywordField.setText(notification.keyword == null ? "" : notification.keyword);
        keywordField.setChangedListener(value -> {
            notification.keyword = value;
            notification.anyMessage = value == null || value.isBlank();
            ConfigManager.saveServer(serverKey, serverConfig);
        });
        addDrawableChild(keywordField);

        y += 32;
        addDrawableChild(new DarkButton(centerX - 120, y, 240, 22, Text.translatable(notification.screenDependent ? "multichatwindows.notifications.screenDependent.on" : "multichatwindows.notifications.screenDependent.off"), () -> {
            notification.screenDependent = !notification.screenDependent;
            ConfigManager.saveServer(serverKey, serverConfig);
            init();
        }));

        

        addBackButton();
    }

    private void addBackButton() {
        addDrawableChild(new DarkButton(width / 2 - 120, height - 28, 240, 20, Text.translatable("multichatwindows.back"), () -> MinecraftClient.getInstance().setScreen(parent)));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("multichatwindows.notifications.trigger"), width / 2, 14, 0xFFFFFFFF);
        TooltipHelper.renderIfHovered(ctx, textRenderer, mouseX, mouseY, keywordField.getX(), keywordField.getY(), 240, 22, Text.translatable("multichatwindows.tooltip.notification.keyword"));
    }
}
