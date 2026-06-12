package com.client.multichatwindows.util;

import net.minecraft.text.Text;

public final class PathTextLinkifier {
    private PathTextLinkifier() {
    }

    public static Text linkifyFolderPaths(Text input) {
        return input == null ? Text.empty() : input.copy();
    }
}
