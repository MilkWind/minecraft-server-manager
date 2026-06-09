package minecraft.milkwind.manager.server.service;

import minecraft.milkwind.manager.server.dto.LogEntryDto;
import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ServerLogServiceTest {

    private final ServerLogService serverLogService = new ServerLogService();

    @TempDir
    Path tempDirectory;

    @Test
    void readsArchivedAndLatestLogsInChronologicalOrder() throws IOException {
        Path logsDirectory = Files.createDirectories(tempDirectory.resolve("logs"));
        writeGzipLog(logsDirectory.resolve("2026-05-14-1.log.gz"), List.of(
                "[00:03:01] [Server thread/INFO]: archived startup",
                "java.lang.IllegalStateException: archived stack trace"
        ));
        writeGzipLog(logsDirectory.resolve("2026-06-04-1.log.gz"), List.of(
                "[17:38:22] [Server thread/WARN]: archived warning"
        ));
        Path latestLog = logsDirectory.resolve("latest.log");
        Files.writeString(latestLog, String.join(System.lineSeparator(), List.of(
                "[20:40:01] [Server thread/INFO]: latest startup",
                "[20:40:02] [Server thread/ERROR]: latest failure"
        )), StandardCharsets.UTF_8);
        Files.setLastModifiedTime(
                latestLog,
                FileTime.from(LocalDateTime.of(2026, 6, 9, 20, 40).atZone(ZoneId.systemDefault()).toInstant())
        );

        ServerConfigEntity config = new ServerConfigEntity();
        config.setRootDirectory(tempDirectory.toString());

        List<LogEntryDto> logs = serverLogService.readLogs(config);

        assertThat(logs).extracting(LogEntryDto::message).containsExactly(
                "archived startup",
                "java.lang.IllegalStateException: archived stack trace",
                "archived warning",
                "latest startup",
                "latest failure"
        );
        assertThat(logs).extracting(LogEntryDto::level).containsExactly("INFO", "INFO", "WARN", "INFO", "ERROR");
        assertThat(logs.get(1).timestamp()).isEqualTo(logs.get(0).timestamp());
        assertThat(logs.get(4).timestamp()).isAfter(logs.get(3).timestamp());
    }

    private void writeGzipLog(Path path, List<String> lines) throws IOException {
        try (OutputStream outputStream = new GZIPOutputStream(Files.newOutputStream(path))) {
            outputStream.write(String.join(System.lineSeparator(), lines).getBytes(StandardCharsets.UTF_8));
        }
    }
}
