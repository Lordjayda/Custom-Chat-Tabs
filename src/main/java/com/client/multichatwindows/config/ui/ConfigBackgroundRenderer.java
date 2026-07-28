package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.GlobalConfig;
import com.mojang.blaze3d.platform.NativeImage;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigBackgroundRenderer {
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("multichatwindows", "config_background");
    private static String loadedPath = "";
    private static int textureWidth = 0;
    private static int textureHeight = 0;
    private static boolean loadFailed = false;

    private ConfigBackgroundRenderer() {
    }

    public static boolean render(GuiGraphicsExtractor context, int width, int height) {
        GlobalConfig config = ConfigManager.global();
        if (config == null || !config.configBackgroundEnabled || config.configBackgroundPath == null || config.configBackgroundPath.isBlank()) {
            return false;
        }
        if (!ensureLoaded(config.configBackgroundPath)) {
            return false;
        }

        context.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE_ID,
                0,
                0,
                0.0f,
                0.0f,
                width,
                height,
                textureWidth,
                textureHeight,
                textureWidth,
                textureHeight
        );

        float opacity = clamp(config.configBackgroundOpacity, 0.0f, 1.0f);
        int overlayAlpha = Math.round((1.0f - opacity) * 150.0f);
        context.fill(0, 0, width, height, (overlayAlpha << 24));
        context.fill(0, 0, width, height, 0x22000000);
        return true;
    }

    public static void invalidate() {
        loadedPath = "";
        textureWidth = 0;
        textureHeight = 0;
        loadFailed = false;
    }

    private static boolean ensureLoaded(String value) {
        String requestedPath = ConfigManager.normalizeUserPathText(value);
        if (requestedPath.isBlank()) {
            return false;
        }
        if (requestedPath.equals(loadedPath) && !loadFailed) {
            return textureWidth > 0 && textureHeight > 0;
        }
        loadedPath = requestedPath;
        textureWidth = 0;
        textureHeight = 0;
        loadFailed = false;

        Path imagePath;
        try {
            imagePath = ConfigManager.resolveUserPath(requestedPath);
        } catch (Throwable ignored) {
            loadFailed = true;
            return false;
        }
        if (!Files.isRegularFile(imagePath)) {
            loadFailed = true;
            return false;
        }

        try {
            NativeImage image = readNativeImage(imagePath);
            textureWidth = image.getWidth();
            textureHeight = image.getHeight();
            DynamicTexture texture = new DynamicTexture(() -> "multichatwindows_config_background", image);
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.register(TEXTURE_ID, texture);
            texture.upload();
            return true;
        } catch (Throwable ignored) {
            loadFailed = true;
            textureWidth = 0;
            textureHeight = 0;
            return false;
        }
    }

    private static NativeImage readNativeImage(Path imagePath) throws Exception {
        try (InputStream in = Files.newInputStream(imagePath)) {
            return NativeImage.read(in);
        } catch (Throwable ignored) {
        }

        BufferedImage bufferedImage = ImageIO.read(imagePath.toFile());
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Unsupported image file");
        }

        BufferedImage argbImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = argbImage.createGraphics();
        graphics.drawImage(bufferedImage, 0, 0, null);
        graphics.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(argbImage, "png", out);
            return NativeImage.read(new ByteArrayInputStream(out.toByteArray()));
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
