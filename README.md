# LIT (Light Intelligent Tracking)

LIT es una capa de abstracción moderna, inteligente y simplificada construida sobre **Git**. 

El objetivo de LIT no es reemplazar a Git, sino actuar como su motor interno, ocultando la complejidad del área de preparación (staging), ramas complejas y comandos crípticos para ofrecer una experiencia limpia y libre de fricciones a la mayoría de los desarrolladores.

---

## 🚀 Requisitos Previos

Para ejecutar LIT en tu máquina local necesitas:
- **Java 17** o superior instalado y configurado en tu variable de entorno `PATH`.
- **Git CLI** instalado en el sistema.

---

## 🛠️ Compilación y Empaquetado

LIT se construye utilizando **Maven**. Para compilar y generar el archivo ejecutable de la CLI, ejecuta el siguiente comando en el directorio raíz del proyecto:

```bash
mvn clean package
```

Esto generará el archivo JAR empaquetado con todas sus dependencias en:
`target/lit-core-1.0-SNAPSHOT-jar-with-dependencies.jar`

---

## 📦 Instalación y Configuración (Windows)

Para poder utilizar el comando `lit` globalmente en tu terminal en Windows:

1. **Añadir al PATH**:
   Agrega la ruta absoluta de la raíz de este proyecto a la variable de entorno `PATH` de tu sistema.
2. **Uso del Script**:
   El proyecto contiene un script `lit.bat` en la raíz. Windows detectará automáticamente este script al escribir `lit` en cualquier terminal, redirigiendo la ejecución al ejecutable de Java compilado en `target/`.

*(Si estás en otro sistema operativo, puedes crear un alias de shell equivalente que apunte a `java -jar target/lit-core-1.0-SNAPSHOT-jar-with-dependencies.jar`)*.

---

## 🧭 Guía de Inicio Rápido

Prueba LIT en un directorio nuevo siguiendo estos sencillos pasos:

```bash
# 1. Crea y entra a una nueva carpeta de prueba
mkdir mi-proyecto-lit
cd mi-proyecto-lit

# 2. Inicializa el repositorio LIT
lit init

# 3. Crea un archivo
echo "Hola Mundo" > hola.txt

# 4. Revisa el estado de tu espacio de trabajo
lit status

# 5. Guarda tu progreso
lit save "Mi primer snapshot en LIT"

# 6. Revisa el estado de nuevo (ahora limpio)
lit status
```

---

## 📚 Documentación Adicional

Toda la documentación detallada del diseño de LIT, su arquitectura y guías de uso se encuentran dentro del directorio `/docs`:

- **[Índice de Documentación](docs/README.md)**: El mapa completo de toda la documentación.
- **[Guía del Usuario](docs/user/USER_GUIDE.md)**: Explicación a fondo del modelo mental y comandos.
- **[Ejemplos Prácticos](docs/user/EXAMPLES.md)**: Escenarios reales de trabajo paso a paso.
- **[Plan de Integración con IDEs](docs/design/IDE_INTEGRATION.md)**: Estrategia de integración futura en VS Code e IntelliJ.
