package chat.multimedia.client.callback;

import chat.multimedia.client.file.FileClient;
import chat.multimedia.client.voice.VoiceClient;
import com.zeroc.Ice.Current;
import distributed.chat.ClientCallback;
import distributed.chat.FileChunk;
import distributed.chat.TargetType;

public class ClientCallbackImpl implements ClientCallback {

    private final FileClient fileClient;
    private final VoiceClient voiceClient;

    public ClientCallbackImpl() {
        this(new FileClient(), null);
    }

    public ClientCallbackImpl(FileClient fileClient) {
        this(fileClient, null);
    }

    public ClientCallbackImpl(
            FileClient fileClient,
            VoiceClient voiceClient
    ) {
        this.fileClient = fileClient;
        this.voiceClient = voiceClient;
    }

    @Override
    public void userConnected(String nickname, Current current) {
        System.out.println(
                "[PRESENCIA] " + nickname + " se conectó."
        );
    }

    @Override
    public void userDisconnected(String nickname, Current current) {
        System.out.println(
                "[PRESENCIA] " + nickname + " se desconectó."
        );
    }

    @Override
    public void receivePrivateMessage(
            String sender,
            String message,
            Current current
    ) {
        System.out.printf(
                "[PRIVADO][%s] %s%n",
                sender,
                message
        );
    }

    @Override
    public void receiveRoomMessage(
            String roomName,
            String sender,
            String message,
            Current current
    ) {
        System.out.printf(
                "[%s][%s] %s%n",
                roomName,
                sender,
                message
        );
    }

    @Override
    public void receiveFileChunk(
            TargetType targetType,
            String target,
            FileChunk chunk,
            Current current
    ) {
        fileClient.receiveChunk(targetType, target, chunk);
    }

    @Override
    public void incomingCall(
            String caller,
            String ip,
            int port,
            Current current
    ) {
        if (voiceClient != null) {
            voiceClient.onIncomingCall(caller, ip, port);
        }

        System.out.printf(
                "[LLAMADA] %s te está llamando. Usa /accept o /reject.%n",
                caller
        );
    }

    @Override
    public void callAccepted(
            String user,
            String ip,
            int port,
            Current current
    ) {
        if (voiceClient != null) {
            voiceClient.onCallAccepted(user, ip, port);
        }

        System.out.println(
                "[LLAMADA] " + user + " aceptó la llamada."
        );
    }

    @Override
    public void callRejected(String user, Current current) {
        if (voiceClient != null) {
            voiceClient.onCallRejected(user);
        }

        System.out.println(
                "[LLAMADA] " + user + " rechazó la llamada."
        );
    }

    @Override
    public void callEnded(String user, Current current) {
        if (voiceClient != null) {
            voiceClient.onCallEnded(user);
        }

        System.out.println(
                "[LLAMADA] Llamada finalizada con " + user + "."
        );
    }

    @Override
    public void groupCallStarted(
            String roomName,
            String host,
            String relayIp,
            int relayPort,
            Current current
    ) {
        if (voiceClient != null) {
            voiceClient.onGroupCallStarted(
                    roomName,
                    host,
                    relayIp,
                    relayPort
            );
        }

        System.out.printf(
                "[VOZ GRUPAL] %s inició llamada en %s. Usa /gjoin %s.%n",
                host,
                roomName,
                roomName
        );
    }

    @Override
    public void groupCallEnded(String roomName, Current current) {
        if (voiceClient != null) {
            voiceClient.onGroupCallEnded(roomName);
        }

        System.out.println(
                "[VOZ GRUPAL] Finalizó llamada en " + roomName + "."
        );
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
