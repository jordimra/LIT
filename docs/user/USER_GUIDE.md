# Guía del Usuario de LIT (Light Intelligent Tracking)

Bienvenido a **LIT**, una capa de abstracción moderna e inteligente sobre Git. Esta guía te enseñará todo lo necesario para comenzar a usar LIT en tus proyectos diarios.

---

## 💡 El Modelo Mental de LIT

A diferencia de Git, que requiere que comprendas conceptos como el *Index* (Staging Area), *HEAD*, *Commits*, *Branches*, *Stash* y *Rebase*, LIT simplifica todo en un modelo mental de **dos espacios**:

1. **Tu Workspace (Espacio de Trabajo)**: Los archivos locales con los que interactúas en tu editor.
2. **Tu Repositorio (Historial de Snapshots)**: El historial guardado de tu proyecto.

```
+--------------------+   lit save "mensaje"   +-------------------------+
|                    | -----------------------> |                         |
|   Tu Workspace     |                          |   Historial de Snapshots |
| (Archivos locales) | <----------------------- |      (Repositorio)      |
|                    |         lit undo         |                         |
+--------------------+                          +-------------------------+
```

LIT maneja de forma invisible la preparación de archivos, las ramas locales, el almacenamiento temporal de cambios (stashing) y la reconciliación remota (rebasing).

---

## 🚀 Instalación y Puesta en Marcha

LIT requiere tener **Java 17** o superior y **Git** instalados en tu sistema.

1. **Compilar el proyecto**:
   Ejecuta el siguiente comando en la raíz del proyecto para generar el empaquetado:
   ```bash
   mvn clean package
   ```
2. **Configuración de la CLI**:
   - En **Windows**, puedes usar el script `lit.bat` disponible en la raíz del proyecto para invocar la CLI desde cualquier parte si agregas el directorio del proyecto a tu variable de entorno `PATH`.
   - Para ejecutar la CLI directamente:
     ```bash
     java -jar target/lit-core-1.0-SNAPSHOT-jar-with-dependencies.jar [comando]
     ```

---

## 🧭 Referencia de Comandos

LIT ofrece un conjunto mínimo de comandos de alto nivel para gestionar todo tu flujo de desarrollo.

### 1. Iniciar un Repositorio: `lit init`
Crea un nuevo repositorio en el directorio actual.
* **Uso**: `lit init`
* **Qué hace por debajo**: Inicializa un repositorio de Git, configura un usuario local de respaldo si no lo hay, crea un snapshot vacío inicial y establece la tarea por defecto en `main`.

### 2. Verificar el Estado: `lit status`
Muestra qué cambios tienes en tu Workspace que aún no has guardado.
* **Uso**: `lit status`
* **Salida**: Te indicará con iconos de colores qué archivos han sido agregados (`✦`), modificados (`⚠`) o eliminados (`⚠`). También avisa si existen conflictos de sincronización pendientes de resolución.

### 3. Guardar el Progreso: `lit save`
Crea una captura de estado (Snapshot) permanente con todos tus cambios actuales.
* **Uso**: `lit save "[mensaje de guardado]"`
* **Qué hace por debajo**: Añade todos los archivos modificados/nuevos de forma automática al commit y realiza un guardado seguro en tu historial.
* **Excepciones**: Si no hay cambios en el Workspace, cancelará la operación indicándote que no hay nada que guardar.

### 5. Trabajar en una Tarea: `lit task`
Cambia el contexto de tu trabajo. Una **Tarea** equivale a una rama de trabajo.
* **Listar tareas**: `lit task` (muestra la tarea activa marcada con un asterisco `*`).
* **Crear o cambiar a una tarea**: `lit task [nombre-de-tarea]`.
* **Stashing Automático**: Si tienes cambios locales sin guardar en la tarea A y te cambias a la tarea B, LIT **guarda temporalmente tus cambios automáticamente** (mediante un stash transparente). Cuando regreses a la tarea A, tus cambios pendientes se restaurarán de forma idéntica a como los dejaste.

### 6. Sincronizar con el Equipo: `lit sync`
Envía tu progreso al servidor remoto y descarga los cambios de tus compañeros en una sola operación.
* **Uso**: `lit sync`
* **Qué hace por debajo**:
  1. Descarga del servidor (`git fetch origin`).
  2. Reconcilia tus cambios aplicando una estrategia de `rebase` limpia para mantener un historial lineal y ordenado.
  3. Sube tus cambios al remoto (`git push origin`).
* **Gestión de Conflictos**: Si tus cambios colisionan con los cambios remotos en la misma línea de código, `lit sync` pausará la operación, marcará los conflictos en los archivos afectados usando indicadores estándar de conflictos de texto y te guiará paso a paso para resolverlos y guardarlos de forma interactiva.

### 7. Deshacer el Último Paso: `lit undo`
Deshace de forma segura tu última acción. Su comportamiento depende del estado actual de tu Workspace:
* **Si tienes cambios locales sin guardar**: Revierte todos los archivos locales al estado del último Snapshot guardado (como un "descartar cambios locales" seguro).
* **Si tu Workspace está limpio**:
  - Si la última acción fue `save`, elimina el último Snapshot del historial y vuelve a colocar los archivos modificados como cambios pendientes en tu Workspace (no pierdes tu trabajo).
  - Si la última acción fue un cambio de tarea (`switch`), te devuelve a la tarea anterior restaurando cualquier cambio temporal.
* **Registro de Auditoría**: LIT mantiene un log interno (`.git/lit/audit_log.json`) de las últimas acciones para garantizar que `undo` funcione de manera predecible y segura.

---

## 🎨 Consola Inteligente y Salidas de Color
LIT utiliza códigos ANSI para dar formato y color a la terminal:
- **Verde (`✔`)**: Éxito. Todo ha ido bien.
- **Azul (`✦`)**: Información del sistema y archivos nuevos.
- **Amarillo (`⚠`)**: Advertencias importantes o archivos modificados.
- **Rojo (`✖`)**: Errores críticos que detienen la ejecución.
