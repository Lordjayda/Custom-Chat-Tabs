package com.client.multichatwindows.util;

import net.minecraft.network.chat.Component;

public final class PathTextLinkifier {
    private PathTextLinkifier() {
    }

    public static Component linkifyFolderPaths(Component input) {
        return input == null ? Component.empty() : input.copy();
    }
}
