# AGENTS.md — LIT (Light Intelligent Tracking)

## Qué es
CLI Java sobre Git (Git = motor interno, invocado como proceso). Módulo Maven único `com.lit:lit-core`, Java 17. Docs y mensajes de usuario en español.

## Comandos
- Build: `mvn clean package` (o `mvn compile` para classes)
- Tests: `mvn test` · test único: `mvn test -Dtest=Clase` o `mvn test -Dtest=Clase#metodo`
- Ejecutar CLI: `.\lit.bat <comando>` (requiere `mvn compile` antes; usa `target\classes` + `target\dependency\*`)
- Prerequisitos: JDK 17+ y **Git CLI en PATH** (LIT y los tests del adaptador ejecutan git real en repos temporales)
- Sin lint/typecheck/CI configurados; el chequeo de calidad es `mvn test`
- ⚠️ El README menciona un fat-jar `*-jar-with-dependencies.jar` que **no se genera** (el pom no tiene plugin shade/assembly). El mecanismo real es `lit.bat` + classes + dependency.

## Arquitectura (dependencias estrictas: CLI → API → Core → Adapter → Git)
- `com.lit.api` — interfaz `LitService`, records y excepciones; sin dependencias de Git
- `com.lit.core.domain` — modelo de dominio puro
- `com.lit.core.service` — `InMemoryLitService` (implementación en memoria, para tests)
- `com.lit.core.adapter.git.GitAdapterLitService` — implementación real vía git CLI
- `com.lit.cli.LitCli` — entrypoint (`main`); actualmente cablea `GitAdapterLitService`
- Metadatos de LIT: `.git/lit/` (`audit_log.json` impulsa `undo`; stashes con prefijo `lit-auto-stash:<task>`; ver ADR-0003)

## Terminología (prohibida la jerga Git en interfaz y docs)
`task`=branch · `snapshot`=commit · `save`=add+commit · `sync`=fetch+rebase+push · `undo`. Referencia: `docs/LANGUAGE.md`.

## Proceso de trabajo (PROJECT.md)
- `docs/` es la fuente de verdad: leerla antes de cambiar; nunca decidir solo con el historial del chat
- Decisiones de arquitectura → ADR en `docs/adr/`; toda iteración actualiza docs afectadas + `docs/tasks/BACKLOG.md`
- No avanzar de fase sin aprobación explícita; conflictos entre docs: documentar, proponer alternativas, esperar aprobación
- Cambios de diseño: workflow OpenSpec (`openspec/` + skills `opsx-*`: propose → apply → sync → archive)
- Estado actual: Fase 11 (beta/endurecimiento) en progreso

## Gotchas
- `docs/README.md` es referenciado desde README.md y el help de la CLI, pero no existe
- `lit init` rechaza directorios con `.git` previo; crea commit vacío inicial y renombra a `main`
- `lit sync` requiere remote `origin`; sin él lanza error explícito
