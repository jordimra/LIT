# Plan de Integración con IDEs — LIT

Este documento detalla la estrategia y el diseño conceptual para la futura integración de **LIT** en los entornos de desarrollo integrado (IDEs) más populares, como **VS Code** e **IntelliJ IDEA**.

---

## 🏗️ Estrategia de Arquitectura

Para integrar LIT de forma eficiente sin duplicar lógica de negocio, se proponen dos caminos según la tecnología del IDE:

```
                  +--------------------------------+
                  |         IDE Frontend           |
                  +--------------------------------+
                     /                          \
     (IDE basado en JVM)                      (IDE no basado en JVM)
    Uso de API de Java Directa                Llamadas CLI o Daemon HTTP/IPC
           /                                      \
+-------------------------+              +--------------------------------+
|  lit-core.jar (Library) |              |  LIT CLI wrapper / Daemon API  |
+-------------------------+              +--------------------------------+
```

1. **Integración Nativa por API (IntelliJ / JVM)**:
   - Al ser LIT un proyecto escrito en Java 17, los plugins para IDEs de JetBrains pueden incluir el archivo JAR de LIT como dependencia directa.
   - Esto permite invocar los métodos del `LitService` directamente en memoria, logrando la máxima velocidad y evitando el overhead de levantar procesos del sistema.

2. **Integración por CLI / Daemon (VS Code / No-JVM)**:
   - Para editores como VS Code, el plugin puede interactuar llamando directamente al ejecutable CLI (`lit status`, `lit save`, etc.) y parseando su salida JSON o texto estructurado.
   - Alternativamente, para proyectos grandes, se puede diseñar un **LIT Daemon** (un micro-servicio en segundo plano que expone una interfaz gRPC o HTTP local) para evitar la latencia del inicio de la JVM en cada comando CLI.

---

## 💻 Integración en VS Code (Visual Studio Code)

VS Code provee una API robusta de **Source Control Utility** que permite reemplazar o complementar la pestaña nativa de Git.

### Componentes de la Extensión
1. **Source Control View (Control de Código Fuente)**:
   - Reemplazar el panel de cambios de Git por uno adaptado a LIT.
   - Mostrar dos grupos principales de archivos: "Nuevos (Added)" y "Modificados (Modified)".
   - Botón directo de **Save** (que pide el mensaje en una barra de input superior).
   - Acciones secundarias en el menú superior: **Sync** y **Undo**.

2. **Gestión de Tareas (Task Switcher)**:
   - Añadir un indicador en la **Barra de Estado** inferior mostrando la tarea activa actual (ej: `LIT: [main]`).
   - Al hacer clic en el indicador, desplegar una lista de tareas disponibles en el Workspace y la opción "Crear nueva tarea".
   - Integración con el stashing automático nativo de LIT al alternar tareas.

3. **Visualización y Resolución de Conflictos**:
   - Registrar un proveedor de contenido para esquemas de conflictos de texto.
   - Aprovechar la interfaz integrada de resolución de conflictos en tres paneles de VS Code (Tuyo, Entrante, Resultado).
   - Impedir que el usuario realice ciertas acciones visuales desde el IDE si `lit status` indica que hay conflictos activos.

---

## ☕ Integración en IntelliJ IDEA (y suite de JetBrains)

IntelliJ provee una API de control de versiones unificada (`VCS framework`).

### Componentes del Plugin
1. **LIT VCS Provider**:
   - Registrar LIT como un proveedor oficial de control de versiones (`AbstractVcs`).
   - Asociar la detección del directorio `.git/lit` con la activación automática del plugin para el proyecto actual.

2. **Ventana de Herramientas "LIT" (Tool Window)**:
   - Equivalente a la pestaña de "Git" actual pero simplificada.
   - **Local Changes**: Lista de cambios en el Workspace.
   - **LIT History**: Un árbol simplificado y lineal que muestra los snapshots anteriores, su autor y fecha. No muestra ramas complejas ni grafos enlazados, solo una línea de tiempo directa.

3. **Acciones de Guardado y Deshacer**:
   - Integrar un diálogo personalizado de "Guardar Snapshot" asignado al atajo clásico de Commit (`Ctrl+K` / `Cmd+K`).
   - Agregar una acción visual de "Deshacer Último Paso" en el menú contextual del proyecto que llame a `service.undo()`.
