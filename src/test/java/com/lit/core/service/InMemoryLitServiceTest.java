package com.lit.core.service;

import com.lit.api.*;
import com.lit.api.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLitServiceTest {

    private InMemoryLitService service;
    private final Path testPath = Path.of("d:/dummy/project");

    @BeforeEach
    void setUp() {
        service = new InMemoryLitService();
    }

    @Test
    void testInitializeSuccess() {
        service.initialize(testPath);
        
        assertEquals("main", service.getActiveTask());
        List<String> tasks = service.listTasks();
        assertEquals(1, tasks.size());
        assertTrue(tasks.contains("main"));
        
        WorkspaceStatus status = service.getStatus();
        assertFalse(status.hasChanges());
        assertTrue(status.activeConflicts().isEmpty());
    }

    @Test
    void testInitializeAlreadyExists() {
        service.initialize(testPath);
        assertThrows(RepositoryAlreadyExistsException.class, () -> service.initialize(testPath));
    }

    @Test
    void testGetStatusWithoutInitThrows() {
        assertThrows(RepositoryNotFoundException.class, () -> service.getStatus());
    }

    @Test
    void testSaveWithoutChangesThrows() {
        service.initialize(testPath);
        assertThrows(NothingToSaveException.class, () -> service.save("no changes"));
    }

    @Test
    void testSaveSuccess() {
        service.initialize(testPath);
        
        // Simular adición de archivos al Workspace
        service.getRepository().getWorkspace().addFile(Path.of("src/App.java"), "public class App {}");
        
        WorkspaceStatus statusBefore = service.getStatus();
        assertTrue(statusBefore.hasChanges());
        assertEquals(1, statusBefore.modifiedFiles().size());
        assertEquals(ChangeType.ADDED, statusBefore.modifiedFiles().get(0).type());
        
        SnapshotRecord record = service.save("Initial code");
        assertNotNull(record.id());
        assertEquals("Initial code", record.message());
        
        WorkspaceStatus statusAfter = service.getStatus();
        assertFalse(statusAfter.hasChanges());
    }

    @Test
    void testSwitchTaskWithStashing() {
        service.initialize(testPath);
        
        // 1. Modificar archivo en la tarea 'main'
        service.getRepository().getWorkspace().addFile(Path.of("src/App.java"), "version 1");
        service.save("App v1");
        
        // Hacer un cambio local sin guardar
        service.getRepository().getWorkspace().modifyFile(Path.of("src/App.java"), "version 1 modificada");
        
        // 2. Cambiar a otra tarea 'feature-login' (la crea automáticamente)
        service.switchTask("feature-login");
        
        assertEquals("feature-login", service.getActiveTask());
        
        // El Workspace de feature-login no tiene el cambio de main modificado, sino el de HEAD (v1)
        WorkspaceStatus statusFeature = service.getStatus();
        assertFalse(statusFeature.hasChanges());
        assertEquals("version 1", service.getRepository().getWorkspace().getVirtualDisk().get(Path.of("src/App.java")));

        // 3. Modificar archivo en 'feature-login'
        service.getRepository().getWorkspace().modifyFile(Path.of("src/App.java"), "version login");
        service.save("Login base");
        
        // 4. Volver a 'main'
        service.switchTask("main");
        
        // Debería recuperar automáticamente los cambios del stash temporal de 'main'
        assertEquals("main", service.getActiveTask());
        assertEquals("version 1 modificada", service.getRepository().getWorkspace().getVirtualDisk().get(Path.of("src/App.java")));
        assertTrue(service.getStatus().hasChanges());
    }

    @Test
    void testConflictBlocksActions() {
        service.initialize(testPath);
        
        // Simular un conflicto activo
        service.getRepository().getWorkspace().addFile(Path.of("conflict.txt"), "local file");
        
        // Disparar conflicto con synchronize (simulación en memoria)
        assertThrows(ActiveConflictException.class, () -> service.synchronize());
        
        // Verificar que hay un conflicto activo
        WorkspaceStatus status = service.getStatus();
        assertFalse(status.activeConflicts().isEmpty());
        
        // Tratar de guardar cambios debería lanzar excepción
        assertThrows(ActiveConflictException.class, () -> service.save("resolviendo"));
        
        // Tratar de cambiar de tarea debería lanzar excepción
        assertThrows(ActiveConflictException.class, () -> service.switchTask("other-task"));
        
        // Resolver el conflicto manualmente
        service.getRepository().getWorkspace().resolveConflict(Path.of("conflict.txt"));
        
        // Ya no debería haber conflictos
        WorkspaceStatus statusAfter = service.getStatus();
        assertTrue(statusAfter.activeConflicts().isEmpty());
    }

    @Test
    void testUndoSave() {
        service.initialize(testPath);
        
        service.getRepository().getWorkspace().addFile(Path.of("src/App.java"), "version 1");
        SnapshotRecord record = service.save("Save 1");
        
        String headSnapshotBefore = service.getRepository().getHistory().getTask("main").get().getHeadSnapshotId();
        assertEquals(record.id(), headSnapshotBefore);
        
        // Ejecutar deshacer
        service.undo();
        
        String headSnapshotAfter = service.getRepository().getHistory().getTask("main").get().getHeadSnapshotId();
        assertEquals("init-0000", headSnapshotAfter);
        
        // Los archivos modificados permanecen en el workspace como cambios pendientes
        WorkspaceStatus status = service.getStatus();
        assertTrue(status.hasChanges());
        assertEquals(1, status.modifiedFiles().size());
    }
}
