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

# Respuestas Cuestionario de Análisis Conceptual y Decisiones de Diseño

## 1. Arquitectura híbrida TCP/UDP

La arquitectura del proyecto separa el **plano de control** del **plano de medios**. El control, el chat de texto, la transferencia de archivos, la presencia de usuarios y la señalización de llamadas se ejecutan mediante **ZeroC Ice sobre TCP**, mientras que el audio de voz se transmite mediante **UDP**.

Esta separación existe porque ambos tipos de tráfico tienen prioridades diferentes. Las operaciones de login/logout, los mensajes de chat, la creación de salas y los archivos necesitan **entrega confiable, ordenada y sin pérdida de información**. TCP ofrece estas propiedades y ZeroC Ice agrega encima el modelo RPC, serialización mediante Slice, proxies remotos, callbacks y manejo de excepciones.

La voz tiene una prioridad distinta: **es más importante que el audio llegue a tiempo que que llegue perfecto**. Una muestra de audio que llega varios cientos de milisegundos tarde ya perdió gran parte de su utilidad en una conversación. UDP evita retransmisiones y no obliga a esperar paquetes anteriores, por lo que mantiene una latencia menor incluso cuando existen pérdidas.

Si la voz se enviara sobre Ice/TCP, una pérdida de segmentos provocaría retransmisiones. Como TCP entrega los datos en orden, los segmentos posteriores quedan retenidos hasta recuperar el segmento faltante. Este efecto, conocido como **Head-of-Line Blocking**, produciría pausas, aumento de latencia y acumulación temporal de audio. Después del bloqueo podrían llegar muestras que ya son antiguas para una conversación en tiempo real.

El **jitter** también sería más perjudicial sobre TCP. La variación del tiempo de llegada, sumada a retransmisiones y buffers, puede generar picos de latencia. En archivos o chat esto es aceptable; en voz se percibe como retardos, cortes y dificultad para mantener una conversación natural.

Con UDP, un datagrama perdido puede simplemente descartarse y el flujo continúa con el siguiente. Puede producirse un pequeño defecto acústico, pero se evita detener toda la conversación esperando una retransmisión tardía.

| Tipo de tráfico | Transporte | Razón principal |
|---|---|---|
| Login, logout y presencia | Ice/TCP | Confiabilidad y consistencia |
| Chat privado y salas | Ice/TCP | Orden y entrega garantizada |
| Transferencia de archivos | Ice/TCP | Integridad de los datos |
| Señalización de llamadas | Ice/TCP | Consistencia del estado de llamada |
| Audio de voz | UDP | Baja latencia y tolerancia a pérdidas |

En síntesis, la arquitectura híbrida utiliza **TCP para información que no puede perderse y UDP para información de tiempo real que no debe llegar tarde**.

---

## 2. Manejo de buffer y chunking en Ice

Enviar un video de 80 MB mediante una única llamada RPC con un parámetro `sequence<byte>` no es adecuado en ZeroC Ice.

En Ice 3.8, la propiedad `Ice.MessageSizeMax` limita por defecto el tamaño máximo de un mensaje Ice entrante a **1024 KiB, aproximadamente 1 MiB**. El límite corresponde al mensaje completo del protocolo, no solamente al arreglo de bytes. También existe `<Adapter>.MessageSizeMax`, que permite establecer un límite específico para un adaptador.

Por lo tanto, un RPC de 80 MB supera ampliamente el límite predeterminado. El receptor rechaza el mensaje y, usando un transporte orientado a conexión como TCP, la conexión se cierra. En una solicitud demasiado grande de cliente a servidor, el cliente normalmente termina observando una excepción de conexión, como `ConnectionLostException`. En otros escenarios o versiones pueden aparecer excepciones asociadas al tamaño del mensaje, memoria o marshaling, pero el comportamiento fundamental es el mismo: **el mensaje no es aceptado con la configuración por defecto**.

