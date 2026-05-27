package minecraft.milkwind.manager.server.runtime;

import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import minecraft.milkwind.manager.server.service.ServerStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ServerProcessService {

    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(20);
    private static final Pattern STRUCTURED_LOG_PATTERN = Pattern.compile("^\\[[^\\]]+\\]\\s*\\[[^\\]/]+/([A-Z]+)]\\s*(?:(?:\\[[^\\]]+])\\s*)?(.*)$");
    private static final Pattern CHAT_PATTERN = Pattern.compile("<([^>]+)>\\s+(.*)");
    private static final Pattern JOIN_PATTERN = Pattern.compile("([A-Za-z0-9_]+)\\s+joined the game", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEFT_PATTERN = Pattern.compile("([A-Za-z0-9_]+)\\s+left the game", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_LIST_HEADER_PATTERN = Pattern.compile("There are\\s+\\d+\\s+of a max(?:imum)?\\s+of\\s+\\d+\\s+players? online:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_PLAYER_LIST_PATTERN = Pattern.compile("players online:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_LIST_PREFIX_PATTERN = Pattern.compile("^(?:[:\\-]|online:?|players?:?)\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern BRACKET_DECORATION_PATTERN = Pattern.compile("^[\\[(<].*[>\\])]$");

    private final ServerRuntimeRegistry runtimeRegistry;
    private final ServerStatusService serverStatusService;
    private final ExecutorService runtimeIoExecutor = Executors.newCachedThreadPool();

    public ServerProcessService(ServerRuntimeRegistry runtimeRegistry, ServerStatusService serverStatusService) {
        this.runtimeRegistry = runtimeRegistry;
        this.serverStatusService = serverStatusService;
    }

    public void start(ServerConfigEntity config) {
        ServerRuntimeState runtime = runtimeRegistry.getOrCreate(config.getServerId(), "STOPPED");
        Process process = runtime.getProcess();
        if (process != null && process.isAlive()) {
            updateRuntimeStatus(runtime, "ONLINE");
            return;
        }

        Path root = Path.of(config.getRootDirectory());
        Path jar = discoverJar(root)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "jar_not_found", "Unable to find a server jar in the root directory"));

        ProcessBuilder builder = new ProcessBuilder()
                .directory(root.toFile())
                .command(buildCommand(config, jar));
        builder.redirectErrorStream(true);

        try {
            Process startedProcess = builder.start();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(startedProcess.getOutputStream(), StandardCharsets.UTF_8));
            long generation = runtime.nextProcessGeneration();

            runtime.clearTransientState();
            runtime.attachProcess(startedProcess, writer, "STARTING", generation);
            serverStatusService.updateStatus(config.getServerId(), "STARTING");
            attachLogPump(runtime, startedProcess, generation);
        } catch (IOException exception) {
            runtime.clearProcess("STOPPED");
            runtime.clearTransientState();
            serverStatusService.updateStatus(config.getServerId(), "STOPPED");
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "process_start_failed", "Failed to start server process");
        }
    }

    public void stop(ServerConfigEntity config) {
        ServerRuntimeState runtime = runtimeRegistry.getOrCreate(config.getServerId(), "STOPPED");
        Process process = runtime.getProcess();
        if (process == null || !process.isAlive()) {
            clearStoppedRuntime(runtime);
            serverStatusService.updateStatus(config.getServerId(), "STOPPED");
            return;
        }

        updateRuntimeStatus(runtime, "STOPPING");
        try {
            sendCommand(runtime, "stop");
        } catch (ApiException ignored) {
            // If stdin is already gone, still continue shutting the process down.
        }

        process.destroy();
        waitForTermination(process);
        if (process.isAlive()) {
            process.destroyForcibly();
            waitForTermination(process);
        }

        closeWriter(runtime.getConsoleWriter());
        clearStoppedRuntime(runtime);
        serverStatusService.updateStatus(config.getServerId(), "STOPPED");
    }

    public void restart(ServerConfigEntity config) {
        stop(config);
        start(config);
    }

    public void refreshPlayerList(ServerConfigEntity config) {
        ServerRuntimeState runtime = snapshotRuntime(config);
        Process process = runtime.getProcess();
        if (process == null || !process.isAlive()) {
            return;
        }

        sendCommand(runtime, "list");
        runtime.markListRefreshNow();
    }

    public void sendCommand(ServerRuntimeState runtime, String command) {
        BufferedWriter writer = runtime.getConsoleWriter();
        Process process = runtime.getProcess();
        if (writer == null || process == null || !process.isAlive()) {
            throw new ApiException(HttpStatus.CONFLICT, "process_not_running", "Server process is not running");
        }

        try {
            writer.write(command);
            writer.newLine();
            writer.flush();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "command_write_failed", "Failed to write to server console");
        }
    }

    public ServerRuntimeState snapshotRuntime(ServerConfigEntity config) {
        ServerRuntimeState runtime = runtimeRegistry.getOrCreate(config.getServerId(), config.getStatus());
        Process process = runtime.getProcess();
        if (process != null && !process.isAlive()) {
            clearStoppedRuntime(runtime);
            serverStatusService.updateStatus(config.getServerId(), "STOPPED");
            return runtime;
        }

        if (process == null && !"STOPPED".equals(runtime.getStatus())) {
            clearStoppedRuntime(runtime);
            serverStatusService.updateStatus(config.getServerId(), "STOPPED");
            return runtime;
        }

        return runtime;
    }

    private void attachLogPump(ServerRuntimeState runtime, Process process, long generation) {
        runtimeIoExecutor.submit(() -> {
            try (InputStreamReader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder line = new StringBuilder();
                int ch;
                while ((ch = reader.read()) != -1) {
                    if (ch == '\n') {
                        handleRuntimeLine(runtime, line.toString().trim());
                        line.setLength(0);
                    } else if (ch != '\r') {
                        line.append((char) ch);
                    }
                }

                if (line.length() > 0) {
                    handleRuntimeLine(runtime, line.toString().trim());
                }
            } catch (IOException ignored) {
                // Process exit or stream closure is handled in finally.
            } finally {
                if (runtime.getProcessGeneration() == generation) {
                    closeWriter(runtime.getConsoleWriter());
                    clearStoppedRuntime(runtime);
                    serverStatusService.updateStatus(runtime.getServerId(), "STOPPED");
                }
            }
        });
    }

    private void handleRuntimeLine(ServerRuntimeState runtime, String line) {
        if (line.isBlank()) {
            return;
        }

        ParsedLogLine parsed = parseLogLine(line);
        runtime.appendLine(parsed.level(), parsed.source(), parsed.message());
        updateRuntimeStatusFromLine(runtime, parsed.message());
        parsePlayerState(runtime, parsed.message());
        parseChat(runtime, parsed);
    }

    private void updateRuntimeStatusFromLine(ServerRuntimeState runtime, String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("done (") || normalized.contains("for help, type \"help\"")) {
            runtime.setStatus("ONLINE");
        } else if (normalized.contains("stopping server") || normalized.contains("server stopped")) {
            runtime.setStatus("STOPPING");
        }
    }

    private void parsePlayerState(ServerRuntimeState runtime, String message) {
        Matcher joinMatcher = JOIN_PATTERN.matcher(message);
        if (joinMatcher.find()) {
            runtime.addOnlinePlayer(joinMatcher.group(1));
            return;
        }

        Matcher leftMatcher = LEFT_PATTERN.matcher(message);
        if (leftMatcher.find()) {
            runtime.removeOnlinePlayer(leftMatcher.group(1));
            return;
        }

        String playersSegment = extractPlayerListSegment(message);
        if (playersSegment == null) {
            return;
        }

        List<String> players = parsePlayerNames(playersSegment);
        runtime.replaceOnlinePlayers(players);
    }

    private void parseChat(ServerRuntimeState runtime, ParsedLogLine parsed) {
        Matcher chatMatcher = CHAT_PATTERN.matcher(parsed.message());
        if (!chatMatcher.find()) {
            return;
        }

        runtime.appendChatLine(parsed.level(), "minecraft-chat", "<" + chatMatcher.group(1) + "> " + chatMatcher.group(2));
    }

    private String extractPlayerListSegment(String message) {
        Matcher playerListHeaderMatcher = PLAYER_LIST_HEADER_PATTERN.matcher(message);
        if (playerListHeaderMatcher.find()) {
            return playerListHeaderMatcher.group(1).trim();
        }

        Matcher legacyMatcher = LEGACY_PLAYER_LIST_PATTERN.matcher(message);
        if (legacyMatcher.find()) {
            return legacyMatcher.group(1).trim();
        }

        return null;
    }

    private List<String> parsePlayerNames(String playersSegment) {
        String normalizedSegment = PLAYER_LIST_PREFIX_PATTERN.matcher(playersSegment.trim()).replaceFirst("").trim();
        if (normalizedSegment.isBlank() || normalizedSegment.equalsIgnoreCase("none")) {
            return List.of();
        }

        String[] rawPlayers = normalizedSegment.split(",");
        List<String> players = new ArrayList<>();
        for (String rawPlayer : rawPlayers) {
            String normalizedPlayer = normalizePlayerName(rawPlayer);
            if (!normalizedPlayer.isBlank()) {
                players.add(normalizedPlayer);
            }
        }
        return players;
    }

    private String normalizePlayerName(String rawPlayer) {
        String normalized = rawPlayer.trim();
        if (normalized.isBlank()) {
            return "";
        }

        normalized = normalized.replaceFirst("^\\d+\\.\\s*", "");
        normalized = normalized.replaceAll("^[\\-:*]+\\s*", "");
        normalized = normalized.replaceAll("\\s*\\([^)]*\\)$", "");
        normalized = normalized.replaceAll("\\s*\\[[^\\]]*]$", "");
        normalized = normalized.replaceAll("\\s+", " ").trim();

        if (BRACKET_DECORATION_PATTERN.matcher(normalized).matches()) {
            return "";
        }

        if (!normalized.matches("[A-Za-z0-9_]{1,16}")) {
            return "";
        }
        return normalized;
    }

    private ParsedLogLine parseLogLine(String line) {
        Matcher structuredMatcher = STRUCTURED_LOG_PATTERN.matcher(line);
        if (structuredMatcher.matches()) {
            String level = normalizeLevel(structuredMatcher.group(1));
            String message = structuredMatcher.group(2).trim();
            return new ParsedLogLine(level, "minecraft", message.isBlank() ? line : message);
        }

        return new ParsedLogLine("INFO", "minecraft", line);
    }

    private String normalizeLevel(String level) {
        return switch (level) {
            case "WARN", "WARNING" -> "WARN";
            case "ERROR", "SEVERE" -> "ERROR";
            case "DEBUG", "TRACE" -> "DEBUG";
            default -> "INFO";
        };
    }

    private void updateRuntimeStatus(ServerRuntimeState runtime, String status) {
        runtime.setStatus(status);
        serverStatusService.updateStatus(runtime.getServerId(), status);
    }

    private void clearStoppedRuntime(ServerRuntimeState runtime) {
        runtime.clearProcess("STOPPED");
        runtime.clearTransientState();
    }

    private void waitForTermination(Process process) {
        try {
            process.waitFor(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void closeWriter(BufferedWriter writer) {
        if (writer == null) {
            return;
        }

        try {
            writer.close();
        } catch (IOException ignored) {
            // Nothing else to do during shutdown.
        }
    }

    private Optional<Path> discoverJar(Path root) {
        if (!Files.exists(root)) {
            return Optional.empty();
        }

        try (var stream = Files.list(root)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst();
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private String[] buildCommand(ServerConfigEntity config, Path jar) {
        String jvmArguments = config.getJvmArguments() == null ? "" : config.getJvmArguments().trim();
        if (jvmArguments.isBlank()) {
            return new String[] {"java", "-jar", jar.toAbsolutePath().toString()};
        }

        String[] tokens = jvmArguments.split("\\s+");
        String[] command = new String[tokens.length + 3];
        command[0] = "java";
        System.arraycopy(tokens, 0, command, 1, tokens.length);
        command[command.length - 2] = "-jar";
        command[command.length - 1] = jar.toAbsolutePath().toString();
        return command;
    }

    private record ParsedLogLine(
            String level,
            String source,
            String message
    ) {
    }
}
