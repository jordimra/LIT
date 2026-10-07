# Ejemplos Prácticos de LIT (Light Intelligent Tracking)

Este documento muestra ejemplos reales paso a paso de los flujos de trabajo más comunes utilizando LIT. Cada escenario detalla la entrada de comandos, las salidas esperadas en consola y lo que ocurre tras bambalinas.

---

## Escenario 1: Inicialización y Primer Guardado (Flujo Básico)

Imagina que empiezas un nuevo proyecto desde cero.

1. **Inicializar el repositorio LIT**:
   ```bash
   $ lit init
   ✔ ¡Repositorio LIT creado con éxito en este directorio!
   ✦ Tarea por defecto establecida en: 'main'
   ✦ Se ha guardado el estado inicial (Snapshot inicial) de tus archivos.
   ```
   *Qué pasó*: LIT inicializó un repositorio Git por debajo y creó el primer snapshot vacío automático para tener un punto de partida para futuros `undo`.

2. **Crear y modificar archivos**:
   Crea un archivo llamado `Index.html` con contenido básico.

3. **Verificar el estado del Workspace**:
   ```bash
   $ lit status
   ✦ Estado actual en la tarea 'main':
   
   Archivos nuevos (se incluirán en el próximo guardado):
     ✦  Index.html
   
   (Usa 'lit save "mensaje"' para guardar estos cambios en tu historial)
   ```

4. **Guardar el progreso**:
   ```bash
   $ lit save "Estructura inicial HTML"
   ✦ Guardando progreso...
   ✔ ¡Progreso guardado correctamente!
   ✦ Snapshot creado: #b7a82c1 (Estructura inicial HTML)
   ```

---

## Escenario 2: Cambio de Tarea con Guardado Temporal (Stashing)

Estás trabajando en una nueva característica de autenticación, pero surge una emergencia y debes volver a la rama estable (`main`).

1. **Crear y cambiar a una nueva tarea**:
   ```bash
   $ lit task feature-auth
   ✔ Cambiado a la tarea 'feature-auth'.
   ```

2. **Escribir código de autenticación (sin guardar)**:
   Modificas `Index.html` añadiendo un formulario de login.
   ```bash
   $ lit status
   ✦ Estado actual en la tarea 'feature-auth':
   
   Archivos modificados (esperando ser guardados):
     ⚠  Index.html
   ```

3. **Cambiar abruptamente de vuelta a `main`**:
   ```bash
   $ lit task main
   ✦ Guardando temporalmente tus cambios locales de la tarea 'feature-auth'...
   ✔ Cambiado a la tarea 'main'.
   ```
   *Qué pasó*: Como tenías cambios sin guardar en `feature-auth`, LIT los guardó automáticamente en la recámara (stash de Git) antes de devolverte a `main` con tu espacio limpio.

4. **Verificar que `main` está limpio**:
   Si abres `Index.html`, verás que el formulario de login no está.
   ```bash
   $ lit status
   ✔ Todo al día. No hay cambios pendientes de guardar en la tarea 'main'.
   ```

5. **Volver a la tarea de autenticación**:
   ```bash
   $ lit task feature-auth
   ✔ Cambiado a la tarea 'feature-auth'.
   ```
   *Qué pasó*: LIT detectó que tenías cambios pendientes guardados transitoriamente para `feature-auth`, los aplicó de nuevo y limpió la caché temporal.
   ```bash
   $ lit status
   ✦ Estado actual en la tarea 'feature-auth':
   
   Archivos modificados (esperando ser guardados):
     ⚠  Index.html
   ```
   ¡Tu formulario de login vuelve a estar en el archivo!

---

## Escenario 3: Sincronización y Resolución de Conflictos

Vas a subir tu tarea terminada al servidor compartido, pero un compañero ha editado las mismas líneas en el servidor remoto.

1. **Intentar sincronizar**:
   ```bash
   $ lit sync
   ✦ Conectando con el servidor remoto...
   ✦ Descargando capturas de estado del servidor...
   ✖ ¡Conflicto de sincronización detectado!
   ⚠ Se han encontrado colisiones de cambios en 1 archivo.
   
     Archivo en conflicto: Index.html
   
   LIT ha pausado la sincronización para evitar sobrescribir código.
   ```

2. **Resolver el conflicto**:
   Si abres `Index.html`, verás estas marcas:
   ```html
   <<<<<<< Tu versión local
   <h1>Iniciar Sesión (LIT)</h1>
   =======
   <h1>Login de Usuario</h1>
   >>>>>>> Cambios entrantes del servidor
   ```
   Editas el archivo en tu editor favorito, dejas la versión definitiva y eliminas las marcas (por ejemplo, decides fusionar ambas en `<h1>Iniciar Sesión / Login</h1>`).

3. **Guardar la resolución**:
   ```bash
   $ lit save "Resuelto conflicto de cabecera en Index"
   ✦ Guardando progreso...
   ✔ ¡Progreso guardado correctamente!
   ✦ Snapshot creado: #f5c8e23 (Resuelto conflicto de cabecera en Index)
   ```
   La sincronización ahora está completa y el historial de tu equipo es lineal y ordenado.

---

## Escenario 4: Corregir un error de inmediato (`lit undo`)

LIT te permite corregir un error fácilmente.

### Caso A: Modificaste un archivo por error y quieres borrar esos cambios
1. Modificas `Index.html` con texto no deseado.
2. Ejecutas `lit undo`:
   ```bash
   $ lit undo
   ✔ Se han restaurado los archivos al estado del último Snapshot.
   ```
   El archivo vuelve a su estado limpio del último guardado de forma segura.

### Caso B: Creaste un Snapshot pero te arrepientes (p.ej. olvidaste añadir un archivo o escribiste mal el mensaje)
1. Guardas el progreso:
   ```bash
   $ lit save "Commti mal escrito"
   ```
2. Ejecutas `lit undo` teniendo el workspace limpio:
   ```bash
   $ lit undo
   ⚠ ¡Atención! Estás a punto de deshacer el último guardado (#a2f4d1a: "Commti mal escrito").
   ✦ Los archivos modificados volverán a tu espacio de trabajo como cambios pendientes.
   ✔ Último guardado deshecho correctamente.
   ```
   El snapshot desaparece del historial y tus archivos modificados vuelven a aparecer en `lit status` para que puedas corregirlos y volver a guardarlos.
