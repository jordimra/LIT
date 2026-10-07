package com.lit.core.adapter.git;

import com.lit.api.ChangeType;
import com.lit.api.ChangedFile;
import com.lit.api.ConflictRecord;
import com.lit.api.LitService;
import com.lit.api.SnapshotRecord;
import com.lit.api.WorkspaceStatus;
import com.lit.api.exceptions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GitAdapterLitService implements LitService {

    private Path workspacePath;

    @Override
    public void initialize(Path workspacePath) {
        if (Files.exists(workspacePath.resolve(".git"))) {
            throw new RepositoryAlreadyExistsException("A git repository already exists at " + workspacePath);
        }
        
        this.workspacePath = workspacePath;
        try {
            Files.createDirectories(workspacePath);
            executeGitCommand("init");
            
            // LIT specific metadata directory
            Files.createDirectories(workspacePath.resolve(".git").resolve("lit"));
            
            // Ensure local config is set if not configured globally
            ensureGitUserConfigured();
            
            // Initial empty commit
            executeGitCommand("commit", "--allow-empty", "-m", "Initial empty snapshot");
            
            try {
                executeGitCommand("branch", "-M", "main");
            } catch (Exception ignored) {}
            
            // Initialize audit log
            writeAuditLog(List.of("init"));
        } catch (RepositoryAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("Failed to initialize git repository: " + e.getMessage(), e);
        }
    }

    private void ensureRepository() {
        if (workspacePath == null) {
            workspacePath = Path.of(".");
        }
        if (!Files.exists(workspacePath.resolve(".git"))) {
            throw new RepositoryNotFoundException("No se encontró ningún repositorio LIT en esta ruta.");
        }
    }

    private void ensureGitUserConfigured() {
        try {
            String name = executeGitCommand("config", "user.name").trim();
            if (name.isEmpty()) {
                executeGitCommand("config", "user.name", "LIT User");
            }
        } catch (Exception e) {
            try {
                executeGitCommand("config", "user.name", "LIT User");
            } catch (Exception ignored) {}
        }
        try {
            String email = executeGitCommand("config", "user.email").trim();
            if (email.isEmpty()) {
                executeGitCommand("config", "user.email", "user@lit-vcs.internal");
            }
        } catch (Exception e) {
            try {
                executeGitCommand("config", "user.email", "user@lit-vcs.internal");
            } catch (Exception ignored) {}
        }
    }

    private String getActiveTaskName() {
        try {
            String branch = executeGitCommand("branch", "--show-current").trim();
            if (!branch.isEmpty()) {
                return branch;
            }
            branch = executeGitCommand("symbolic-ref", "--short", "-q", "HEAD").trim();
            if (!branch.isEmpty()) {
                return branch;
            }
        } catch (Exception ignored) {}
        return "HEAD";
    }

    @Override
    public WorkspaceStatus getStatus() {
        ensureRepository();
        
        String activeTask = getActiveTaskName();
        List<ChangedFile> modifiedFiles = new ArrayList<>();
        List<ConflictRecord> activeConflicts = new ArrayList<>();
        
        try {
            String statusOutput = executeGitCommand("status", "--porcelain");
            if (!statusOutput.isEmpty()) {
                String[] lines = statusOutput.split("\n");
                for (String line : lines) {
                    if (line.length() < 4) continue;
                    char x = line.charAt(0);
                    char y = line.charAt(1);
                    String pathStr = line.substring(3).trim();
                    
                    // Handle renames: R  old_path -> new_path
                    if (x == 'R') {
                        int arrowIdx = pathStr.indexOf(" -> ");
                        if (arrowIdx != -1) {
                            pathStr = pathStr.substring(arrowIdx + 4).trim();
                        }
                    }
                    
                    // Strip quotes if any
                    if (pathStr.startsWith("\"") && pathStr.endsWith("\"")) {
                        pathStr = pathStr.substring(1, pathStr.length() - 1);
                    }
                    
                    Path relativePath = Path.of(pathStr);
                    
                    boolean isConflict = (x == 'U' || y == 'U' || (x == 'A' && y == 'A') || (x == 'D' && y == 'D'));
                    if (isConflict) {
                        activeConflicts.add(parseConflictFile(relativePath));
                    } else {
                        ChangeType type = null;
                        if (x == '?' && y == '?') {
                            type = ChangeType.ADDED;
                        } else if (x == 'A' || y == 'A') {
                            type = ChangeType.ADDED;
                        } else if (x == 'D' || y == 'D') {
                            type = ChangeType.DELETED;
                        } else if (x == 'M' || y == 'M') {
                            type = ChangeType.MODIFIED;
                        }
                        
                        if (type != null) {
                            modifiedFiles.add(new ChangedFile(relativePath, type));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RepositoryException("Failed to get repository status", e);
        }
        
        boolean hasChanges = !modifiedFiles.isEmpty();
        return new WorkspaceStatus(activeTask, modifiedFiles, activeConflicts, hasChanges);
    }

    private ConflictRecord parseConflictFile(Path relativePath) {
        Path absolutePath = workspacePath.resolve(relativePath);
        List<String> localLines = new ArrayList<>();
        List<String> incomingLines = new ArrayList<>();
        if (Files.exists(absolutePath)) {
            try {
                List<String> lines = Files.readAllLines(absolutePath);
                boolean inLocal = false;
                boolean inIncoming = false;
                for (String line : lines) {
                    if (line.startsWith("<<<<<<<")) {
                        inLocal = true;
                        inIncoming = false;
                    } else if (line.startsWith("=======")) {
                        inLocal = false;
                        inIncoming = true;
                    } else if (line.startsWith(">>>>>>>")) {
                        inLocal = false;
                        inIncoming = false;
                    } else {
                        if (inLocal) {
                            localLines.add(line);
                        } else if (inIncoming) {
                            incomingLines.add(line);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        
        // Si hay un rebase en curso (frecuente tras synchronize), Git intercambia el rol de
        // HEAD y del commit entrante. Para mantener el modelo mental de LIT (local = mi cambio actual,
        // incoming = lo que viene de fuera), intercambiamos las líneas leídas.
        boolean rebaseInProgress = Files.exists(workspacePath.resolve(".git").resolve("rebase-merge"))
                || Files.exists(workspacePath.resolve(".git").resolve("rebase-apply"));
        if (rebaseInProgress) {
            List<String> temp = localLines;
            localLines = incomingLines;
            incomingLines = temp;
        }
        
        return new ConflictRecord(relativePath, localLines, incomingLines);
    }

    @Override
    public SnapshotRecord save(String message) {
        ensureRepository();
        
        WorkspaceStatus status = getStatus();
        if (!status.activeConflicts().isEmpty()) {
            throw new ActiveConflictException("No se puede guardar: existen conflictos pendientes de resolución.");
        }
        
        if (!status.hasChanges()) {
            throw new NothingToSaveException("No hay cambios pendientes para guardar.");
        }
        
        try {
            executeGitCommand("add", "-A");
            executeGitCommand("commit", "-m", message);
            
            // Get details of the commit we just created
            String commitInfo = executeGitCommand("log", "-1", "--format=%H|%P|%s|%cI|%an").trim();
            String[] parts = commitInfo.split("\\|", -1);
            String id = parts[0];
            String parentIdStr = parts[1];
            String msg = parts[2];
            String timestampStr = parts[3];
            String author = parts[4];
            
            java.util.Optional<String> parentId = java.util.Optional.empty();
            if (!parentIdStr.isEmpty()) {
                String[] parents = parentIdStr.split(" ");
                parentId = java.util.Optional.of(parents[0]);
            }
            
            java.time.Instant timestamp;
            try {
                timestamp = java.time.OffsetDateTime.parse(timestampStr).toInstant();
            } catch (Exception e) {
                timestamp = java.time.Instant.now();
            }
            
            // Update audit log
            appendToAuditLog("save:" + id);
            
            return new SnapshotRecord(id, parentId, msg, timestamp, author);
        } catch (Exception e) {
            throw new RepositoryException("Failed to save changes: " + e.getMessage(), e);
        }
    }

    @Override
    public void switchTask(String taskName) {
        switchTaskInternal(taskName, true);
    }

    private void switchTaskInternal(String taskName, boolean logAction) {
        ensureRepository();
        
        WorkspaceStatus status = getStatus();
        if (!status.activeConflicts().isEmpty()) {
            throw new ActiveConflictException("No se puede cambiar de tarea: existen conflictos activos.");
        }
        
        String currentTask = getActiveTaskName();
        if (currentTask.equals(taskName)) {
            return;
        }
        
        try {
            // 1. If there are changes, stash them
            if (status.hasChanges()) {
                executeGitCommand("add", "-A");
                executeGitCommand("stash", "push", "--include-untracked", "-m", "lit-auto-stash:" + currentTask);
            }
            
            // 2. Checkout or create the target task (branch)
            boolean taskExists = listTasks().contains(taskName);
            if (!taskExists) {
                executeGitCommand("checkout", "-b", taskName);
            } else {
                executeGitCommand("checkout", taskName);
            }
            
            // 3. Apply target task's stash if exists
            String targetStashRef = findStashForTask(taskName);
            if (targetStashRef != null) {
                executeGitCommand("stash", "pop", targetStashRef);
            }
            
            // Update audit log
            if (logAction) {
                appendToAuditLog("switch:" + currentTask + "->" + taskName);
            }
        } catch (Exception e) {
            throw new RepositoryException("Failed to switch task to " + taskName + ": " + e.getMessage(), e);
        }
    }

    private String findStashForTask(String taskName) throws Exception {
        String stashList = executeGitCommand("stash", "list");
        if (stashList.isEmpty()) {
            return null;
        }
        String[] lines = stashList.split("\n");
        String searchStr = "lit-auto-stash:" + taskName;
        for (String line : lines) {
            if (line.contains(searchStr)) {
                int colonIdx = line.indexOf(':');
                if (colonIdx != -1) {
                    return line.substring(0, colonIdx).trim();
                }
            }
        }
        return null;
    }

    @Override
    public List<String> listTasks() {
        ensureRepository();
        try {
            String branches = executeGitCommand("branch", "--format=%(refname:short)");
            if (branches.isEmpty()) {
                return List.of();
            }
            return List.of(branches.split("\n"));
        } catch (Exception e) {
            throw new RepositoryException("Failed to list tasks: " + e.getMessage(), e);
        }
    }

    @Override
    public String getActiveTask() {
        ensureRepository();
        return getActiveTaskName();
    }

    @Override
    public void synchronize() {
        ensureRepository();
        
        String activeTask = getActiveTaskName();
        
        boolean hasOrigin = false;
        try {
            String remotes = executeGitCommand("remote").trim();
            hasOrigin = java.util.Arrays.asList(remotes.split("\n")).contains("origin");
        } catch (Exception ignored) {}
        
        if (!hasOrigin) {
            throw new RepositoryException("No se ha configurado el remoto 'origin'.");
        }
        
        try {
            executeGitCommand("fetch", "origin");
        } catch (Exception e) {
            throw new RepositoryException("Servidor remoto no disponible o problema de conexión. Inténtalo más tarde.");
        }
        
        try {
            executeGitCommand("rebase", "origin/" + activeTask);
        } catch (Exception e) {
            WorkspaceStatus status = getStatus();
            if (!status.activeConflicts().isEmpty()) {
                throw new ActiveConflictException("Se han detectado conflictos durante la sincronización remota. Por favor, resuélvalos manualmente.");
            } else {
                throw new RepositoryException("La sincronización falló durante la integración: " + e.getMessage(), e);
            }
        }
        
        try {
            executeGitCommand("push", "origin", activeTask);
            
            appendToAuditLog("sync");
        } catch (Exception e) {
            throw new RepositoryException("La sincronización se preparó pero falló al enviarse al servidor remoto (problema de red). Inténtalo más tarde.");
        }
    }

    @Override
    public void undo() {
        ensureRepository();
        
        WorkspaceStatus status = getStatus();
        if (status.hasChanges()) {
            // Case 1: Workspace has changes. Restore to HEAD snapshot.
            try {
                // Restore modified and deleted files
                executeGitCommand("restore", ".");
                // Clean new untracked files
                executeGitCommand("clean", "-df");
            } catch (Exception e) {
                throw new RepositoryException("Failed to restore workspace changes: " + e.getMessage(), e);
            }
            return;
        }
        
        List<String> log = readAuditLog();
        if (log.isEmpty()) {
            return;
        }
        
        String lastAction = log.remove(log.size() - 1);
        if (lastAction.startsWith("save:")) {
            // Case 2: Undo save
            try {
                executeGitCommand("reset", "--soft", "HEAD~1");
                writeAuditLog(log);
            } catch (Exception e) {
                throw new RepositoryException("Failed to undo last save operation: " + e.getMessage(), e);
            }
        } else if (lastAction.startsWith("switch:")) {
            // Case 3: Undo switch (switch:fromTask->toTask)
            try {
                String transition = lastAction.substring(7);
                String[] parts = transition.split("->");
                if (parts.length == 2) {
                    String sourceTask = parts[0];
                    switchTaskInternal(sourceTask, false);
                }
                writeAuditLog(log);
            } catch (Exception e) {
                throw new RepositoryException("Failed to undo last task switch: " + e.getMessage(), e);
            }
        } else {
            try {
                writeAuditLog(log);
            } catch (Exception e) {
                throw new RepositoryException("Failed to update audit log", e);
            }
        }
    }

    private List<String> readAuditLog() {
        Path logPath = workspacePath.resolve(".git").resolve("lit").resolve("audit_log.json");
        if (!Files.exists(logPath)) {
            return new ArrayList<>();
        }
        try {
            String content = Files.readString(logPath).trim();
            if (content.isEmpty()) {
                return new ArrayList<>();
            }
            if (content.startsWith("[")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(content, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            } else {
                return new ArrayList<>(Files.readAllLines(logPath));
            }
        } catch (Exception ignored) {}
        return new ArrayList<>();
    }

    private void writeAuditLog(List<String> log) {
        Path logPath = workspacePath.resolve(".git").resolve("lit").resolve("audit_log.json");
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(log);
            Files.writeString(logPath, json, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
    }

    private void appendToAuditLog(String entry) {
        List<String> log = readAuditLog();
        log.add(entry);
        writeAuditLog(log);
    }

    private String executeGitCommand(String... args) throws Exception {
        if (workspacePath == null) {
            throw new IllegalStateException("Workspace path is not initialized");
        }
        
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String arg : args) {
            command.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workspacePath.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Git command failed with exit code " + exitCode + ":\n" + output);
        }
        return output;
    }
}
