package com.client.multichatwindows.hud;

import com.client.multichatwindows.config.ConfigManager;
import com.client.multichatwindows.config.model.DependencyRule;
import com.client.multichatwindows.config.model.GlobalConfig;
import com.client.multichatwindows.config.model.ServerConfig;
import com.client.multichatwindows.config.model.TabConfig;
import com.client.multichatwindows.util.DebugLog;
import com.client.multichatwindows.util.I18nUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WindowService {

    private static final Map<String, ChatWindow> WINDOWS = new LinkedHashMap<>();
    private static String lastServerKey = "";

    private WindowService() {}

    public static Collection<ChatWindow> allWindows() {
        ensureForCurrentServer();
        return WINDOWS.values();
    }

    public static String currentServerKey() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo si = mc.getCurrentServerEntry();

        if (si == null) return "singleplayer";

        return (si.address == null || si.address.isBlank()) ? "unknown" : si.address;
    }

    public static void ensureForCurrentServer() {
        String key = currentServerKey();

        // ✅ NUR bei Serverwechsel rebuild
        if (!key.equals(lastServerKey)) {
            rebuildForCurrentServer();
        }
    }

    public static void rebuildForCurrentServer() {
        String key = currentServerKey();
        lastServerKey = key;

        ServerConfig sc = ConfigManager.getOrCreateServer(key);

        // ✅ alte Windows behalten (inkl Chat)
        Map<String, ChatWindow> old = new LinkedHashMap<>(WINDOWS);
        WINDOWS.clear();

        for (TabConfig t : sc.tabs) {
            ChatWindow w = old.get(t.id);

            if (w == null) {
                w = new ChatWindow(t.id);
            }

            // ✅ nur Eigenschaften aktualisieren
            w.displayName = I18nUtil.tKeyOrLiteral(t.name).getString();
            w.enabled = t.enabled;
            w.useVanilla = t.useVanilla;

            w.x = t.x;
            w.y = t.y;
            w.w = t.width;
            w.h = t.height;

            w.opacity = t.opacity;
            w.textScale = Math.max(0.5f, Math.min(3.0f, t.textScale));

            w.outlineEnabled = t.outlineEnabled;
            w.outlineWidth = Math.max(1, Math.min(20, t.outlineWidth));
            w.outlineColor = parseColor(t.outlineColor);

            WINDOWS.put(t.id, w);
        }
    }
	
	public static void resetScrollOnChatClose() {
    // Kein ensureForCurrentServer() hier, damit es beim Schließen nicht triggert/rebuildet.
    for (ChatWindow w : WINDOWS.values()) {
        w.scrollOffset = 0;
    }
  }

    public static boolean scrollAt(double mouseX, double mouseY, double verticalAmount) {
        ensureForCurrentServer();

        ChatWindow target = null;

        for (ChatWindow w : WINDOWS.values()) {
            if (!w.enabled) continue;

            boolean inside =
                    mouseX >= w.x
                            && mouseX <= w.x + w.w
                            && mouseY >= w.y
                            && mouseY <= w.y + w.h;

            if (inside) {
                target = w;
            }
        }

        if (target == null) return false;

        int delta = verticalAmount > 0 ? 3 : -3;

        // ✅ korrektes limit (kein faktor 4 mehr)
        int max = Math.max(0, target.lines.size());

        target.scrollOffset = Math.max(0, Math.min(max, target.scrollOffset + delta));

        return true;
    }

    private static int parseColor(String hex) {
        if (hex == null) return 0xFFFFFF;

        String s = hex.trim();

        if (s.startsWith("#")) {
            s = s.substring(1);
        }

        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return 0xFFFFFF;
        }
    }

    private static String guessPlayerName(String plain) {
        if (plain == null) return null;

        int idx = plain.indexOf(':');
        if (idx <= 0) return null;

        String pre = plain.substring(0, idx).trim();

        // ✅ KEIN Regex → stabil
        pre = pre.replace("<", "").replace("[", "");
        pre = pre.replace(">", "").replace("]", "");

        // Vanilla Formatcodes entfernen
        pre = pre.replaceAll("§.", "").trim();

        if (pre.isEmpty()) return null;

        String[] parts = pre.split("\\s+");

        return parts.length == 0 ? pre : parts[parts.length - 1];
    }

    public static boolean routeIncoming(Text msg) {
        GlobalConfig g = ConfigManager.global();

        if (!g.enabled) return false;

        ensureForCurrentServer();

        String serverKey = currentServerKey();
        ServerConfig server = ConfigManager.getOrCreateServer(serverKey);

        String plain = msg == null ? "" : msg.getString();
        String player = guessPlayerName(plain);

        boolean filterFromAll = false;

        for (TabConfig tab : server.tabs) {
            if (tab == null || !tab.enabled) continue;
            if ("all".equalsIgnoreCase(tab.id)) continue;
            if (tab.dependencies == null || tab.dependencies.isEmpty()) continue;

            for (DependencyRule rule : tab.dependencies) {
                if (rule != null && rule.matches(player, plain)) {
                    ChatWindow w = WINDOWS.get(tab.id);

                    if (w != null) {
                        w.push(msg);
                        DebugLog.write(plain, w.displayName, rule.describe());
                    }

                    if (tab.filterAllChat) {
                        filterFromAll = true;
                    }

                    break;
                }
            }
        }

        if (!filterFromAll) {
            ChatWindow all = WINDOWS.get("all");

            if (all != null) {
                all.push(msg);
            }
        }

        return true;
    }
}
