# Especificación de la API Pública de Java — LIT

Este documento detalla el diseño formal de la interfaz de programación de aplicaciones (API) pública de LIT. Siguiendo las directrices del proyecto, esta API está diseñada en **Java**, es agnóstica a la infraestructura de almacenamiento (Git) e implementa políticas estrictas de inmutabilidad.

---

## 🏗️ Jerarquía de Excepciones

LIT utiliza excepciones de tiempo de ejecución (`RuntimeException`) para evitar el acoplamiento y simplificar el flujo de control del cliente. Todas heredan de `LitException`.

```
LitException (abstract)
├── RepositoryException
│   ├── RepositoryNotFoundException
│   └── RepositoryAlreadyExistsException
├── WorkspaceException
│   ├── ActiveConflictException
│   └── NothingToSaveException
├── TaskException
│   ├── TaskNotFoundException
│   └── TaskAlreadyExistsException
└── SQSException (Sincronización y Red)
```

### Detalle de Excepciones
* **`RepositoryNotFoundException`**: Lanzada si se intenta ejecutar una operación en un directorio que no contiene un repositorio LIT.
* **`ActiveConflictException`**: Lanzada al intentar cambiar de tarea o sincronizar mientras existen conflictos de fusión pendientes en el Workspace.
* **`NothingToSaveException`**: Lanzada cuando el usuario solicita guardar un Snapshot pero no se ha detectado ninguna modificación.

---

## 📦 Clases de Datos Inmutables (DTOs)

Para asegurar la robustez, todos los objetos devueltos por la API que representan información histórica o de estado son inmutables (se recomienda el uso de **Java Records**).

### 1. `SnapshotRecord`
Representa una captura de estado histórica.
```java
public record SnapshotRecord(
    String id,
    Optional<String> parentId,
    String message,
    Instant timestamp,
    String author
) {}
```

### 2. `ChangedFile`
Representa un archivo modificado en el Workspace.
```java
public record ChangedFile(
    Path relativePath,
    ChangeType type // Enum: ADDED, MODIFIED, DELETED
) {}
```

### 3. `ConflictRecord`
Representa un archivo colisionado pendiente de resolución.
```java
public record ConflictRecord(
    Path relativePath,
    List<String> localLines,
    List<String> incomingLines
) {}
```

### 4. `WorkspaceStatus`
Describe el estado global actual del Workspace.
```java
public record WorkspaceStatus(
    String activeTaskName,
    List<ChangedFile> modifiedFiles,
    List<ConflictRecord> activeConflicts,
    boolean hasChanges
) {}
```

---

## 🔌 Fachada Principal: `LitService`

La interfaz `LitService` es el punto de entrada exclusivo para todas las operaciones de control de versiones de LIT.

```java
package com.lit.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface LitService {

    /**
     * Inicializa un nuevo repositorio en la ruta dada.
     * Si ya existe un repositorio Git, se acopla a él.
     * 
     * @param workspacePath Ruta raíz del proyecto.
     * @throws RepositoryAlreadyExistsException si ya existe un repositorio LIT.
     */
    void initialize(Path workspacePath);

    /**
     * Retorna el estado actual del espacio de trabajo y sus conflictos.
     * 
     * @return estado actual del Workspace.
     * @throws RepositoryNotFoundException si la ruta no es un repositorio LIT.
     */
    WorkspaceStatus getStatus();

    /**
     * Guarda el estado actual del Workspace en un Snapshot.
     * 
     * @param message Descripción amigable de los cambios.
     * @return El registro del Snapshot creado.
     * @throws NothingToSaveException si el Workspace está limpio.
     * @throws ActiveConflictException si hay conflictos sin resolver.
     */
    SnapshotRecord save(String message);

    /**
     * Cambia de tarea activa. Si la tarea no existe, se crea a partir
     * del Snapshot actual. Los cambios no guardados se almacenan automáticamente.
     * 
     * @param taskName Nombre de la tarea a activar.
     * @throws ActiveConflictException si hay conflictos activos.
     */
    void switchTask(String taskName);

    /**
     * Lista todas las tareas configuradas en el repositorio.
     */
    List<String> listTasks();

    /**
     * Retorna el nombre de la tarea actualmente activa.
     */
    String getActiveTask();

    /**
     * Sincroniza bidireccionalmente la tarea activa con el servidor remoto.
     * 
     * @throws ActiveConflictException si se detectan colisiones durante la sincronización.
     */
    void synchronize();

    /**
     * Deshace la última acción del desarrollador (p. ej., revierte un guardado
     * o restaura archivos modificados).
     */
    void undo();
}
```

---

## ⚖️ Políticas de Diseño y Extensibilidad

### 1. Inmutabilidad por Defecto
Ningún método de `LitService` ni de las clases de datos asociadas debe permitir la modificación destructiva de su estado directamente. Todas las mutaciones en el espacio de trabajo físico se realizan a través de métodos de comando de `LitService` y son atómicas.

### 2. Manejo de Nulos
La API prohíbe el uso de retornos o parámetros nulos. Se debe emplear `java.util.Optional` para representar cualquier valor opcional (como la ausencia de un snapshot padre).

### 3. Extensibilidad Futura
Para añadir funcionalidades en el futuro sin romper compatibilidad con clientes existentes (como extensiones de IDEs), la inicialización del servicio se realizará mediante un patrón Factory que aislará la implementación interna:
```java
public final class LitServiceFactory {
    public static LitService createService() {
        // Retorna la implementación por defecto inyectada
        return ServiceLoader.load(LitService.class)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No se encontró implementación de LitService"));
    }
}
```
