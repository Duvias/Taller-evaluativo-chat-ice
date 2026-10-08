# Distribución del equipo

## Integrante 1: Ice, usuarios y chat privado

Responsable de:

- servidor Ice;
- login/logout;
- callbacks;
- presencia;
- usuarios conectados;
- mensajes privados;
- excepciones de usuario.

Rama:

```text
feature/ice-usuarios
```

## Integrante 2: salas, archivos y CLI

Responsable de:

- creación/listado/unión/salida de salas;
- broadcast;
- aislamiento de salas;
- transferencia de archivos por chunks usando Ice;
- comandos CLI.

Rama:

```text
feature/salas-archivos
```

## Integrante 3: llamadas y voz UDP

Responsable de:

- señalización de llamadas mediante Ice;
- audio UDP 1 a 1;
- captura/reproducción con `javax.sound.sampled`;
- mute;
- finalización limpia;
- conferencia grupal mediante Relay/SFU.

Rama:

```text
feature/voz-udp
```

## Trabajo compartido

Los tres revisan:

- `Chat.ice`;
- arquitectura;
- integración;
- pruebas con varios clientes;
- README/documentación;
- sustentación.
