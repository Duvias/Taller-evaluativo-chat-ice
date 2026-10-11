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