package minecraft.milkwind.manager.server.runtime;

import java.io.BufferedWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class ServerRuntimeState {

    private final String serverId;
    private volatile String status;
    private volatile Process process;
    private volatile BufferedWriter consoleWriter;
    private volatile long processGeneration;
    private volatile Instant lastListRefreshAt;
    private volatile boolean restartRecommended;
    private final AtomicLong generationCounter = new AtomicLong();
    private final Deque<RuntimeLogLine> recentChatLines = new ConcurrentLinkedDeque<>();
    private final Set<String> onlinePlayers = new LinkedHashSet<>();

    public ServerRuntimeState(String serverId, String status) {
        this.serverId = serverId;
        this.status = status;
    }

    public String getServerId() {
        return serverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Process getProcess() {
        return process;
    }

    public BufferedWriter getConsoleWriter() {
        return consoleWriter;
    }

    public long nextProcessGeneration() {
        return generationCounter.incrementAndGet();
    }

    public long getProcessGeneration() {
        return processGeneration;
    }

    public synchronized void attachProcess(Process process, BufferedWriter consoleWriter, String status, long generation) {
        this.process = process;
        this.consoleWriter = consoleWriter;
        this.status = status;
        this.processGeneration = generation;
    }

    public synchronized void clearProcess(String status) {
        this.process = null;
        this.consoleWriter = null;
        this.status = status;
    }

    public Instant getLastListRefreshAt() {
        return lastListRefreshAt;
    }

    public void markListRefreshNow() {
        this.lastListRefreshAt = Instant.now();
    }

    public boolean isRestartRecommended() {
        return restartRecommended;
    }

    public void setRestartRecommended(boolean restartRecommended) {
        this.restartRecommended = restartRecommended;
    }

    public Deque<RuntimeLogLine> getRecentChatLines() {
        return recentChatLines;
    }

    public synchronized List<String> getOnlinePlayers() {
        return new ArrayList<>(onlinePlayers);
    }

    public synchronized void replaceOnlinePlayers(List<String> players) {
        onlinePlayers.clear();
        onlinePlayers.addAll(players);
    }

    public synchronized void addOnlinePlayer(String playerName) {
        onlinePlayers.add(playerName);
    }

    public synchronized void removeOnlinePlayer(String playerName) {
        onlinePlayers.remove(playerName);
    }

    public synchronized void clearTransientState() {
        onlinePlayers.clear();
        lastListRefreshAt = null;
        restartRecommended = false;
    }

    public void appendChatLine(String level, String source, String line) {
        RuntimeLogLine runtimeLine = new RuntimeLogLine(Instant.now(), level, source, line);
        recentChatLines.addFirst(runtimeLine);
        while (recentChatLines.size() > 80) {
            recentChatLines.removeLast();
        }
    }

    public record RuntimeLogLine(
            Instant timestamp,
            String level,
            String source,
            String message
    ) {
    }
}
