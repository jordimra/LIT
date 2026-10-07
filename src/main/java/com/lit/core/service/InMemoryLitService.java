package com.lit.core.service;

import com.lit.api.*;
import com.lit.api.exceptions.*;
import com.lit.core.domain.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class InMemoryLitService implements LitService {

    private Repository repository;
    private String activeTaskName;
    private final List<String> actionHistory = new ArrayList<>(); // Log simple de acciones para "undo"

    @Override
    public void initialize(Path workspacePath) {
        if (repository != null) {
            throw new RepositoryAlreadyExistsException("Ya existe un repositorio LIT inicializado.");
        }
        repository = new Repository(workspacePath);
        
        // Crear primer snapshot vacío de base
        String initialSnapshotId = "init-0000";
        Snapshot initialSnapshot = new Snapshot(
            initialSnapshotId,
            null,
            "Initial empty snapshot",
            Instant.now(),
            "system",
            Map.of()
        );
        repository.getHistory().addSnapshot(initialSnapshot);
        
        // Crear tarea por defecto 'main' apuntando al snapshot inicial
        Task mainTask = new Task("main", initialSnapshotId);
        repository.getHistory().addTask(mainTask);
        activeTaskName = "main";

        actionHistory.add("init");
    }

    private void ensureRepository() {
        if (repository == null) {
            throw new RepositoryNotFoundException("No se encontró ningún repositorio LIT en esta ruta.");
        }
    }

    @Override
    public WorkspaceStatus getStatus() {
        ensureRepository();
        
        Task activeTask = repository.getHistory().getTask(activeTaskName)
            .orElseThrow(() -> new TaskNotFoundException("La tarea activa no existe."));
        
        Snapshot headSnapshot = repository.getHistory().getSnapshot(activeTask.getHeadSnapshotId())
            .orElseThrow(() -> new LitException("Snapshot HEAD no encontrado."));

        Map<Path, String> baseTree = headSnapshot.getFileTree();
        Map<Path, String> currentTree = repository.getWorkspace().getVirtualDisk();

        List<ChangedFile> modifiedFiles = new ArrayList<>();

        // Detectar nuevos y modificados
        for (Map.Entry<Path, String> entry : currentTree.entrySet()) {
            Path path = entry.getKey();
            String content = entry.getValue();
            if (!baseTree.containsKey(path)) {
                modifiedFiles.add(new ChangedFile(path, ChangeType.ADDED));
            } else if (!baseTree.get(path).equals(content)) {
                modifiedFiles.add(new ChangedFile(path, ChangeType.MODIFIED));
            }
        }

        // Detectar eliminados
        for (Path path : baseTree.keySet()) {
            if (!currentTree.containsKey(path)) {
                modifiedFiles.add(new ChangedFile(path, ChangeType.DELETED));
            }
        }

        // Mapear conflictos
        List<ConflictRecord> activeConflicts = new ArrayList<>();
        for (Conflict conflict : repository.getWorkspace().getConflicts().values()) {
            if (!conflict.isResolved()) {
                activeConflicts.add(new ConflictRecord(
                    conflict.getRelativePath(),
                    conflict.getLocalLines(),
                    conflict.getIncomingLines()
                ));
            }
        }

        boolean hasChanges = !modifiedFiles.isEmpty();
        return new WorkspaceStatus(activeTaskName, modifiedFiles, activeConflicts, hasChanges);
    }

    @Override
    public SnapshotRecord save(String message) {
        ensureRepository();

        // 1. Validar conflictos activos
        WorkspaceStatus status = getStatus();
        if (!status.activeConflicts().isEmpty()) {
            throw new ActiveConflictException("No se puede guardar: existen conflictos pendientes de resolución.");
        }

        // 2. Validar que haya cambios
        if (!status.hasChanges()) {
            throw new NothingToSaveException("No hay cambios pendientes para guardar.");
        }

        Task activeTask = repository.getHistory().getTask(activeTaskName)
            .orElseThrow(() -> new TaskNotFoundException("La tarea activa no existe."));

        // 3. Crear Snapshot
        String snapshotId = "snap-" + UUID.randomUUID().toString().substring(0, 8);
        Map<Path, String> currentWorkspaceCopy = Map.copyOf(repository.getWorkspace().getVirtualDisk());
        
        Snapshot newSnapshot = new Snapshot(
            snapshotId,
            activeTask.getHeadSnapshotId(),
            message,
            Instant.now(),
            "developer",
            currentWorkspaceCopy
        );

        repository.getHistory().addSnapshot(newSnapshot);
        activeTask.setHeadSnapshotId(snapshotId);

        // Limpiar conflictos resueltos si los hubiera
        repository.getWorkspace().clearConflicts();

        actionHistory.add("save:" + snapshotId);

        return new SnapshotRecord(
            newSnapshot.getId(),
            newSnapshot.getParentId(),
            newSnapshot.getMessage(),
            newSnapshot.getTimestamp(),
            newSnapshot.getAuthor()
        );
    }

    @Override
    public void switchTask(String taskName) {
        ensureRepository();

        WorkspaceStatus status = getStatus();
        if (!status.activeConflicts().isEmpty()) {
            throw new ActiveConflictException("No se puede cambiar de tarea: existen conflictos activos.");
        }

        Task currentTask = repository.getHistory().getTask(activeTaskName)
            .orElseThrow(() -> new TaskNotFoundException("La tarea activa no existe."));

        // 1. Guardar cambios pendientes locales de forma automática en la tarea de origen (Stashing)
        Map<Path, String> currentWorkspace = repository.getWorkspace().getVirtualDisk();
        Snapshot headSnapshot = repository.getHistory().getSnapshot(currentTask.getHeadSnapshotId())
            .orElseThrow(() -> new LitException("Snapshot HEAD no encontrado."));
        
        Map<Path, String> pendingChanges = new HashMap<>();
        for (Map.Entry<Path, String> entry : currentWorkspace.entrySet()) {
            Path path = entry.getKey();
            String content = entry.getValue();
            if (!headSnapshot.getFileTree().containsKey(path) || !headSnapshot.getFileTree().get(path).equals(content)) {
                pendingChanges.put(path, content);
            }
        }
        // También guardar cuáles fueron eliminados de la base
        for (Path path : headSnapshot.getFileTree().keySet()) {
            if (!currentWorkspace.containsKey(path)) {
                pendingChanges.put(path, null); // null representa eliminación temporal
            }
        }

        currentTask.putCachedChanges(pendingChanges);

        // 2. Obtener o crear tarea de destino
        Optional<Task> targetTaskOpt = repository.getHistory().getTask(taskName);
        Task targetTask;
        if (targetTaskOpt.isEmpty()) {
            // Crear tarea a partir del snapshot HEAD actual
            targetTask = new Task(taskName, currentTask.getHeadSnapshotId());
            repository.getHistory().addTask(targetTask);
        } else {
            targetTask = targetTaskOpt.get();
        }

        // 3. Restaurar espacio de trabajo con los archivos de la tarea de destino
        Snapshot targetHeadSnapshot = repository.getHistory().getSnapshot(targetTask.getHeadSnapshotId())
            .orElseThrow(() -> new LitException("Snapshot HEAD de destino no encontrado."));

        currentWorkspace.clear();
        currentWorkspace.putAll(targetHeadSnapshot.getFileTree());

        // 4. Aplicar cambios locales guardados de la tarea de destino si los hubiera
        for (Map.Entry<Path, String> entry : targetTask.getCachedChanges().entrySet()) {
            Path path = entry.getKey();
            String content = entry.getValue();
            if (content == null) {
                currentWorkspace.remove(path);
            } else {
                currentWorkspace.put(path, content);
            }
        }

        activeTaskName = taskName;
        actionHistory.add("switch:" + taskName);
    }

    @Override
    public List<String> listTasks() {
        ensureRepository();
        List<String> list = new ArrayList<>();
        for (Task task : repository.getHistory().getTasks()) {
            list.add(task.getName());
        }
        return list;
    }

    @Override
    public String getActiveTask() {
        ensureRepository();
        return activeTaskName;
    }

    @Override
    public void synchronize() {
        ensureRepository();
        // Simulación en memoria
        // Si hay algún archivo virtual específico llamado "conflict.txt", disparamos un conflicto de simulación
        Map<Path, String> workspaceFiles = repository.getWorkspace().getVirtualDisk();
        Path conflictPath = Path.of("conflict.txt");
        if (workspaceFiles.containsKey(conflictPath)) {
            Conflict mockConflict = new Conflict(
                conflictPath,
                List.of("Mi cambio local"),
                List.of("Cambio remoto del servidor")
            );
            repository.getWorkspace().addConflict(mockConflict);
            throw new ActiveConflictException("Se han detectado conflictos durante la sincronización remota.");
        }
        actionHistory.add("sync");
    }

    @Override
    public void undo() {
        ensureRepository();
        if (actionHistory.isEmpty()) {
            return;
        }

        String lastAction = actionHistory.remove(actionHistory.size() - 1);
        if (lastAction.startsWith("save:")) {
            String snapshotId = lastAction.substring(5);
            Task activeTask = repository.getHistory().getTask(activeTaskName)
                .orElseThrow(() -> new TaskNotFoundException("La tarea activa no existe."));

            Snapshot currentHead = repository.getHistory().getSnapshot(activeTask.getHeadSnapshotId())
                .orElseThrow(() -> new LitException("Snapshot no encontrado."));

            if (currentHead.getId().equals(snapshotId)) {
                // Volver al padre
                String parentId = currentHead.getParentId().orElse("init-0000");
                activeTask.setHeadSnapshotId(parentId);
                
                // Los archivos del snapshot borrado se quedan en el workspace como cambios pendientes
                // No tocamos el virtualDisk para que los archivos modificados permanezcan modificados.
            }
        } else if (lastAction.startsWith("switch:")) {
            // Sería revertir el cambio de tarea volviendo a la anterior
            // Por simplicidad en este mock básico, no hacemos nada o limpiamos el log
        }
    }

    // Helper de depuración para simular la interacción física en tests
    public Repository getRepository() {
        return repository;
    }
}
