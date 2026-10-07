package com.lit.core.service;

import com.lit.api.*;
import com.lit.api.exceptions.*;
import com.lit.core.adapter.git.GitAdapterLitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GitAdapterLitServiceTest {

    private GitAdapterLitService service;

    @BeforeEach
    void setUp() {
        service = new GitAdapterLitService();
    }

    @Test
    void testInitializeSuccess(@TempDir Path tempDir) {
        service.initialize(tempDir);
        
        assertEquals("main", service.getActiveTask());
        List<String> tasks = service.listTasks();
        assertEquals(1, tasks.size());
        assertTrue(tasks.contains("main"));
        
        WorkspaceStatus status = service.getStatus();
        assertFalse(status.hasChanges());
        assertTrue(status.activeConflicts().isEmpty());
    }

    @Test
    void testInitializeAlreadyExists(@TempDir Path tempDir) {
        service.initialize(tempDir);
        assertThrows(RepositoryAlreadyExistsException.class, () -> service.initialize(tempDir));
    }

    @Test
    void testGetStatusWithoutInitThrows() {
        assertThrows(RepositoryNotFoundException.class, () -> service.getStatus());
    }

    @Test
    void testSaveWithoutChangesThrows(@TempDir Path tempDir) {
        service.initialize(tempDir);
        assertThrows(NothingToSaveException.class, () -> service.save("no changes"));
    }

    @Test
    void testSaveSuccess(@TempDir Path tempDir) throws Exception {
        service.initialize(tempDir);
        
        Path file = tempDir.resolve("App.java");
        Files.writeString(file, "public class App {}");
        
        WorkspaceStatus statusBefore = service.getStatus();
        assertTrue(statusBefore.hasChanges());
        assertEquals(1, statusBefore.modifiedFiles().size());
        assertEquals(ChangeType.ADDED, statusBefore.modifiedFiles().get(0).type());
        assertEquals(Path.of("App.java"), statusBefore.modifiedFiles().get(0).relativePath());
        
        SnapshotRecord record = service.save("Initial code");
        assertNotNull(record.id());
        assertEquals("Initial code", record.message());
        
        WorkspaceStatus statusAfter = service.getStatus();
        assertFalse(statusAfter.hasChanges());
    }

    @Test
    void testSwitchTaskWithStashing(@TempDir Path tempDir) throws Exception {
        service.initialize(tempDir);
        
        Path file = tempDir.resolve("App.java");
        Files.writeString(file, "version 1");
        service.save("App v1");
        
        // Modificación local sin guardar
        Files.writeString(file, "version 1 modificada");
        
        // Cambiar a otra tarea
        service.switchTask("feature-login");
        assertEquals("feature-login", service.getActiveTask());
        
        // El workspace de feature-login debería estar limpio y el archivo restaurado a la versión de HEAD (version 1)
        WorkspaceStatus statusFeature = service.getStatus();
        assertFalse(statusFeature.hasChanges());
        assertEquals("version 1", Files.readString(file).trim());

        // Realizar cambios en feature-login
        Files.writeString(file, "version login");
        service.save("Login base");
        
        // Volver a 'main'
        service.switchTask("main");
        assertEquals("main", service.getActiveTask());
        
        // Debería recuperar los cambios stasheados automáticamente
        assertEquals("version 1 modificada", Files.readString(file).trim());
        assertTrue(service.getStatus().hasChanges());
    }

    @Test
    void testUndoSave(@TempDir Path tempDir) throws Exception {
        service.initialize(tempDir);
        
        Path file = tempDir.resolve("App.java");
        Files.writeString(file, "version 1");
        SnapshotRecord record = service.save("Save 1");
        
        // Ejecutar deshacer
        service.undo();
        
        // Los archivos modificados permanecen en el workspace
        assertEquals("version 1", Files.readString(file).trim());
        
        WorkspaceStatus status = service.getStatus();
        assertTrue(status.hasChanges());
        assertEquals(1, status.modifiedFiles().size());
        assertEquals(ChangeType.ADDED, status.modifiedFiles().get(0).type());
    }

    @Test
    void testConflictDuringSync(@TempDir Path tempDir) throws Exception {
        // 1. Inicializar el repositorio origen (bare)
        Path originDir = tempDir.resolve("origin.git");
        Files.createDirectories(originDir);
        runCommandInDir(originDir, "git", "init", "--bare");
        runCommandInDir(originDir, "git", "symbolic-ref", "HEAD", "refs/heads/main");
        
        // 2. Inicializar repositorio del servicio principal
        Path repo1 = tempDir.resolve("repo1");
        service.initialize(repo1);
        
        // Configurar remoto
        runCommandInDir(repo1, "git", "remote", "add", "origin", originDir.toAbsolutePath().toString());
        
        // Crear un archivo base y subirlo
        Path file1 = repo1.resolve("conflict.txt");
        Files.writeString(file1, "Linea Base\n");
        service.save("Commit Base");
        runCommandInDir(repo1, "git", "push", "-u", "origin", "main");
        
        // 3. Clonar en otro directorio temporal (repo2) para hacer cambios concurrentes
        Path repo2 = tempDir.resolve("repo2");
        runCommandInDir(tempDir, "git", "clone", originDir.toAbsolutePath().toString(), "repo2");
        
        // Configurar usuario local para el repo2
        runCommandInDir(repo2, "git", "config", "user.name", "LIT User 2");
        runCommandInDir(repo2, "git", "config", "user.email", "user2@lit.internal");
        
        // Modificar el archivo en repo2 y subirlo
        Path file2 = repo2.resolve("conflict.txt");
        Files.writeString(file2, "Linea Base\nCambio concurrentemente en Repo 2\n");
        runCommandInDir(repo2, "git", "add", "-A");
        runCommandInDir(repo2, "git", "commit", "-m", "Commit concurrent");
        runCommandInDir(repo2, "git", "push", "origin", "main");
        
        // 4. Modificar el mismo archivo en repo1 (nuestro servicio) de forma conflictiva
        Files.writeString(file1, "Linea Base\nCambio conflictivo en Repo 1\n");
        service.save("Commit local conflictivo");
        
        // 5. Intentar sincronizar (debería fallar con conflicto)
        assertThrows(ActiveConflictException.class, () -> service.synchronize());
        
        // Verificar que hay un conflicto activo
        WorkspaceStatus status = service.getStatus();
        assertFalse(status.activeConflicts().isEmpty());
        assertEquals(Path.of("conflict.txt"), status.activeConflicts().get(0).relativePath());
        
        // Verificar líneas del conflicto
        ConflictRecord conflict = status.activeConflicts().get(0);
        assertTrue(conflict.localLines().contains("Cambio conflictivo en Repo 1"));
    }

    @Test
    void testUndoWorkspaceChanges(@TempDir Path tempDir) throws Exception {
        service.initialize(tempDir);
        
        Path file = tempDir.resolve("App.java");
        Files.writeString(file, "original content");
        service.save("Save 1");
        
        // Modificar archivo y crear uno nuevo
        Files.writeString(file, "modified content");
        Path newFile = tempDir.resolve("New.java");
        Files.writeString(newFile, "new file content");
        
        assertTrue(service.getStatus().hasChanges());
        
        // Deshacer cambios del workspace (debe restaurar App.java y borrar New.java)
        service.undo();
        
        assertFalse(service.getStatus().hasChanges());
        assertEquals("original content", Files.readString(file).trim());
        assertFalse(Files.exists(newFile));
    }

    @Test
    void testUndoTaskSwitch(@TempDir Path tempDir) throws Exception {
        service.initialize(tempDir);
        
        // Cambiar a otra tarea
        service.switchTask("feature");
        assertEquals("feature", service.getActiveTask());
        
        // Deshacer el cambio de tarea
        service.undo();
        assertEquals("main", service.getActiveTask());
    }

    @Test
    void testStressPerformance(@TempDir Path tempDir) throws Exception {
        service.initialize(tempDir);
        Path file = tempDir.resolve("Stress.java");
        
        long start = System.currentTimeMillis();
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            Files.writeString(file, "Iteración " + i);
            service.save("Stress commit " + i);
        }
        
        long end = System.currentTimeMillis();
        long durationMs = end - start;
        
        // El test pasa si completa 100 iteraciones sin errores.
        // También podemos imprimir el tiempo.
        System.out.println("Stress test completed 100 saves in " + durationMs + " ms");
        WorkspaceStatus status = service.getStatus();
        assertFalse(status.hasChanges());
        assertEquals("Iteración 99", Files.readString(file).trim());
    }

    private void runCommandInDir(Path dir, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            throw new RuntimeException("Command failed with code " + code + ": " + String.join(" ", args) + "\nOutput:\n" + output);
        }
    }
}
