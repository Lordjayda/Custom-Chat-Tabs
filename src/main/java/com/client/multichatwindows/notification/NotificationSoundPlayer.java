package com.client.multichatwindows.notification;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.NotificationConfig;
import javazoom.jl.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NotificationSoundPlayer {
    private static final List<Clip> ACTIVE_CLIPS = new CopyOnWriteArrayList<>();

    private NotificationSoundPlayer() {
    }

    public static void play(NotificationConfig notification) {
        playInternal(notification, true);
    }

    public static void playNotification(NotificationConfig notification) {
        playInternal(notification, true);
    }

    public static void playTest(NotificationConfig notification) {
        playInternal(notification, false);
    }

    private static void playInternal(NotificationConfig notification, boolean allowMinecraftFallback) {
        if (notification == null) {
            return;
        }

        String customPath = notification.customSoundPath == null ? "" : ConfigManager.normalizeUserPathText(notification.customSoundPath);
        boolean hasCustomPath = !customPath.isBlank();
        if (!notification.soundEnabled && !hasCustomPath) {
            return;
        }

        if (hasCustomPath) {
            Path path = ConfigManager.resolveUserPath(customPath);
            if (Files.isRegularFile(path) && playCustom(path, clamp(notification.volume, 0.0f, 2.0f))) {
                return;
            }
            return;
        }

        if (allowMinecraftFallback) {
            playMinecraftFallback(notification);
        }
    }

    private static boolean playCustom(Path path, float volume) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".mp3")) {
            return playMp3(path);
        }
        return playJavaSound(path, volume);
    }

    private static boolean playMp3(Path path) {
        Thread thread = new Thread(() -> {
            try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
                Player player = new Player(in);
                player.play();
            } catch (Throwable ignored) {
            }
        }, "MultiChatWindows-MP3NotificationSound");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private static boolean playJavaSound(Path path, float volume) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile());
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            stream.close();
            applyVolume(clip, volume);
            ACTIVE_CLIPS.add(clip);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP || event.getType() == LineEvent.Type.CLOSE) {
                    ACTIVE_CLIPS.remove(clip);
                    clip.close();
                }
            });
            clip.start();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void playMinecraftFallback(NotificationConfig notification) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> client.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.UI_BUTTON_CLICK,
                    clamp(notification.pitch, 0.5f, 2.0f)
            )));
        }
    }

    private static void applyVolume(Clip clip, float volume) {
        if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float normalized = clamp(volume, 0.0f, 2.0f) / 2.0f;
        if (normalized <= 0.0f) {
            control.setValue(control.getMinimum());
            return;
        }
        float db = 20.0f * (float) Math.log10(normalized);
        control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), db)));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
