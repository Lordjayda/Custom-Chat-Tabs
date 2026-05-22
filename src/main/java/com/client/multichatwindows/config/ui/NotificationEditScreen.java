package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class NotificationEditScreen extends ScrollableDarkScreen {
    private final Screen parent;
    private final String serverKey;
    private final int notifIndex;

    private ServerConfig sc;
    private NotificationConfig n;

    private TextFieldWidget keywordField;

    public NotificationEditScreen(Screen parent, String serverKey, int notifIndex) {
        super(Text.translatable("multichatwindows.notifications.edit.title"));
        this.parent = parent;
        this.serverKey = serverKey;
        this.notifIndex = notifIndex;
    }

    @Override
    protected void init() {
        clearChildren();
        sc = ConfigManager.getOrCreateServer(serverKey);
        if (sc.notifications == null || notifIndex < 0 || notifIndex >= sc.notifications.size()) {
            addDrawableChild(new DarkButton(
                    width / 2 - 100, height - 28, 200, 20,
                    Text.translatable("multichatwindows.back"),
                    () -> MinecraftClient.getInstance().setScreen(parent)
            ));
            return;
        }

        n = sc.notifications.get(notifIndex);
        int cx = width / 2;
        int y = 46;

        addDrawableChild(new DarkButton(
                cx - 100, y, 200, 20,
                Text.translatable(n.enabled
                        ? "multichatwindows.notifications.enabled.on"
                        : "multichatwindows.notifications.enabled.off"),
                () -> {
                    n.enabled = !n.enabled;
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));
        y += 26;

        // Mode toggle
        boolean keywordMode = (n.keyword != null && !n.keyword.isBlank());
        addDrawableChild(new DarkButton(
                cx - 100, y, 200, 20,
                Text.translatable(keywordMode
                        ? "multichatwindows.notifications.mode.keyword"
                        : "multichatwindows.notifications.mode.any"),
                () -> {
                    if (n.keyword != null && !n.keyword.isBlank()) {
                        n.keyword = "";
                        n.anyMessage = true;
                    } else {
                        n.anyMessage = false;
                        n.keyword = "hi"; // Default nur als Startwert, kann direkt überschrieben werden
                    }
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));
        y += 26;

        keywordField = new TextFieldWidget(
                textRenderer, cx - 100, y, 200, 20,
                Text.translatable("multichatwindows.notifications.keyword")
        );
        keywordField.setText(n.keyword == null ? "" : n.keyword);
        keywordField.setChangedListener(s -> {
            n.keyword = s;
            if (s != null && !s.isBlank()) {
                n.anyMessage = false;
            }
            ConfigManager.saveServer(serverKey, sc);
        });
        keywordField.setEditable(true);
        addDrawableChild(keywordField);
        y += 26;

        addDrawableChild(new DarkButton(
                cx - 100, y, 200, 20,
                Text.translatable(n.screenDependent
                        ? "multichatwindows.notifications.screenDependent.on"
                        : "multichatwindows.notifications.screenDependent.off"),
                () -> {
                    n.screenDependent = !n.screenDependent;
                    ConfigManager.saveServer(serverKey, sc);
                    init();
                }
        ));
        y += 26;

        addDrawableChild(new DarkButton(
                cx - 100, y, 200, 20,
                Text.translatable("multichatwindows.notifications.screens.select"),
                () -> MinecraftClient.getInstance().setScreen(
                        new ScreenSelectionScreen(this, serverKey, notifIndex)
                )
        ));

        addDrawableChild(new DarkButton(
                cx - 100, height - 54, 200, 20,
                Text.translatable("multichatwindows.save"),
                () -> {
                    // apply final keyword text
                    n.keyword = keywordField.getText();
                    if (n.keyword != null && !n.keyword.isBlank()) n.anyMessage = false;
                    ConfigManager.saveServer(serverKey, sc);
                    MinecraftClient.getInstance().setScreen(parent);
                }
        ));

        addDrawableChild(new DarkButton(
                cx - 100, height - 28, 200, 20,
                Text.translatable("multichatwindows.back"),
                () -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.notifications.edit.title"),
                width / 2,
                14,
                0xFFFFFFFF
        );
        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("multichatwindows.notifications.edit.hint"),
                width / 2,
                28,
                0xFFB0B0B0
        );
    }
}