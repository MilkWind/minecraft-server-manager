package minecraft.milkwind.manager.server.service;

import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.server.dto.LogEntryDto;
import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import minecraft.milkwind.manager.server.runtime.MinecraftLogParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Service
public class ServerLogService {

    private static final String LOGS_DIRECTORY_NAME = "logs";
    private static final String LATEST_LOG_FILE_NAME = "latest.log";
    private static final Pattern ARCHIVED_LOG_FILE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})-(\\d+)\\.log\\.gz$");
    private static final DateTimeFormatter ARCHIVED_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_TOKEN_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConcurrentHashMap<String, CachedFileLog> fileCache = new ConcurrentHashMap<>();

    public List<LogEntryDto> readLogs(ServerConfigEntity config) {
        Path logsDirectory = Path.of(config.getRootDirectory(), LOGS_DIRECTORY_NAME);
        if (!Files.isDirectory(logsDirectory)) {
            return List.of(buildPlaceholder("No log files were found in the server logs directory."));
        }

        List<Path> logFiles = discoverLogFiles(logsDirectory);
        if (logFiles.isEmpty()) {
            return List.of(buildPlaceholder("No log files were found in the server logs directory."));
        }

        List<LogEntryDto> entries = new ArrayList<>();

        for (Path logFile : logFiles) {
            entries.addAll(readLogFile(logFile));
        }

        return entries.isEmpty()
                ? List.of(buildPlaceholder("Log files were found, but no readable log lines were produced."))
                : entries;
    }

    private List<Path> discoverLogFiles(Path logsDirectory) {
        try (var stream = Files.list(logsDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedLogFile)
                    .sorted(logFileComparator())
                    .toList();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "log_scan_failed", "扫描服务器日志文件失败");
        }
    }

    private boolean isSupportedLogFile(Path path) {
        String fileName = path.getFileName().toString();
        return LATEST_LOG_FILE_NAME.equalsIgnoreCase(fileName)
                || ARCHIVED_LOG_FILE_PATTERN.matcher(fileName).matches();
    }

    private Comparator<Path> logFileComparator() {
        return Comparator
                .comparing(this::logFileOrderKey)
                .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER);
    }

    private LogFileOrderKey logFileOrderKey(Path path) {
        String fileName = path.getFileName().toString();
        Matcher matcher = ARCHIVED_LOG_FILE_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            return new LogFileOrderKey(
                    0,
                    LocalDate.parse(matcher.group(1), ARCHIVED_DATE_FORMATTER),
                    Integer.parseInt(matcher.group(2))
            );
        }

        return new LogFileOrderKey(1, resolveFileDate(path), Integer.MAX_VALUE);
    }

    private List<LogEntryDto> readLogFile(Path path) {
        String cacheKey = path.toAbsolutePath().normalize().toString();
        try {
            long fileSize = Files.size(path);
            FileTime lastModified = Files.getLastModifiedTime(path);
            CachedFileLog cached = fileCache.get(cacheKey);

            if (cached != null && cached.size() == fileSize && cached.lastModifiedMillis() == lastModified.toMillis()) {
                return cached.entries();
            }

            List<LogEntryDto> parsedEntries = parseLogFile(path, lastModified);
            fileCache.put(cacheKey, new CachedFileLog(fileSize, lastModified.toMillis(), parsedEntries));
            return parsedEntries;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "log_read_failed", "读取服务器日志文件失败");
        }
    }

    private List<LogEntryDto> parseLogFile(Path path, FileTime lastModified) throws IOException {
        LocalDate fileDate = resolveFileDate(path);
        Instant rollingTimestamp = fileDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<LogEntryDto> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openLogStream(path), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                MinecraftLogParser.ParsedLogLine parsed = MinecraftLogParser.parse(line);
                Instant resolvedTimestamp = resolveTimestamp(parsed.timeToken(), fileDate);
                if (resolvedTimestamp != null) {
                    rollingTimestamp = resolvedTimestamp;
                }

                entries.add(new LogEntryDto(
                        path.getFileName() + ":" + lineNumber,
                        rollingTimestamp != null ? rollingTimestamp : lastModified.toInstant(),
                        parsed.level(),
                        parsed.source(),
                        parsed.message(),
                        false
                ));
            }
        }

        return entries;
    }

    private InputStream openLogStream(Path path) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        if (path.getFileName().toString().endsWith(".gz")) {
            return new GZIPInputStream(inputStream);
        }
        return inputStream;
    }

    private LocalDate resolveFileDate(Path path) {
        Matcher matcher = ARCHIVED_LOG_FILE_PATTERN.matcher(path.getFileName().toString());
        if (matcher.matches()) {
            return LocalDate.parse(matcher.group(1), ARCHIVED_DATE_FORMATTER);
        }

        try {
            return Files.getLastModifiedTime(path)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (IOException exception) {
            return LocalDate.now();
        }
    }

    private Instant resolveTimestamp(String timeToken, LocalDate fileDate) {
        if (timeToken == null || timeToken.isBlank()) {
            return null;
        }

        try {
            LocalTime time = LocalTime.parse(timeToken.trim(), TIME_TOKEN_FORMATTER);
            return fileDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LogEntryDto buildPlaceholder(String message) {
        return new LogEntryDto(
                "manager-log-placeholder",
                Instant.now(),
                "INFO",
                "manager",
                message,
                false
        );
    }

    private record CachedFileLog(
            long size,
            long lastModifiedMillis,
            List<LogEntryDto> entries
    ) {
    }

    private record LogFileOrderKey(
            int kind,
            LocalDate date,
            int sequence
    ) implements Comparable<LogFileOrderKey> {
        @Override
        public int compareTo(LogFileOrderKey other) {
            int kindCompare = Integer.compare(kind, other.kind);
            if (kindCompare != 0) {
                return kindCompare;
            }

            int dateCompare = date.compareTo(other.date);
            if (dateCompare != 0) {
                return dateCompare;
            }

            return Integer.compare(sequence, other.sequence);
        }
    }
}