Podría aumentarse `Ice.MessageSizeMax`, pero esto no es una solución ideal para archivos grandes. Un RPC de decenas de megabytes provoca mayores picos de memoria, serializaciones grandes y una recuperación poco eficiente si ocurre una falla.

La técnica utilizada en el proyecto es **fragmentar el archivo en chunks**. Por ejemplo, utilizando bloques de 32 KiB:

```text
Archivo de 80 MiB
        |
        v
+-----------+
| Chunk 0   | 32 KiB
+-----------+
| Chunk 1   | 32 KiB
+-----------+
| Chunk 2   | 32 KiB
+-----------+
|    ...    |
+-----------+
| Chunk N   |
+-----------+
```

Cada chunk se envía en una invocación Ice independiente y lleva información suficiente para reconstruir el archivo, por ejemplo:

- identificador de transferencia;
- nombre del archivo;
- tamaño total;
- índice del chunk;
- número total de chunks;
- remitente y destinatario.

Con chunks de 32 KiB, un archivo de 80 MiB requiere aproximadamente:

```text
80 MiB = 80 × 1024 KiB
(80 × 1024) / 32 = 2560 chunks
```

El receptor reconstruye el archivo usando el índice de cada bloque. Este diseño evita exceder el límite de Ice y reduce la cantidad de memoria requerida por cada invocación.

Las propiedades más importantes son:

- `Ice.MessageSizeMax`: límite global del `Communicator` para mensajes entrantes;
- `<Adapter>.MessageSizeMax`: límite específico de mensajes recibidos por un `ObjectAdapter`;
- `Ice.Warn.Connections`: útil para diagnosticar rechazos y cierres de conexión.

La fragmentación ofrece además otras ventajas: permite reportar progreso, facilita validaciones y evita que toda la transferencia dependa de una única llamada RPC gigantesca.

---

## 3. Escalabilidad en llamadas grupales: Relay vs. Mesh

Para las llamadas grupales el equipo eligió un **Audio Relay centralizado**. El servidor recibe el audio UDP de cada participante y lo reenvía a los demás usuarios de la llamada.

La alternativa sería una topología **Mesh Peer-to-Peer**, en la que cada cliente envía directamente su audio a todos los demás participantes.

Supongamos una sala con `N` participantes y que todos generan un paquete de audio en el mismo intervalo de tiempo.

### Mesh Peer-to-Peer

Cada cliente debe enviar su paquete a los otros `N - 1` participantes:

```text
Paquetes enviados por cada cliente = N - 1
```

Como hay `N` clientes, el total de transmisiones UDP es:

```text
Total Mesh = N(N - 1)
```

La red del servidor no transporta audio, por lo que su carga de medios es prácticamente cero. El servidor puede seguir participando en la señalización mediante Ice, pero no reenvía datagramas de voz.

Ejemplo con 5 participantes:

```text
Cada cliente envía: 4 paquetes
Total: 5 × 4 = 20 transmisiones
Carga UDP de audio en el servidor: 0
```

El problema del Mesh es que el ancho de banda de subida requerido a cada usuario aumenta con el número de participantes. Con muchos usuarios, cada cliente debe mantener y alimentar varios destinos UDP simultáneamente.

### Relay centralizado

En el Relay cada cliente envía únicamente **un paquete** al servidor:

```text
Paquetes enviados por cada cliente = 1
```

El servidor recibe un paquete de cada participante:

```text
Entrada al servidor = N
```

Luego debe reenviar cada paquete a los otros `N - 1` participantes:

```text
Salida del servidor = N(N - 1)
```

Si se cuentan tanto los paquetes recibidos como los enviados por la interfaz del servidor:

```text
Carga total del servidor = N + N(N - 1)
                         = N²
```

Ejemplo con 5 participantes:

```text
Cada cliente envía: 1 paquete
Servidor recibe: 5 paquetes
Servidor reenvía: 20 paquetes
Total que atraviesa la red del servidor: 25 paquetes
```

### Comparación

