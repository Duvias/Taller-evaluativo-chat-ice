# Chat Multimedia Distribuido

Proyecto académico de chat distribuido con:

- **ZeroC Ice / TCP** para control, chat, salas, callbacks, archivos y señalización de llamadas.
- **UDP** para el transporte de audio.
- **CLI** como interfaz de usuario.
- **Gradle** como sistema de construcción.

## Requisitos de desarrollo

- JDK **17**
- Gradle
- ZeroC Ice **3.8.x**

La compilación de Slice se realiza con el plugin oficial `com.zeroc.slice-tools`.

## Módulos

```text
chat-multimedia/
├── common/   # Contrato Slice y elementos compartidos
├── server/   # Servidor Ice y managers
├── client/   # Cliente CLI, callbacks y módulos cliente
└── docs/     # Arquitectura, Git y distribución del trabajo
```

## Construcción

Si el repositorio ya tiene Gradle Wrapper:

```bash
./gradlew clean build
```

En Windows:

```powershell
.\gradlew.bat clean build
```

Si todavía no existe el wrapper, desde la raíz se puede generar una vez con:

```bash
gradle wrapper
```

y luego versionar `gradlew`, `gradlew.bat` y `gradle/wrapper/`.

## Ejecución prevista

Servidor:

```bash
./gradlew :server:run
```

Cliente:

```bash
./gradlew :client:run
```

> La rama `develop` contiene la base arquitectónica. Las funcionalidades se implementan en ramas `feature/*`.

## Ramas

```text
main
└── develop
    ├── feature/ice-usuarios
    ├── feature/salas-archivos
    └── feature/voz-udp
```

Ver `docs/GIT_SETUP.md` para los comandos recomendados.

## Puertos base

- Ice/TCP: `10000`
- Relay UDP de voz: `12000`

Estos valores están centralizados en `NetworkConfig.java` y pueden sobrescribirse con propiedades JVM.
