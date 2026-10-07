# Casos de Uso — LIT

Este documento describe los casos de uso esenciales del sistema LIT desde la perspectiva del usuario final. En consonancia con el principio de que Git es un detalle de implementación, los flujos aquí descritos prescinden por completo de la jerga de Git y se enfocan en las necesidades reales del desarrollador.

---

## 1. Inicialización de un Repositorio (Crear Proyecto)

### Actores
- Desarrollador

### Precondiciones
- El desarrollador tiene una carpeta local en su disco duro donde reside (o residirá) el código de su proyecto.

### Flujo Principal
1. El desarrollador solicita inicializar el control de versiones en el directorio actual.
2. El sistema configura las carpetas internas necesarias para comenzar el seguimiento.
3. El sistema confirma al usuario la creación exitosa del repositorio LIT y establece una Tarea por defecto (`main`).
4. El sistema realiza una primera captura automática de base (Snapshot inicial) si la carpeta ya contiene archivos.

---

## 2. Consulta de Estado (Ver qué está pasando)

### Actores
- Desarrollador

### Flujo Principal
1. El desarrollador solicita ver el estado actual del espacio de trabajo.
2. El sistema escanea los archivos en el disco y los compara con la captura de estado (Snapshot) activa.
3. El sistema presenta al usuario una lista clara organizada en tres secciones lógicas:
   - Archivos nuevos (aún no rastreados).
   - Archivos modificados.
   - Archivos eliminados.
4. Si hay conflictos activos, el sistema los destaca al principio de la lista indicando qué archivos requieren atención.

---

## 3. Guardar el Trabajo (Tomar una Captura de Estado)

### Actores
- Desarrollador

### Precondiciones
- El espacio de trabajo contiene cambios detectados (archivos nuevos, modificados o eliminados) y no hay conflictos bloqueantes.

### Flujo Principal
1. El desarrollador solicita guardar el progreso actual proporcionando un mensaje descriptivo.
2. El sistema congela de forma atómica el estado de todos los archivos del espacio de trabajo.
3. El sistema crea un Snapshot inmutable y lo asocia cronológicamente a la Tarea activa.
4. El sistema informa al desarrollador de que el trabajo se ha guardado de forma segura y limpia el listado de cambios pendientes.

---

## 4. Aislar un Flujo de Trabajo (Cambiar de Tarea)

### Actores
- Desarrollador

### Flujo Principal
1. El desarrollador solicita cambiar a una Tarea específica (por ejemplo, `fix-login`).
2. El sistema verifica si la Tarea ya existe:
   - Si no existe, la crea a partir del Snapshot activo de la Tarea actual.
3. El sistema comprueba si hay cambios sin guardar en el espacio de trabajo actual:
   - Si los hay, los resguarda automáticamente de forma temporal para evitar pérdidas.
4. El sistema actualiza los archivos del espacio de trabajo en el disco para que coincidan exactamente con el último estado guardado en la Tarea de destino.
5. El sistema informa de que se ha cambiado de tarea con éxito.

---

## 5. Revisar el Historial de Cambios

### Actores
- Desarrollador

### Flujo Principal
1. El desarrollador solicita ver la cronología del proyecto.
2. El sistema recopila los Snapshots de la Tarea actual en orden descendente (del más nuevo al más antiguo).
3. El sistema presenta cada Snapshot con su autor, fecha, identificador corto y mensaje descriptivo.

---

## 6. Sincronizar el Proyecto con el Equipo

### Actores
- Desarrollador

### Precondiciones
- El repositorio está conectado a un servidor de sincronización externo (Remote).

### Flujo Principal
1. El desarrollador solicita sincronizar sus cambios.
2. El sistema se conecta al servidor remoto y realiza una doble operación en un único paso:
   - Trae e integra las capturas de estado del servidor en la Tarea local correspondiente.
   - Envía las nuevas capturas de estado locales que aún no estén en el servidor.
3. Si no hay conflictos, los archivos locales se actualizan y el sistema confirma que el proyecto está al día.

### Flujo Alternativo (Conflictos en Sincronización)
1. Durante la integración, el sistema detecta que otra persona modificó las mismas líneas en el servidor remoto.
2. El sistema descarga las capturas del servidor, pero detiene el proceso de sincronización.
3. El sistema marca el espacio de trabajo en "Estado de Conflicto", marcando los archivos colisionados y guiando al usuario sobre cómo resolverlos.

---

## 7. Resolver Conflictos

### Actores
- Desarrollador

### Precondiciones
- El espacio de trabajo está en "Estado de Conflicto".

### Flujo Principal
1. El desarrollador abre los archivos marcados con conflictos.
2. El desarrollador decide manualmente qué líneas de código conservar (su versión local, la versión remota o una combinación de ambas).
3. El desarrollador marca los archivos como resueltos.
4. Una vez resueltos todos los conflictos, el sistema permite volver a guardar el trabajo mediante un nuevo Snapshot, finalizando la sincronización.

---

## 8. Deshacer el Último Cambio (Red de Seguridad)

### Actores
- Desarrollador

### Flujo Principal
1. El desarrollador solicita deshacer su última acción.
2. El sistema analiza el registro de acciones reciente.
3. Si la última acción fue guardar un Snapshot:
   - El sistema disuelve el Snapshot en el historial (los cambios vuelven al estado de "no guardados" en el espacio de trabajo sin perder código).
4. Si la última acción fue modificar archivos locales por error (p. ej. al descartar cambios):
   - El sistema recupera el estado de los archivos antes del descarte.
5. El sistema confirma la restauración del estado anterior.
