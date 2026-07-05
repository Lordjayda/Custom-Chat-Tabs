package com.client.multichatwindows.config.ui;

import com.client.multichatwindows.config.ConfigManager;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigFileTransferScreen extends ScrollableDarkScreen {
    private static final int ROW_HEIGHT = 22;
    private static final int MAX_VISIBLE_ROWS = 10;

    private final Screen parent;
    private final boolean importMode;

    private Path currentDir;
    private String selectedPath;
    private EditBox pathField;
    private Component status = Component.empty();
    private int scrollOffset = 0;

    public ConfigFileTransferScreen(Screen parent, boolean importMode) {
        super(Component.literal(importMode ? "Config Import" : "Config Export"));
        this.parent = parent;
        this.importMode = importMode;

        Path fallback = ConfigManager.exportFile().toAbsolutePath().normalize();
        this.selectedPath = fallback.toString();
        this.currentDir = fallback.getParent() == null ? ConfigManager.basePath().toAbsolutePath().normalize() : fallback.getParent();
    }

    @Override
    protected void init() {
        clearWidgets();
        ConfigManager.init();

        int centerX = width / 2;
        int fieldWidth = Math.min(620, Math.max(260, width - 40));
        int left = centerX - fieldWidth / 2;
        int y = 48;

        pathField = new EditBox(font, left, y, fieldWidth, 22, Component.literal("Config-Datei"));
        pathField.setMaxLength(4096);
        pathField.setValue(selectedPath == null ? "" : selectedPath);
        pathField.setResponder(value -> selectedPath = ConfigManager.normalizeUserPathText(value));
        addRenderableWidget(pathField);
        y += 28;

        addRenderableWidget(new DarkButton(left, y, 90, 20, Component.literal("Ordner öffnen"), () -> {
            Path path = pathFromField();
            if (path == null) {
                status = Component.literal("Ungültiger Pfad");
                return;
            }
            Path dir = Files.isDirectory(path) ? path : path.getParent();
            if (dir != null && Files.isDirectory(dir)) {
                currentDir = dir.toAbsolutePath().normalize();
                scrollOffset = 0;
                init();
            } else {
                status = Component.literal("Ordner nicht gefunden");
            }
        }));

        addRenderableWidget(new DarkButton(left + 96, y, 70, 20, Component.literal("Hoch"), () -> {
            if (currentDir != null && currentDir.getParent() != null) {
                currentDir = currentDir.getParent().toAbsolutePath().normalize();
                scrollOffset = 0;
                init();
            }
        }));

        addRenderableWidget(new DarkButton(left + 172, y, 90, 20, Component.literal("Config"), () -> {
            currentDir = ConfigManager.basePath().toAbsolutePath().normalize();
            scrollOffset = 0;
            init();
        }));

        addRenderableWidget(new DarkButton(left + 268, y, 90, 20, Component.literal("Neu laden"), () -> init()));

        addRenderableWidget(new DarkButton(left + fieldWidth - 120, y, 120, 20, Component.literal(importMode ? "Importieren" : "Exportieren"), () -> {
            if (importMode) {
                importSelected();
            } else {
                exportSelected();
            }
        }));
        y += 30;

        List<Path> entries = listEntries();
        int maxRows = Math.min(MAX_VISIBLE_ROWS, Math.max(0, (height - y - 54) / ROW_HEIGHT));
        int end = Math.min(entries.size(), scrollOffset + maxRows);
        for (int i = scrollOffset; i < end; i++) {
            Path entry = entries.get(i);
            boolean directory = Files.isDirectory(entry);
            String name = entry.getFileName() == null ? entry.toString() : entry.getFileName().toString();
            String label = directory ? "[DIR] " + name : name;
            int rowY = y + (i - scrollOffset) * ROW_HEIGHT;
            addRenderableWidget(new DarkButton(left, rowY, fieldWidth, 20, Component.literal(label), () -> {
                if (Files.isDirectory(entry)) {
                    currentDir = entry.toAbsolutePath().normalize();
                    scrollOffset = 0;
                    init();
                    return;
                }

                selectedPath = entry.toAbsolutePath().normalize().toString();
                if (pathField != null) {
                    pathField.setValue(selectedPath);
                }

                if (importMode) {
                    importSelected();
                }
            }));
        }

        addRenderableWidget(new DarkButton(centerX - 130, height - 28, 260, 20, Component.translatable("multichatwindows.back"), () -> {
            Minecraft.getInstance().setScreen(parent);
        }));
    }

    private List<Path> listEntries() {
        List<Path> result = new ArrayList<>();
        try {
            Path dir = currentDir == null ? ConfigManager.basePath().toAbsolutePath().normalize() : currentDir.toAbsolutePath().normalize();
            if (!Files.isDirectory(dir)) {
                dir = ConfigManager.basePath().toAbsolutePath().normalize();
            }
            currentDir = dir;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path path : stream) {
                    if (Files.isDirectory(path) || isJson(path)) {
                        result.add(path.toAbsolutePath().normalize());
                    }
                }
            }

            result.sort(Comparator
                    .comparing((Path p) -> !Files.isDirectory(p))
                    .thenComparing(p -> p.getFileName() == null ? p.toString().toLowerCase(Locale.ROOT) : p.getFileName().toString().toLowerCase(Locale.ROOT)));
        } catch (IOException ignored) {
            status = Component.literal("Ordner kann nicht gelesen werden");
        }
        return result;
    }

    private boolean isJson(Path path) {
        String name = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        return name.toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private Path pathFromField() {
        String raw = pathField == null ? selectedPath : pathField.getValue();
        raw = ConfigManager.normalizeUserPathText(raw);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Path path = Path.of(raw);
            if (!path.isAbsolute()) {
                Path base = currentDir == null ? ConfigManager.basePath() : currentDir;
                path = base.resolve(path);
            }
            return path.toAbsolutePath().normalize();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void importSelected() {
        Path path = pathFromField();
        if (path == null || !Files.isRegularFile(path)) {
            status = Component.literal("Import fehlgeschlagen: Datei nicht gefunden");
            return;
        }

        String imported = ConfigManager.importConfig(path);
        if (imported == null || imported.isBlank()) {
            status = Component.literal("Import fehlgeschlagen: ungültige Config-Datei");
            return;
        }

        com.client.multichatwindows.hud.WindowService.rebuildForCurrentServer();
        selectedPath = path.toString();
        status = Component.literal("Importiert: " + selectedPath);
    }

    private void exportSelected() {
        Path path = pathFromField();
        if (path == null) {
            status = Component.literal("Export fehlgeschlagen: ungültiger Pfad");
            return;
        }

        if (!isJson(path)) {
            path = Path.of(path.toString() + ".json").toAbsolutePath().normalize();
        }

        String exported = ConfigManager.exportConfig(path);
        if (exported == null || exported.isBlank()) {
            status = Component.literal("Export fehlgeschlagen");
            return;
        }

        selectedPath = path.toString();
        if (pathField != null) {
            pathField.setValue(selectedPath);
        }
        Path parent = path.getParent();
        if (parent != null) {
            currentDir = parent.toAbsolutePath().normalize();
        }
        status = Component.literal("Exportiert: " + selectedPath);
        init();
    }

    @Override
    protected boolean onScroll(int delta) {
        List<Path> entries = listEntries();
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, Math.max(1, (height - 108) / ROW_HEIGHT));
        int max = Math.max(0, entries.size() - visibleRows);
        int next = Math.max(0, Math.min(max, scrollOffset + (delta > 0 ? 1 : -1)));
        if (next != scrollOffset) {
            scrollOffset = next;
            init();
            return true;
        }
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        com.client.multichatwindows.util.GuiDrawHelper.centered(context, font, Component.literal(importMode ? "Config importieren" : "Config exportieren"), width / 2, 14, 0xFFFFFFFF);
        com.client.multichatwindows.util.GuiDrawHelper.centered(context, font, Component.literal("Klicke Ordner zum Öffnen oder JSON-Dateien zum Auswählen."), width / 2, 28, 0xFFB0B0B0);

        String dir = currentDir == null ? "" : currentDir.toString();
        com.client.multichatwindows.util.GuiDrawHelper.text(context, font, Component.literal("Ordner: " + trimMiddle(dir, 100)), 12, 104, 0xFFB0B0B0);

        if (status != null && !status.getString().isBlank()) {
            com.client.multichatwindows.util.GuiDrawHelper.centered(context, font, status, width / 2, height - 48, 0xFFB0FFB0);
        }
    }

    private String trimMiddle(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        int keep = Math.max(10, (maxLength - 3) / 2);
        return value.substring(0, keep) + "..." + value.substring(value.length() - keep);
    }
}
