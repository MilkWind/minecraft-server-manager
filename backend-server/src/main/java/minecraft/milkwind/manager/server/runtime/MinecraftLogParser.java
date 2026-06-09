package minecraft.milkwind.manager.server.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MinecraftLogParser {

    private static final Pattern STRUCTURED_LOG_PATTERN = Pattern.compile(
            "^\\[(?<time>[^\\]]+)]\\s*\\[(?<thread>[^\\]/]+)/(?<level>[A-Z]+)]\\s*:?"
                    + "\\s*(?:(?:\\[(?<marker>[^\\]]+)])\\s*)?(?<message>.*)$"
    );

    private MinecraftLogParser() {
    }

    public static ParsedLogLine parse(String line) {
        Matcher structuredMatcher = STRUCTURED_LOG_PATTERN.matcher(line);
        if (structuredMatcher.matches()) {
            String level = normalizeLevel(structuredMatcher.group("level"));
            String message = structuredMatcher.group("message").trim();
            return new ParsedLogLine(
                    level,
                    "minecraft",
                    message.isBlank() ? line : message,
                    structuredMatcher.group("time")
            );
        }

        return new ParsedLogLine("INFO", "minecraft", line, null);
    }

    private static String normalizeLevel(String level) {
        return switch (level) {
            case "WARN", "WARNING" -> "WARN";
            case "ERROR", "SEVERE" -> "ERROR";
            case "DEBUG", "TRACE" -> "DEBUG";
            default -> "INFO";
        };
    }

    public record ParsedLogLine(
            String level,
            String source,
            String message,
            String timeToken
    ) {
    }
}
