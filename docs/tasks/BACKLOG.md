# Backlog del Proyecto LIT (Light Intelligent Tracking)

Este documento detalla el roadmap y backlog oficial del proyecto, dividido en las fases definidas para el desarrollo del sistema de abstracción de Git.

---

## Fase 0 — Fundación (Completada)

### Objetivo

Establecer la base documental del proyecto y definir las reglas de trabajo que gobernarán todo el desarrollo.

### Tareas

- [x] Analizar `LIT.md` y crear la estructura inicial del proyecto.
- [x] Generar los documentos base (`VISION.md`, `PRINCIPLES.md`, `README.md`, etc.).
- [x] Crear el roadmap inicial del proyecto en `BACKLOG.md`.
- [x] Definir el formato de los ADR y crear los documentos iniciales.
- [x] Revisar y aprobar la estructura documental de la Fase 0.

### Definition of Done

- Toda la documentación inicial existe.
- La estructura documental es estable.
- El usuario aprueba el cierre de la fase.

---

## Fase 1 — Casos de uso y lenguaje de LIT (Completada)

> Antes de diseñar el dominio debemos entender cómo piensa el usuario.

### Objetivo

Definir el lenguaje funcional de LIT y los casos de uso que el sistema deberá resolver.

### Tareas

- [x] Identificar los casos de uso principales (crear repositorio, guardar cambios, sincronizar, deshacer, compartir, revisar historial, resolver conflictos...).
- [x] Documentar el flujo completo de cada caso de uso desde la perspectiva del usuario, sin mencionar Git.
- [x] Diseñar el vocabulario oficial de LIT (`save`, `sync`, `undo`, `task`, `publish`, etc.) justificando cada término elegido.
- [x] Documentar qué conceptos de Git desaparecerán de la interfaz pública y cuáles permanecerán ocultos.
- [x] Validar que cualquier desarrollador pueda comprender el flujo de trabajo de LIT leyendo únicamente esta documentación.

### Definition of Done

- Existe un documento `LANGUAGE.md`.
- Existe un documento `USE_CASES.md`.
- El vocabulario oficial de LIT está aprobado.

---

## Fase 2 — Modelo de dominio (Completada)

### Objetivo

Diseñar el modelo conceptual que da soporte a los casos de uso definidos en la fase anterior.

### Tareas

- [x] Identificar las entidades del dominio derivadas de los casos de uso.
- [x] Definir completamente `Repository`, indicando responsabilidades, ciclo de vida y relaciones.
- [x] Definir completamente `Workspace`.
- [x] Definir completamente `Snapshot`.
- [x] Definir completamente `Task`.
- [x] Definir completamente `History`.
- [x] Definir completamente `Conflict`.
- [x] Definir completamente `Remote`.
- [x] Documentar todas las relaciones entre entidades mediante diagramas.
- [x] Identificar invariantes y restricciones del modelo.
- [x] Revisar el modelo buscando redundancias o conceptos innecesarios.

### Definition of Done

- El modelo de dominio está completamente documentado.
- Todas las entidades tienen responsabilidades claramente definidas.
- No existen ambigüedades conocidas.

---

## Fase 3 — Diseño de la experiencia de usuario (Completada)

### Objetivo

Diseñar cómo interactuará un usuario con LIT antes de implementar ninguna API.

### Tareas

- [x] Diseñar la sintaxis completa de la CLI.
- [x] Diseñar ejemplos reales de uso para todas las operaciones principales.
- [x] Diseñar los mensajes de error, advertencia e información.
- [x] Diseñar el flujo de resolución de conflictos.
- [x] Diseñar el flujo de deshacer (`undo`).
- [x] Validar que un usuario sin conocimientos de Git pueda comprender la CLI.

### Definition of Done

- Existe una especificación completa de la CLI.
- Todos los comandos tienen ejemplos documentados.
- Los mensajes siguen una guía de estilo común.

---

## Fase 4 — Diseño de la API (Completada)

### Objetivo

Diseñar la API pública de LIT basándose en el dominio y en la experiencia de usuario previamente definidos.

