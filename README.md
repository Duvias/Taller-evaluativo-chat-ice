# Integrantes
- Juan Fernando Devia
- Daniel Varela
- Camilo Toro Agudelo

# Chat Multimedia Distribuido

Proyecto académico de chat distribuido con:

- **ZeroC Ice / TCP** para control, chat, salas, callbacks, archivos y señalización de llamadas.
- **UDP** para el transporte de audio.
- **CLI** como interfaz de usuario.
- **Gradle Wrapper** para una construcción reproducible en todo el equipo.

## Requisitos

- JDK **17 o superior**.
- No es necesario instalar Gradle manualmente: el repositorio incluye el Wrapper.
- ZeroC Ice **3.8.3** se descarga como dependencia del proyecto.

Comprueba Java con:

```powershell
java -version
```

## Módulos

```text
chat-multimedia/
├── common/   # Slice, configuración y código compartido
├── server/   # Servidor Ice y managers
├── client/   # Cliente CLI, callbacks y módulos cliente
└── docs/     # Arquitectura, Git y distribución del trabajo
```

## Construcción

Windows:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

El proyecto compila con `--release 17`, por lo que puede ejecutarse el build con cualquier JDK 17+ manteniendo bytecode compatible con Java 17.

## Ejecución

Servidor:

```powershell
.\gradlew.bat :server:run
```

Cliente, en otra terminal:

```powershell
.\gradlew.bat :client:run
```

El servidor base registra el servicio Ice con identidad `chatService` en el puerto TCP `10000`.

## Ice 3.8 y `Communicator`

En Java con Ice 3.8 la inicialización correcta puede hacerse con el constructor público:

```java
try (Communicator communicator = new Communicator(args)) {
    // ...
}
```

Por eso `Client.java` y `Server.java` mantienen esta forma de inicialización.

## Ramas

```text
main
└── develop
    ├── feature/ice-usuarios
    ├── feature/salas-archivos
    └── feature/voz-udp
```

La infraestructura común (Wrapper, configuración de build y correcciones compartidas) debe entrar primero a `develop`. La implementación de voz se integra después desde `feature/voz-udp`.

## Puertos base

- Ice/TCP: `10000`
- Relay UDP de voz: `12000`

Los valores están centralizados en `common/src/main/java/chat/multimedia/common/NetworkConfig.java`.

## Pruebas actuales

La base incluye pruebas para:

- configuración compartida;
- creación del proxy del cliente;
- registro base del servicio Ice del servidor;
- estado inicial de llamadas 1 a 1 (`VoiceManager`).

## Comandos de salas, archivos y CLI

```text
/login <nickname>
/create <sala>
/join <sala>
/leave <sala>
/rooms
/roommsg <sala> <mensaje>
```

Crear una sala no une automáticamente al creador: después de `/create general`, ejecutar `/join general`.

## Comandos de archivos

Enviar a un usuario:

```text
/sendfile user <nickname> <ruta>
```

Enviar a una sala (el remitente debe pertenecer a ella):

```text
/sendfile room <sala> <ruta>
```

También se acepta la forma abreviada para enviar a un usuario:

```text
/sendfile <nickname> <ruta>
```

Las rutas que contienen espacios se pueden encerrar entre comillas. Los archivos recibidos se guardan por defecto en `received_files/`; el directorio se puede cambiar con `-Dchat.files.dir=<ruta>`.

El tamaño de fragmento es 32 KiB y la transferencia utiliza `ChatService.sendFileChunk` sobre Ice. No se abre un socket TCP alternativo para archivos.

## Callback y ejecución en otra máquina

Por defecto el callback se publica en `127.0.0.1`, suficiente para ejecutar servidor y clientes en la misma máquina. Si los clientes se ejecutan en otra máquina, configura la dirección alcanzable del cliente mediante la propiedad JVM `chat.callback.host` y permite conexiones entrantes al puerto TCP dinámico del callback.