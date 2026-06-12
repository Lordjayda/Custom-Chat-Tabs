package com.client.multichatwindows.config.ui;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UtilOpenHelper {
    private UtilOpenHelper() {
    }

    public static void openPath(Path path) {
        try {
            if (path == null) {
                return;
            }
            Files.createDirectories(path);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (Throwable ignored) {
        }
    }
}
