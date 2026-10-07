# Especificación de la CLI — LIT

Este documento detalla la sintaxis, experiencia de usuario (UX), formato de salidas y flujos de control de la Interfaz de Línea de Comandos (CLI) de LIT.

---

## 🎨 Guía de Estilo de Salida en Consola

LIT utiliza un sistema semántico de colores e iconos Unicode para proporcionar una retroalimentación visual inmediata y amigable al desarrollador.

### Paleta de Colores Semánticos
* **Verde (Éxito)**: Operaciones completadas correctamente. Icono: `✔`
* **Azul (Información / Progreso)**: Detalles del sistema o estado informativo. Icono: `✦`
* **Amarillo (Advertencia / Atención)**: Situaciones de cuidado que no bloquean el flujo. Icono: `⚠`
* **Rojo (Error / Bloqueo)**: Errores del sistema o fallos de ejecución. Icono: `✖`

### Tono Conversacional
Los mensajes deben evitar tecnicismos internos y hablar en términos de las acciones del usuario (p. ej., usar "guardar progreso" en vez de "crear commit", y "conflictos detectados" en lugar de "merge conflict aborting").

---

## 🧭 Sintaxis y Comandos de la CLI

### 1. Inicializar Repositorio: `lit init`
* **Sintaxis**: `lit init`
* **Salida esperada**:
  ```bash
  ✔ ¡Repositorio LIT creado con éxito en este directorio!
  ✦ Tarea por defecto establecida en: 'main'
  ✦ Se ha guardado el estado inicial (Snapshot inicial) de tus archivos.
  ```

### 2. Estado del Workspace: `lit status`
* **Sintaxis**: `lit status`
* **Salida esperada (Workspace limpio)**:
  ```bash
  ✔ Todo al día. No hay cambios pendientes de guardar en la tarea 'main'.
  ```
* **Salida esperada (Con modificaciones)**:
  ```bash
  ✦ Estado actual en la tarea 'refactor-auth':
  
  Archivos modificados (esperando ser guardados):
    ⚠  src/AuthService.java
    ⚠  config/settings.json
  
  Archivos nuevos (se incluirán en el próximo guardado):
    ✦  src/models/User.java
  
  (Usa 'lit save "mensaje"' para guardar estos cambios en tu historial)
  ```

### 3. Guardar el Progreso: `lit save`
* **Sintaxis**: `lit save "[mensaje]"`
* **Salida esperada**:
  ```bash
  ✦ Guardando progreso...
  ✔ ¡Progreso guardado correctamente!
  ✦ Snapshot creado: #a1b2c3d (Añadido sistema de login seguro)
  ```
* **Error (Sin cambios)**:
  ```bash
  ✖ No se pudo guardar: Tu espacio de trabajo no contiene cambios respecto al último guardado.
  ```

### 4. Gestión de Tareas: `lit task`
* **Sintaxis**: `lit task` (Lista las tareas) o `lit task [nombre]` (Cambia de tarea o la crea)
* **Listado de Tareas**:
  ```bash
    * main (último guardado hace 2 horas)
      refactor-auth (último guardado hace 5 minutos)
      hotfix-db (último guardado ayer)
  ```
* **Cambio de Tarea (Sin cambios pendientes)**:
  ```bash
  ✔ Cambiado a la tarea 'hotfix-db'.
  ```
* **Cambio de Tarea (Con stashing automático)**:
  ```bash
  ✦ Guardando temporalmente tus cambios locales de la tarea 'refactor-auth'...
  ✔ Cambiado a la tarea 'main'.
  ```
  *(Al regresar a `refactor-auth`, LIT restaurará los archivos modificados a su estado temporal automáticamente).*

### 5. Sincronización Remota: `lit sync`
* **Sintaxis**: `lit sync`
* **Salida esperada (Sincronización limpia)**:
  ```bash
  ✦ Conectando con el servidor remoto...
  ✦ Descargando capturas de estado del servidor...
  ✦ Enviando tus capturas de estado locales...
  ✔ Sincronización finalizada correctamente. Tu proyecto está al día.
  ```

### 6. Deshacer Cambios: `lit undo`
* **Sintaxis**: `lit undo`
* **Salida esperada (Deshacer el último Snapshot guardado)**:
  ```bash
  ⚠ ¡Atención! Estás a punto de deshacer el último guardado (#a1b2c3d: "Corregido login").
  ✦ Los archivos modificados volverán a tu espacio de trabajo como cambios pendientes.
  ✔ Último guardado deshecho correctamente.
  ```
* **Salida esperada (Deshacer cambios del Workspace modificados por error)**:
  ```bash
  ✔ Se han restaurado los archivos al estado del último Snapshot.
  ```

---

## ⚡ Flujo Interactivo de Resolución de Conflictos

Cuando `lit sync` detecta que otra persona ha editado las mismas líneas en el servidor remoto, la CLI guía al usuario de la siguiente manera:

1. **Mensaje de Alerta**:
   ```bash
   ✖ ¡Conflicto de sincronización detectado!
   ⚠ Se han encontrado colisiones de cambios en 1 archivo.
   
     Archivo en conflicto: src/AuthService.java
   
   LIT ha pausado la sincronización para evitar sobrescribir código.
   ```
2. **Interfaz de Ayuda en Consola**:
   ```bash
   Por favor, abre 'src/AuthService.java' en tu editor de código. Verás marcas de conflicto como estas:
   
   <<<<<<< Tu versión local
   String userRole = "ADMIN";
   =======
   String userRole = "SUPER_USER";
   >>>>>>> Cambios entrantes del servidor
   
   1. Edita el archivo y decide qué versión conservar.
   2. Una vez guardado el archivo en tu editor, ejecuta:
      lit save "Resuelto conflicto de roles en AuthService"
   ```
3. **Bloqueo de Estado**:
   Si el usuario intenta hacer otra operación (como cambiar de tarea) mientras hay conflictos activos:
   ```bash
   ✖ Operación bloqueada: Debes resolver los conflictos en 'src/AuthService.java' antes de continuar.
   ```

---

## 🛡️ Guía de Robustez del Comando `lit undo`

El comando `lit undo` almacena un registro de auditoría local de las últimas 10 acciones del desarrollador en esa máquina.

* **Caso 1: Borrado de archivo accidental**: Si el usuario elimina un archivo y quiere recuperarlo, `lit undo` restaura el archivo desde la caché local del último Snapshot.
* **Caso 2: Guardado de Snapshot erróneo**: Al ejecutar `lit undo` inmediatamente después de un `save`, el Snapshot se elimina del historial y los archivos vuelven al estado de pendientes (Workspace).
* **Caso 3: Cambio de Tarea por error**: Si el usuario cambia de tarea y quiere volver a la anterior, `lit undo` lo regresa a la tarea anterior y restaura la caché transitoria de archivos locales si existía.