| Característica | Mesh P2P | Relay centralizado |
|---|---:|---:|
| Paquetes enviados por cada cliente | `N - 1` | `1` |
| Total enviado por clientes | `N(N - 1)` | `N` |
| Paquetes recibidos por servidor | `0` | `N` |
| Paquetes enviados por servidor | `0` | `N(N - 1)` |
| Carga total de medios en servidor | `0` | `N²` contando entrada y salida |
| Complejidad del cliente | Mayor | Menor |
| Consumo de subida del cliente | Crece con `N` | Constante por flujo propio |
| Punto central de fallo | No | Sí |

La decisión de usar Relay traslada el costo de red hacia el servidor, pero simplifica mucho los clientes. Cada usuario mantiene un único destino UDP y el servidor puede controlar centralmente la pertenencia a la llamada.

Para la escala académica del proyecto, este compromiso es conveniente. El Relay implementado se comporta conceptualmente como un **SFU simple**: no mezcla todas las voces en una única señal, sino que recibe los datagramas de un participante y los reenvía al resto.

---

## 4. Transparencia y callbacks en Ice

Los callbacks permiten que el servidor invoque operaciones sobre un objeto que vive en el cliente. Esto invierte temporalmente la dirección normal de una llamada remota: el cliente primero entrega al servidor un proxy de callback y, posteriormente, el servidor utiliza ese proxy cuando necesita notificar un evento.

En el proyecto los callbacks sirven para eventos como:

- mensajes privados;
- mensajes de sala;
- conexión y desconexión de usuarios;
- recepción de chunks de archivo;
- llamadas entrantes;
- aceptación, rechazo o finalización de llamadas;
- eventos de llamadas grupales.

El ciclo de vida puede representarse así:

```text
Cliente
  |
  | 1. crea ObjectAdapter
  v
ObjectAdapter del cliente
  |
  | 2. registra ClientCallbackImpl
  v
ClientCallbackPrx
  |
  | 3. envía el proxy durante login
  v
Servidor
  |
  | 4. almacena proxy por usuario
  |
  | 5. ocurre un evento
  v
Servidor invoca callback
  |
  v
Cliente recibe el evento
```

### 1. Creación del `ObjectAdapter`

El cliente necesita un adaptador porque debe recibir invocaciones Ice. Aunque su función principal sea actuar como cliente, para los callbacks también debe poder despachar operaciones entrantes.

Conceptualmente:

```java
ObjectAdapter adapter =
        communicator.createObjectAdapterWithEndpoints(
                "ClientAdapter",
                "tcp"
        );
```

Si se utilizan conexiones bidireccionales, el adaptador puede ser incluso un adaptador sin endpoints dedicado a recibir callbacks sobre una conexión ya existente.

### 2. Registro del servant

El cliente implementa la interfaz Slice del callback con una clase como `ClientCallbackImpl` y registra esa instancia en el adaptador.

```java
ClientCallbackImpl callback =
        new ClientCallbackImpl(...);

ClientCallbackPrx callbackProxy =
        ClientCallbackPrx.uncheckedCast(
                adapter.addWithUUID(callback)
        );
```

`ClientCallbackImpl` es el **servant**, mientras que `ClientCallbackPrx` es la referencia remota que puede entregarse al servidor.

### 3. Activación del adaptador

Cuando el adaptador utiliza endpoints propios, debe activarse:

```java
adapter.activate();
```

Después de esto puede recibir y despachar invocaciones.

### 4. Entrega del proxy al servidor

Durante el login, el cliente entrega el proxy de callback junto con su nickname. El servidor lo almacena en su estructura de usuarios conectados.

```text
alice -> ClientCallbackPrx
bob   -> ClientCallbackPrx
```

Cuando ocurre un evento para Alice, el servidor invoca el proxy de Alice en lugar de esperar a que el cliente consulte repetidamente el servidor. De esta forma se evita el **polling** continuo y se obtiene una comunicación realmente asíncrona.

### 5. Cierre y limpieza

Al hacer logout o cerrar la aplicación, el servidor debe eliminar el proxy almacenado y el cliente debe cerrar su adaptador y su `Communicator`. Un proxy perteneciente a un cliente desconectado ya no representa un destino válido y una invocación posterior puede producir una excepción de red.

