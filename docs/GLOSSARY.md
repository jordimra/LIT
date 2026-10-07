# Glosario de Términos — LIT

Este glosario define los conceptos centrales de LIT con el fin de unificar la terminología utilizada en la API, la CLI y la documentación del proyecto. Se evita de manera intencionada la terminología interna de Git para simplificar el modelo conceptual.

---

### Repository (Repositorio)
El contenedor lógico que almacena todo el historial de cambios, los metadatos y la configuración del proyecto. Físicamente, se corresponde con una carpeta especial (`.git` en el backend) que almacena la información de forma persistente.

### Workspace (Espacio de Trabajo)
El directorio local de archivos del usuario en su disco duro donde edita, añade y elimina ficheros de su proyecto. Es el estado actual de los archivos sobre los cuales el desarrollador realiza su trabajo.

### Snapshot (Captura de Estado)
Un registro inmutable del estado del proyecto en un momento determinado. Captura el contenido exacto de todos los archivos rastreados en el Workspace. A diferencia de Git, en LIT el concepto sustituye a términos complejos como "commit" o "tree".

### Task (Tarea)
Un flujo de trabajo o línea de progreso en la que se están implementando cambios. Permite trabajar de manera aislada en una característica o corrección sin interferir con otras tareas. Equivale conceptualmente a una "rama" (branch) en sistemas tradicionales, pero enfocada en la semántica del trabajo diario del desarrollador.

### Conflict (Conflicto)
Una situación que se produce cuando dos conjuntos de cambios incompatibles intentan fusionarse sobre el mismo archivo o línea de código, requiriendo intervención humana para decidir qué versión conservar.

### History (Historial)
La secuencia cronológica y dirigida de Snapshots (capturas de estado) que registran la evolución del proyecto a lo largo del tiempo. Permite navegar, comparar o regresar a estados pasados.

### Remote (Remoto)
Una versión del Repositorio alojada en un servidor externo (por ejemplo, en la nube o en la red local) que se utiliza para sincronizar el trabajo con otros miembros del equipo.
