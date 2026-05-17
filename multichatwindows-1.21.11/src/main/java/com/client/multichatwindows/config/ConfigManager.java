package com.client.multichatwindows.config;

import com.client.multichatwindows.config.model.GlobalConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GlobalConfig GLOBAL;

    private ConfigManager() {
    }

    private static Path baseDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("multichatwindows");
    }

    private static Path globalFile() {
        return baseDir().resolve("global.json");
    }

    private static Path serversDir() {
        return baseDir().resolve("servers");
    }

    public static String safe(String key) {
        return key == null ? "unknown" : key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static Path serverDir(String serverKey) {
        return serversDir().resolve(safe(serverKey));
    }

    private static Path serverFile(String serverKey) {
        return serverDir(serverKey).resolve("server.json");
    }

    private static Path tabsDir(String serverKey) {
        return serverDir(serverKey).resolve("tabs");
    }

    private static Path tabFile(String serverKey, String tabId) {
        return tabsDir(serverKey).resolve(safe(tabId) + ".json");
    }

    public static void init() {
        if (GLOBAL != null) return;

        try {
            Files.createDirectories(baseDir());
            Files.createDirectories(serversDir());
        } catch (IOException ignored) {
        }

        GLOBAL = read(globalFile(), GlobalConfig.class, new GlobalConfig());
    }

    public static GlobalConfig global() {
        init();
        return GLOBAL;
    }

    public static void saveGlobal() {
        init();
        write(globalFile(), GLOBAL);
    }

    public static ServerConfig getOrCreateServer(String serverKey) {
        init();

        boolean isSingle = "singleplayer".equalsIgnoreCase(serverKey);
        Path sf = serverFile(serverKey);

        ServerConfig sc = read(sf, ServerConfig.class, null);

        if (sc == null) {
            sc = new ServerConfig();
            sc.serverKey = serverKey;
            ensureAllTab(sc);

            if (!isSingle && serverKey != null && !serverKey.isBlank()) {
                write(sf, sc);

                for (TabConfig t : sc.tabs) {
                    saveTab(serverKey, t);
                }
            }
        } else {
            sc.serverKey = serverKey;
            loadTabsFromFolder(sc);
            ensureAllTab(sc);
        }

        return sc;
    }

    public static void saveServer(String serverKey, ServerConfig sc) {
        init();

        if (sc == null) return;

        sc.serverKey = serverKey;
        ensureAllTab(sc);

        boolean isSingle = "singleplayer".equalsIgnoreCase(serverKey);
        Path sf = serverFile(serverKey);

        if (isSingle && !Files.exists(sf)) return;

        write(sf, sc);

        for (TabConfig t : sc.tabs) {
            saveTab(serverKey, t);
        }
    }

    public static void saveServer(String serverKey) {
        saveServer(serverKey, getOrCreateServer(serverKey));
    }

    public static void saveTab(String serverKey, TabConfig tab) {
        init();

        if (tab == null || tab.id == null || tab.id.isBlank()) return;
        if ("singleplayer".equalsIgnoreCase(serverKey) && !Files.exists(serverFile(serverKey))) return;

        try {
            Files.createDirectories(tabsDir(serverKey));
        } catch (IOException ignored) {
        }

        write(tabFile(serverKey, tab.id), tab);
    }

    public static void deleteTab(String serverKey, String tabId) {
        init();

        if (tabId == null || tabId.isBlank()) return;
        if ("all".equalsIgnoreCase(tabId)) return;

        try {
            Files.deleteIfExists(tabFile(serverKey, tabId));
        } catch (IOException ignored) {
        }
    }

    public static List<String> listServers() {
        init();

        List<String> out = new ArrayList<>();

        try {
            if (!Files.exists(serversDir())) return out;

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(serversDir())) {
                for (Path p : ds) {
                    if (Files.isDirectory(p)) {
                        out.add(p.getFileName().toString());
                    }
                }
            }
        } catch (IOException ignored) {
        }

        Collections.sort(out);
        return out;
    }

    public static void deleteServer(String serverKey) {
        Path dir = serverDir(serverKey);

        if (!Files.exists(dir)) return;

        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private static void loadTabsFromFolder(ServerConfig sc) {
        try {
            Path td = tabsDir(sc.serverKey);

            if (!Files.exists(td)) return;

            List<TabConfig> tabs = new ArrayList<>();

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(td, "*.json")) {
                for (Path f : ds) {
                    TabConfig t = read(f, TabConfig.class, null);

                    if (t != null && t.id != null && !t.id.isBlank()) {
                        tabs.add(t);
                    }
                }
            }

            if (!tabs.isEmpty()) {
                sc.tabs = tabs;
            }
        } catch (IOException ignored) {
        }
    }

    private static void ensureAllTab(ServerConfig sc) {
        if (sc.tabs == null) {
            sc.tabs = new ArrayList<>();
        }

        for (TabConfig t : sc.tabs) {
            if (t != null && "all".equalsIgnoreCase(t.id)) {
                if (t.name == null || t.name.isBlank()) {
                    t.name = "multichatwindows.all_tab";
                }

                t.filterAllChat = false;

                if (t.dependencies != null) {
                    t.dependencies.clear();
                }

                return;
            }
        }

        TabConfig all = new TabConfig();
        all.id = "all";
        all.name = "multichatwindows.all_tab";
        all.enabled = true;
        all.useVanilla = true;
        all.filterAllChat = false;

        sc.tabs.add(0, all);
    }

    private static <T> T read(Path file, Class<T> type, T def) {
        try {
            if (!Files.exists(file)) return def;

            T v = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), type);
            return v == null ? def : v;
        } catch (Throwable ignored) {
            return def;
        }
    }

    private static void write(Path file, Object obj) {
        try {
            Files.createDirectories(file.getParent());

            Files.writeString(
                    file,
                    GSON.toJson(obj),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Throwable ignored) {
        }
    }
}