### Proxies directos

Un **proxy directo** contiene la identidad del objeto y los endpoints concretos necesarios para alcanzarlo, por ejemplo transporte, host y puerto.

```text
callbackId:tcp -h 192.168.1.20 -p 12050
```

El servidor puede conectarse directamente sin consultar un servicio de localización. Es una opción sencilla para una LAN o un entorno controlado.

Su desventaja es que el endpoint publicado debe ser realmente alcanzable desde el servidor. Si el cliente se encuentra detrás de NAT o firewall, una conexión entrante puede quedar bloqueada.

### Proxies indirectos

Un **proxy indirecto** no contiene directamente el endpoint final. Puede contener una identidad y un `AdapterId`:

```text
callbackId @ ClientAdapter
```

Ice utiliza un `Locator`, normalmente asociado con IceGrid, para resolver dinámicamente el endpoint real. Esto desacopla la identidad lógica del objeto de su ubicación física y resulta útil en sistemas donde los servicios cambian de host, se replican o son administrados dinámicamente.

Para un proyecto académico ejecutado en una red controlada, desplegar IceGrid únicamente para resolver callbacks puede ser innecesario. Un proxy directo es suficiente si los clientes son alcanzables.

### Conexiones bidireccionales y proxies fijos

Ice ofrece además conexiones bidireccionales. El cliente abre inicialmente la conexión hacia el servidor y el servidor reutiliza esa misma conexión para realizar callbacks. Esto evita la necesidad de abrir una segunda conexión hacia el cliente, lo cual es especialmente útil con NAT o firewalls.

En este caso el servidor puede utilizar un **proxy fijo**, asociado a la conexión existente. Si esa conexión se cierra, el proxy deja de ser utilizable.

| Tipo de proxy | Característica | Escenario típico |
|---|---|---|
| Directo | Contiene endpoints concretos | LAN o clientes directamente alcanzables |
| Indirecto | Se resuelve mediante `Locator`/`AdapterId` | IceGrid y sistemas dinámicos |
| Fijo sobre conexión bidireccional | Queda ligado a una conexión existente | Callbacks detrás de NAT/firewall |

La transparencia de Ice hace que la invocación sobre un proxy se parezca a una llamada sobre un objeto local, pero no elimina la naturaleza distribuida de la operación. El código debe seguir contemplando desconexiones, latencia y excepciones de comunicación.

---

## Conclusión

Las decisiones de diseño del proyecto responden a los requisitos de cada tipo de tráfico:

- **Ice/TCP** se utiliza para control, mensajería, transferencia de archivos y señalización porque estas operaciones requieren confiabilidad y orden.
- **UDP** se utiliza para voz porque el objetivo principal es minimizar la latencia y tolerar pérdidas ocasionales.
- Los archivos se dividen en **chunks** para mantenerse dentro de los límites de mensajes de Ice y evitar RPC excesivamente grandes.
- Las llamadas grupales usan un **Relay centralizado**, reduciendo el ancho de banda de subida y la complejidad de cada cliente.
- Los **callbacks Ice** permiten entregar eventos del servidor a los clientes de forma asíncrona sin polling constante.

En conjunto, la arquitectura separa de manera clara el tráfico confiable del tráfico de tiempo real y asigna cada tecnología al problema para el que resulta más adecuada.

## Referencias técnicas

- ZeroC Ice 3.8, `Ice.MessageSizeMax`: https://docs.zeroc.com/ice/3.8/property-reference/ice-properties
- ZeroC Ice 3.8, propiedades de `ObjectAdapter`: https://docs.zeroc.com/ice/3.8/property-reference/object-adapter-properties
- ZeroC Ice 3.8, creación de proxies: https://docs.zeroc.com/ice/3.8/runtime/invocation/creating-proxies
- ZeroC Ice 3.8, conexiones bidireccionales: https://docs.zeroc.com/ice/3.8/runtime/connection-management/bidirectional-connections
