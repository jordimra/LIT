# Lenguaje y Vocabulario Oficial — LIT

Este documento define el vocabulario oficial de comandos, conceptos e interacciones de LIT, contrastándolo con el modelo mental y los comandos de Git tradicionales. El objetivo es ofrecer un lenguaje coherente, intuitivo y orientado a la semántica cotidiana de un desarrollador.

---

## 📖 Conceptos y Comandos Principales

En lugar de los términos históricos de Git, LIT adopta un vocabulario de sentido común:

| Comando LIT | Concepto Equivalente en Git | Justificación de LIT |
| :--- | :--- | :--- |
| **`lit init`** | `git init` | Mantiene el estándar de la industria para comenzar un proyecto. |
| **`lit status`** | `git status` | Indica el estado actual del espacio de trabajo. Se simplifica el formato de salida para que sea de lectura directa. |
| **`lit save [mensaje]`** | `git add -A && git commit -m` | Unifica la acción de persistir el progreso. El usuario "guarda" su trabajo en una captura inmutable sin preocuparse de preparar archivos manualmente. |
| **`lit task [nombre]`** | `git checkout -b` / `git checkout` | Reemplaza el concepto abstracto de "rama" (branch) por "tarea" (task). Un desarrollador no crea ramas por diversión; crea tareas para organizar su trabajo cotidiano. |
| **`lit sync`** | `git pull --rebase && git push` | Sincroniza bidireccionalmente el repositorio local con el remoto en un solo paso. Resuelve la duda común de si hay que hacer push o pull primero. |
| **`lit undo`** | `git reset --soft HEAD~1` / `git restore` | Proporciona una red de seguridad explícita. El usuario tiene un botón de "deshacer" para cualquier comando destructivo. |

---

## 🚫 Conceptos de Git que Desaparecen de la Interfaz Pública

Para reducir la sobrecarga cognitiva, LIT oculta o elimina por completo varios conceptos de Git:

### 1. El Staging Area (Index)
En Git, el Staging Area actúa como un búfer entre el disco y el commit. Esto obliga al uso de `git add` antes de cada `git commit`. 
- **Decisión**: Se elimina conceptualmente. En LIT, `lit save` guarda el estado actual del Workspace directamente (ver [ADR-0002](adr/ADR-0002.md)).

### 2. Fast-Forward, Merge y Rebase
El usuario medio de Git suele confundir el significado de un merge no destructivo con un rebase, lo que a menudo destruye o altera el historial de forma accidental.
- **Decisión**: LIT oculta estas estrategias. Por defecto, al sincronizar (`lit sync`), LIT realiza una integración limpia e inteligente de los cambios (similar a un rebase automático para mantener un historial lineal y legible). Si surgen colisiones, se presentan como un "Conflicto" directo a resolver.

### 3. Detached HEAD
Un estado de Git altamente propenso a errores que ocurre al navegar por commits pasados sin una rama activa.
- **Decisión**: LIT impide que el usuario entre en un estado huérfano. La navegación por snapshots pasados se realiza de forma segura y en modo de solo lectura, sin desenganchar el puntero de la tarea activa.

### 4. Stashing
Guardar cambios temporalmente para cambiar de rama rápido.
- **Decisión**: LIT realiza stashing de forma automática e invisible. Cuando cambias de tarea (`lit task nombre`), LIT resguarda tus cambios locales sin guardar y los restaura cuando regresas, eliminando la necesidad de gestionar una pila de stash manual.

---

## 🧭 Tabla Comparativa de Flujos de Trabajo

### Guardar el progreso local
* **En Git**:
  ```bash
  git add src/main.java
  git commit -m "Añadida validación de acceso"
  ```
* **En LIT**:
  ```bash
  lit save "Añadida validación de acceso"
  ```

### Cambiar a otra tarea para corregir un bug urgente
* **En Git**:
  ```bash
  git stash
  git checkout master
  git checkout -b hotfix-login
  # ... trabajar ...
  git commit -am "Corregido login"
  git checkout master
  # ... volver a la rama anterior ...
  git checkout mi-caracteristica
  git stash pop
  ```
* **En LIT**:
  ```bash
  lit task hotfix-login
  # ... trabajar ...
  lit save "Corregido login"
  lit task mi-caracteristica
  ```
  *(LIT realiza el stashing y restauración del contexto de manera implícita).*

---

## 💡 Validación Conceptual

Cualquier desarrollador familiarizado con editores de texto tradicionales (donde existe un botón de "Guardar" y otro de "Deshacer") o herramientas de sincronización en la nube (como Google Drive, donde los archivos se sincronizan solos en segundo plano) entenderá intuitivamente el modelo mental de LIT.
"Tengo mi espacio de trabajo, puedo crear tareas para aislar características, guardo capturas inmutables de mi progreso y sincronizo con mi equipo cuando estoy listo."
