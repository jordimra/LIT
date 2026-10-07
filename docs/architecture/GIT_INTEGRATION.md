# Integración Conceptual con Git — LIT

Este documento describe la especificación de diseño técnico sobre cómo el modelo conceptual de LIT interactúa y se mapea con la base de datos y comandos de Git.

---

## 🗺️ Mapeo de Operaciones de LIT a Git

El adaptador de Git (infraestructura) traduce de forma transparente las operaciones de la API pública de LIT a comandos de Git o llamadas de bajo nivel (JGit).

```
   ┌───────────┐                ┌───────────────┐
   │  API LIT  │  ────────────>  │  Git Backend  │
   └───────────┘                └───────────────┘
      init                        git init
      status                      git status --porcelain
      save                        git add -A && git commit
      switchTask                  git checkout / stash
      synchronize                 git fetch && git rebase && git push
      undo                        git reset / git restore
```

### 1. `lit init` (Inicialización)
* **Traducción**:
  1. Ejecuta `git init`.
  2. Crea el directorio de metadatos `.git/lit/`.
  3. Crea un archivo inicial de exclusiones si es necesario.
  4. Crea el commit inicial vacío para asegurar que el repositorio tenga un punto de partida válido.

### 2. `lit status` (Estado del Workspace)
* **Traducción**:
  1. Ejecuta `git status --porcelain=v1` para obtener una representación estructurada, ligera y rápida de los cambios.
  2. Filtra archivos ignorados.
  3. Mapea la nomenclatura de estados de archivos de Git (A, M, D, ??) a las constantes `ChangedFile` (ADDED, MODIFIED, DELETED) de LIT.
  4. Detecta colisiones de fusión en curso para rellenar la lista de `ConflictRecord`.

### 3. `lit save "[mensaje]"` (Guardar cambios)
* **Traducción**:
  1. Comprueba si hay conflictos activos. Si los hay, aborta y lanza `ActiveConflictException`.
  2. Ejecuta `git add -A` para preparar todos los cambios en el Staging Area (oculto para el usuario de LIT).
  3. Ejecuta `git commit -m "[mensaje]"` para generar un nuevo Snapshot persistido en la base de datos de Git.

### 4. `lit task [nombre]` (Gestión de Tareas)
* **Traducción**:
  1. Mapea conceptualmente la "Task" a una rama (branch) de Git.
  2. Si el usuario solicita cambiar a una tarea, LIT primero ejecuta `git status --porcelain` para ver si hay cambios locales sin guardar.
  3. **Stashing Automático**: Si hay cambios locales, LIT ejecuta `git stash save "lit-auto-stash:[nombre_tarea_actual]"` para resguardarlos automáticamente.
  4. Ejecuta `git checkout [nombre]`. Si la rama no existe, ejecuta `git checkout -b [nombre]`.
  5. Una vez cambiada la rama, si existe un stash asociado al nombre de la tarea de destino, LIT lo extrae inmediatamente con `git stash pop` para restaurar los cambios en curso.

### 5. `lit sync` (Sincronización Remota)
* **Traducción**:
  1. Para mantener el principio de historial limpio y lineal, LIT implementa la sincronización mediante **Rebase automático**.
  2. Ejecuta `git fetch origin`.
  3. Ejecuta `git rebase origin/[nombre_tarea_activa]`.
  4. Si hay colisiones en el rebase, se detiene, entra en "Estado de Conflicto" y expone los archivos a la resolución manual.
  5. Si el rebase se completa de forma limpia, ejecuta `git push origin [nombre_tarea_activa]` para subir los snapshots locales.

### 6. `lit undo` (Deshacer)
* **Traducción**:
  1. LIT consulta el registro local `.git/lit/audit_log.json` para determinar la última acción destructiva del usuario.
  2. **Deshacer Save**: Ejecuta `git reset --soft HEAD~1`. Esto retira el commit del historial pero conserva todos los cambios en los archivos en disco como modificaciones pendientes en el Workspace.
  3. **Deshacer Cambios del Workspace**: Ejecuta `git restore [ruta_archivo]` o `git checkout -- [ruta_archivo]` para devolver el archivo a su estado original guardado en el Snapshot.

---

## 🚫 Limitaciones Técnicas Impuestas por Git

* **Merges No Lineales**: Si un usuario edita el repositorio utilizando una herramienta Git nativa externa y realiza un `merge` clásico que genera commits con múltiples padres, la API de LIT simplificará la visualización para el desarrollador, mostrando un historial linealizado para evitar la confusión de las ramas entrelazadas.
* **Operaciones Forenses**: LIT no proporcionará comandos para reescribir la historia intermedia de forma arbitraria (como `rebase -i`). Para estas operaciones forenses o de corrección avanzada, se requerirá el uso de la CLI de Git nativa.

---

## 🤝 Compatibilidad con Herramientas Git Existentes

Dado que LIT no inventa ningún formato de almacenamiento y utiliza el directorio `.git` de manera estándar:
- Cualquier repositorio gestionado con LIT es **100% compatible** con herramientas tradicionales como la CLI de Git, VS Code, GitHub Desktop, Sourcetree, etc.
- Un commit de Git equivale exactamente a un `Snapshot` de LIT.
- Una rama (branch) de Git equivale exactamente a una `Task` de LIT.
- Las marcas de conflicto añadidas en los archivos durante la sincronización utilizan el estándar universal de Git (`<<<<<<<`, `=======`, `>>>>>>>`), por lo que cualquier editor moderno detectará y coloreará los conflictos de manera automática.
