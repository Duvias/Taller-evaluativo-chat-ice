# Arquitectura inicial

## Separación de protocolos

### ZeroC Ice / TCP

Se utilizará para:

- login y logout;
- presencia;
- mensajería privada;
- salas y broadcast;
- transferencia de archivos por chunks;
- callbacks asíncronos;
- señalización de llamadas.

### UDP

Se utilizará exclusivamente para:

- audio 1 a 1;
- audio grupal;
- relay/SFU centralizado.

## Componentes

```text
                         ┌──────────────────────────┐
                         │         SERVIDOR         │
                         │                          │
                         │ UserManager              │
                         │ RoomManager              │
                         │ FileManager              │
                         │ VoiceManager             │
                         └────────────┬─────────────┘
                                      │
                               ZeroC Ice / TCP
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                 Cliente A         Cliente B         Cliente C
                    │                 │                 │
                    └──────────── UDP / VOZ ────────────┘
                                      │
                                Relay/SFU central
```

## Regla de dependencias

- `common` no depende de `server` ni de `client`.
- `server` depende de `common`.
- `client` depende de `common`.
- `server` y `client` no se dependen directamente entre sí.

## Contrato distribuido

El contrato compartido está en:

```text
common/src/main/slice/Chat.ice
```

El código Java generado por Slice no se escribe a mano.
