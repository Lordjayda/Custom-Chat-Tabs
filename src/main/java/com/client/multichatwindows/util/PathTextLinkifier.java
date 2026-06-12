package com.client.multichatwindows.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PathTextLinkifier {
    private static final Pattern PATH_PATTERN = Pattern.compile(
            "(?i)(\\\"[^\\\"\\r\\n]*(?:voicechat|voice[_ -]?chat|recordings|recording|\\.minecraft)[^\\\"\\r\\n]*[\\\\/][^\\\"\\r\\n]*\\\"|" +
                    "'[^'\\r\\n]*(?:voicechat|voice[_ -]?chat|recordings|recording|\\.minecraft)[^'\\r\\n]*[\\\\/][^'\\r\\n]*'|" +
                    "[A-Za-z]:[\\\\/][^\\s<>\\\"']+|" +
                    "~[\\\\/][^\\s<>\\\"']+|" +
                    "\\.{1,2}[\\\\/][^\\s<>\\\"']+|" +
                    "(?:[^\\s<>\\\"']*(?:voicechat|voice[_ -]?chat|recordings|recording|\\.minecraft)[^\\s<>\\\"']*[\\\\/][^\\s<>\\\"']+)|" +
                    "/[^\\s<>\\\"']+)"
    );

    private PathTextLinkifier() {
    }

    public static Text linkifyFolderPaths(Text input) {
        if (input == null) {
            return Text.empty();
        }

        String plain = input.getString();
        if (plain == null || plain.isBlank()) {
            return input.copy();
        }

        Matcher matcher = PATH_PATTERN.matcher(plain);
        MutableText result = Text.empty();
        int last = 0;
        boolean changed = false;

        while (matcher.find()) {
            PathCandidate candidate = normalizeCandidate(matcher.group());
            if (candidate == null) {
                continue;
            }

            int displayStart = matcher.start() + candidate.leadingTrim;
            int displayEnd = matcher.end() - candidate.trailingTrim;
            if (displayStart < last || displayEnd <= displayStart) {
                continue;
            }

            if (displayStart > last) {
                result.append(Text.literal(plain.substring(last, displayStart)));
            }

            result.append(clickablePathText(plain.substring(displayStart, displayEnd), candidate.path));
            last = displayEnd;
            changed = true;
        }

        if (!changed) {
            return input.copy();
        }

        if (last < plain.length()) {
            result.append(Text.literal(plain.substring(last)));
        }

        return result;
    }

    private static MutableText clickablePathText(String display, Path path) {
        return Text.literal(display).styled(style -> style
                .withClickEvent(new ClickEvent.OpenFile(path))
                .withColor(Formatting.AQUA)
                .withUnderline(true));
    }

    private static PathCandidate normalizeCandidate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        int start = 0;
        int end = raw.length();

        while (start < end && isOpeningWrapper(raw.charAt(start))) {
            start++;
        }
        while (end > start && isTrailingPunctuation(raw.charAt(end - 1))) {
            end--;
        }

        if (end <= start) {
            return null;
        }

        String candidate = raw.substring(start, end).trim();
        if (candidate.isBlank() || candidate.contains("://")) {
            return null;
        }
        if (!(candidate.indexOf('/') >= 0 || candidate.indexOf('\\') >= 0)) {
            return null;
        }
        if (candidate.length() < 3) {
            return null;
        }

        try {
            Path path = resolvePath(candidate);
            return new PathCandidate(path, start, raw.length() - end);
        } catch (InvalidPathException ignored) {
            return null;
        }
    }

    private static Path resolvePath(String value) {
        String expanded = value;
        if (expanded.startsWith("~") && (expanded.length() == 1 || expanded.charAt(1) == '/' || expanded.charAt(1) == '\\')) {
            String home = System.getProperty("user.home", "");
            if (!home.isBlank()) {
                expanded = home + expanded.substring(1);
            }
        }

        Path path = Path.of(expanded);
        if (path.isAbsolute()) {
            return path.normalize();
        }

        return FabricLoader.getInstance().getGameDir().resolve(path).normalize();
    }

    private static boolean isOpeningWrapper(char c) {
        return c == '"' || c == '\'' || c == '<' || c == '(' || c == '[' || c == '{';
    }

    private static boolean isTrailingPunctuation(char c) {
        return c == '"' || c == '\'' || c == '>' || c == ')' || c == ']' || c == '}' || c == '.' || c == ',' || c == ';' || c == ':';
    }

    private record PathCandidate(Path path, int leadingTrim, int trailingTrim) {
    }
}
