package com.client.multichatwindows.util;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class EventLog {
    private EventLog() {}

    private static Path path() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("multichatwindows")
                .resolve("events-log.txt");
    }

    public static void log(String type, String serverKey, String screen, String reason, String content) {
        try {
            String line =
                    "[" + Instant.now() + "]\t" +
                    safe(type) + "\t" +
                    safe(serverKey) + "\t" +
                    safe(screen) + "\t" +
                    safe(reason) + "\t" +
                    (content == null ? "" : content) +
                    System.lineSeparator();

            Files.createDirectories(path().getParent());
            Files.writeString(
                    path(),
                    line,
                    StandardCharsets.UTF_8,
                    Files.exists(path()) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
            );
        } catch (Exception ignored) {
        }
    }

    public static void filtered(String serverKey, String reason, String content) {
        log("FILTER", serverKey, "-", reason, content);
    }

    public static void notified(String serverKey, String screen, String reason, String content) {
        log("NOTIFY", serverKey, screen, reason, content);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\t", " ").replace("\n", " ").replace("\r", " ");
    }
}