### Tareas

- [x] Diseñar las interfaces públicas.
- [x] Definir las operaciones de alto nivel.
- [x] Diseñar la jerarquía de excepciones.
- [x] Definir las reglas de inmutabilidad y mutabilidad.
- [x] Diseñar la extensibilidad futura de la API.
- [x] Revisar la API buscando simplificaciones.

### Definition of Done

- API completamente documentada.
- Ninguna decisión depende todavía de Git.

---

## Fase 5 — Integración conceptual con Git (Completada)

### Objetivo

Diseñar cómo el modelo de LIT se implementará utilizando Git como motor interno.

### Tareas

- [x] Mapear cada operación pública de LIT con una o varias operaciones de Git.
- [x] Documentar todas las diferencias conceptuales entre ambos modelos.
- [x] Identificar limitaciones impuestas por Git.
- [x] Diseñar el almacenamiento de metadatos propios de LIT.
- [x] Definir la estrategia de compatibilidad con herramientas Git existentes.

### Definition of Done

- Toda operación de LIT tiene un comportamiento definido sobre Git.
- Las limitaciones están documentadas y justificadas.

---

## Fase 6 — Implementación del Core (Completada)

### Objetivo

Implementar el núcleo de LIT independientemente de Git.

### Tareas

- [x] Configurar el proyecto Java.
- [x] Implementar el modelo de dominio.
- [x] Implementar los servicios principales.
- [x] Implementar pruebas unitarias.
- [x] Alcanzar una cobertura mínima acordada.

---

## Fase 7 — Adaptador Git (Completada)

### Objetivo

Conectar el núcleo con repositorios Git reales.

### Tareas

- [x] Implementar el adaptador Git.
- [x] Ejecutar pruebas de integración.
- [x] Validar la interoperabilidad con herramientas Git existentes.

---

## Fase 8 — CLI (Completada)

### Objetivo

Implementar la interfaz de línea de comandos diseñada previamente.

### Tareas

- [x] Implementar el parser de comandos.
- [x] Integrar la CLI con el Core.
- [x] Implementar la ayuda integrada.
- [x] Implementar pruebas E2E.

---

## Fase 9 — Robustez (Completada)

### Objetivo

Aumentar la calidad del producto antes de la publicación.

### Tareas

- [x] Implementar el sistema global de `undo`.
- [x] Mejorar la recuperación ante errores.
- [x] Optimizar rendimiento.
- [x] Realizar pruebas de estrés.

---

## Fase 10 — Publicación (Completada)

### Objetivo

Preparar la primera versión pública de LIT.

### Tareas

- [x] Redactar la documentación para usuarios.
- [x] Preparar ejemplos completos.
- [x] Publicar la primera versión.
- [x] Planificar la integración con IDEs.

---

## Fase 11 — Beta Testing & Endurecimiento (En progreso)

### Objetivo

Garantizar la resiliencia absoluta del núcleo frente a escenarios impredecibles, errores de red y manipulación directa de Git, antes de publicar la versión final.

### Tareas

- [ ] Refactorizar el parseo de registros (`audit_log.json`) para usar un parser JSON estándar (Jackson).
- [ ] Eliminar textos harcodeados en la CLI y usar el modelo del dominio para mostrar estados (como en conflictos).
- [ ] Implementar gestión robusta y amigable frente a fallos de sincronización y conexión de red.
- [ ] Probar la tolerancia del sistema y del Audit Log inyectando comandos Git directos (`git checkout`, `git reset`, `git commit`).
- [ ] Realizar "La prueba de los 5 minutos" con desarrolladores reales trabajando a ciegas y validar el modelo conceptual.
- [ ] Implementar un suite de automatización extensa (`LitResilienceTest.java`) para blindar flujos atípicos.

### Definition of Done

- El sistema sobrevive y se recupera de manipulaciones manuales comunes usando Git.
- El log de auditoría nunca se corrompe por mensajes arbitrarios del usuario.
- `sync` maneja bien la desconexión del remoto sin romper nada.
- Usuarios beta utilizan LIT sin requerir explicaciones técnicas.
