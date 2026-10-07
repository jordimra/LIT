# Visión del Proyecto — LIT (Light Intelligent Tracking)

## ¿Qué es LIT?
LIT (Light Intelligent Tracking) es una capa de abstracción moderna e intuitiva construida sobre Git. No pretende reemplazar a Git, sino actuar como su motor interno y formato de almacenamiento, proporcionando a los desarrolladores una interfaz limpia, comprensible y libre de la complejidad accidental acumulada históricamente en Git.

## ¿Qué problemas intenta resolver?
- **Complejidad y curva de aprendizaje**: Reducir la barrera de entrada a sistemas de control de versiones eliminando conceptos redundantes o confusos.
- **Accidentes comunes**: Prevenir pérdidas accidentales de trabajo mediante comandos seguros y la capacidad de deshacer (`undo`) cualquier operación destructiva.
- **Inconsistencia de la CLI de Git**: Diseñar comandos predecibles con nombres claros y opciones coherentes.
- **Sobrecarga cognitiva**: Ocultar conceptos internos de Git (como index, reflog, detached HEAD) a los desarrolladores que solo necesitan rastrear su progreso y colaborar.

## ¿Qué problemas NO intenta resolver?
- **Reemplazar a Git**: LIT no define un nuevo formato de base de datos ni protocolos de red propios. Todo se almacena en Git estándar.
- **Soportar flujos de trabajo hiper-complejos directamente**: Aquellos desarrolladores que necesiten realizar operaciones forenses avanzadas o reescrituras de historial complejas deberán recurrir a la CLI nativa de Git.
- **Hospedaje de código**: LIT no proporciona servicios de servidor remoto en la nube (como GitHub o GitLab), sino que interactúa con ellos a través de los mecanismos estándar de Git.

## Público objetivo
- Desarrolladores junior y de nivel medio que encuentran Git intimidante o propenso a errores.
- Equipos de desarrollo que buscan flujos de trabajo más ágiles, limpios y unificados.
- Desarrolladores experimentados que desean una interfaz más rápida y libre de fricciones para las operaciones cotidianas del 95% de su tiempo de trabajo.

## Filosofía del proyecto
La filosofía de LIT se centra en cinco pilares esenciales descritos en [PRINCIPLES.md](file:///d:/Personal/proyectos/programar/API/LIT/docs/PRINCIPLES.md):
1. Simplicidad antes que potencia.
2. La API es el producto principal.
3. Git es un detalle de implementación.
4. Todo debe poder deshacerse.
5. Nunca sorprender al usuario.
