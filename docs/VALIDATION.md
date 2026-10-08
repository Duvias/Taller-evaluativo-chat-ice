# Validación realizada

Se revisó el proyecto recibido y se limpiaron los problemas de infraestructura que estaban mezclados con la rama de voz.

## Verificaciones realizadas sobre esta entrega

- El Gradle Wrapper está completo: `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` y `gradle-wrapper.properties`.
- `.gitignore` permite versionar explícitamente `gradle-wrapper.jar`.
- Los tres módulos tienen configuración coherente de JUnit Platform para Gradle 9.
- La compilación Java está fijada a `--release 17` y no obliga a encontrar una instalación exacta de JDK 17 cuando se ejecuta Gradle con un JDK más nuevo.
- `Client.java` y `Server.java` usan la API correcta de Ice 3.8 para Java: `new Communicator(args)` dentro de `try-with-resources`.
- Las fuentes principales de `client` y `server` fueron compiladas manualmente con `javac --release 17` contra Ice 3.8.3 y las clases Slice generadas del proyecto recibido.
- El servidor corregido fue iniciado y registró el mensaje `Chat server listening on TCP port 10000.`.
- El cliente corregido pudo crear el proxy `chatService:tcp -h 127.0.0.1 -p 10000`.

## Limitación del entorno usado para revisar

No fue posible ejecutar aquí el comando completo `./gradlew clean build` porque este entorno no tiene resolución DNS hacia `services.gradle.org` y el Wrapper necesita descargar la distribución de Gradle la primera vez.

En un equipo con Internet, la comprobación final es:

```powershell
.\gradlew.bat clean build
```

El proyecto recibido ya había generado artefactos con Ice 3.8.3; estos se usaron únicamente para validar la compilación de las fuentes principales y no se incluyen en el proyecto limpio.
