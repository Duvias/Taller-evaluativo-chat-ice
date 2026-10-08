package chat.multimedia.client.callback;

import com.zeroc.Ice.Current;
import distributed.chat.ClientCallback;
import distributed.chat.FileChunk;
import distributed.chat.TargetType;

/**
 * Callback base del cliente.
 */
public class ClientCallbackImpl implements ClientCallback {

    @Override
    public void userConnected(String nickname, Current current) {
        System.out.println("[PRESENCIA] " + nickname + " se conectó.");
    }

    @Override
    public void userDisconnected(String nickname, Current current) {
        System.out.println("[PRESENCIA] " + nickname + " se desconectó.");
    }

    @Override
    public void receivePrivateMessage(
            String sender,
            String message,
            Current current
    ) {
        System.out.printf("[PRIVADO][%s] %s%n", sender, message);
    }

    @Override
    public void receiveRoomMessage(
            String roomName,
            String sender,
            String message,
            Current current
    ) {
        System.out.printf("[%s][%s] %s%n", roomName, sender, message);
    }

    @Override
    public void receiveFileChunk(
            TargetType targetType,
            String target,
            FileChunk chunk,
            Current current
    ) {
        // TODO Integrante 2
    }

    @Override
    public void incomingCall(
            String caller,
            String ip,
            int port,
            Current current
    ) {
        System.out.printf("[LLAMADA] %s te está llamando.%n", caller);
    }

    @Override
    public void callAccepted(
            String user,
            String ip,
            int port,
            Current current
    ) {
        System.out.printf(
                "[LLAMADA] %s aceptó. UDP remoto: %s:%d%n",
                user,
                ip,
                port
        );
    }

    @Override
    public void callRejected(String user, Current current) {
        System.out.println("[LLAMADA] " + user + " rechazó la llamada.");
    }

    @Override
    public void callEnded(String user, Current current) {
        System.out.println("[LLAMADA] Llamada finalizada con " + user + ".");
    }

    @Override
    public void groupCallStarted(
            String roomName,
            String host,
            String relayIp,
            int relayPort,
            Current current
    ) {
        System.out.printf(
                "[VOZ GRUPAL] %s inició llamada en %s. Relay %s:%d%n",
                host,
                roomName,
                relayIp,
                relayPort
        );
    }

    @Override
    public void groupCallEnded(String roomName, Current current) {
        System.out.println("[VOZ GRUPAL] Finalizó llamada en " + roomName + ".");
    }

    @Override
    public void groupCallParticipantJoined(
            String roomName,
            String nickname,
            Current current
    ) {
        System.out.printf(
                "[VOZ GRUPAL][%s] %s entró.%n",
                roomName,
                nickname
        );
    }

    @Override
    public void groupCallParticipantLeft(
            String roomName,
            String nickname,
            Current current
    ) {
        System.out.printf(
                "[VOZ GRUPAL][%s] %s salió.%n",
                roomName,
                nickname
        );
    }

    @Override
    public void systemMessage(String message, Current current) {
        System.out.println("[SISTEMA] " + message);
    }
}
