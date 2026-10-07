# Principios de Diseño — LIT (Light Intelligent Tracking)

Este documento detalla los cinco principios fundamentales que guían el diseño de la arquitectura, la API pública, la CLI y las decisiones internas de desarrollo de LIT.

---

### 1. Simplicidad antes que potencia
La facilidad de uso y la claridad mental del desarrollador son prioritarias frente a capacidades complejas o poco comunes. Si una funcionalidad añade demasiada complejidad cognitiva para el 95% de los casos de uso, debe omitirse o rediseñarse.

### 2. La API es el producto principal
La interfaz de programación de aplicaciones (API) y la interfaz de línea de comandos (CLI) de LIT son los activos más valiosos del proyecto. Deben ser consistentes, predecibles, auto-explicativas y estar perfectamente documentadas antes de escribir cualquier código de producción.

### 3. Git es un detalle de implementación
Aunque LIT almacena toda la información y el historial en un repositorio Git estándar, el usuario no debería tener que saber cómo interactúa LIT internamente con Git. Todos los conceptos expuestos hacia afuera pertenecen al dominio conceptual de LIT, no al de Git.

### 4. Todo debe poder deshacerse
Ninguna acción del usuario en LIT debe provocar la pérdida irreversible de información local. Siempre debe ser posible regresar al estado anterior del espacio de trabajo o del historial de manera segura y mediante un comando unificado (`undo`).

### 5. Nunca sorprender al usuario
Las operaciones deben tener comportamientos predecibles y de sentido común. No deben existir efectos secundarios ocultos ni comportamientos mágicos inesperados. El principio de menor sorpresa rige sobre cada interacción CLI y llamada de API.
