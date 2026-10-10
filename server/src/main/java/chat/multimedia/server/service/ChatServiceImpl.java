
package chat.multimedia.server.service;

import chat.multimedia.server.file.FileManager;
import chat.multimedia.server.room.RoomManager;
import chat.multimedia.server.user.UserManager;
import chat.multimedia.server.voice.VoiceManager;
import com.zeroc.Ice.Current;
import distributed.chat.CallError;
import distributed.chat.ChatService;
import distributed.chat.ClientCallbackPrx;
import distributed.chat.FileChunk;
import distributed.chat.NotInRoom;
import distributed.chat.RoomAlreadyExists;
import distributed.chat.RoomNotFound;
import distributed.chat.TargetType;
import distributed.chat.UserAlreadyExists;
import distributed.chat.UserNotFound;

public class ChatServiceImpl implements ChatService {

    private final UserManager userManager;
    private final RoomManager roomManager;
    private final FileManager fileManager;
    private final VoiceManager voiceManager;

    public ChatServiceImpl() {
        this.userManager = new UserManager();
        this.roomManager = new RoomManager();
        this.fileManager = new FileManager();
        this.voiceManager = new VoiceManager();
    }

    // =========================
    // INTEGRANTE 1: USUARIOS
    // =========================

    @Override
    public void login(
            String nickname,
            ClientCallbackPrx callback,
            Current current
    ) throws UserAlreadyExists {

        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException(
                    "El nickname no puede estar vacío."
            );
        }

        if (callback == null) {
            throw new IllegalArgumentException(
                    "El callback no puede ser null."
            );
        }

        nickname = nickname.trim();

        if (!userManager.register(nickname, callback)) {
            throw new UserAlreadyExists(nickname);
        }

        for (var entry : userManager.connectedUsers().entrySet()) {
            if (!entry.getKey().equals(nickname)) {
                try {
                    entry.getValue().userConnected(nickname);
                } catch (com.zeroc.Ice.LocalException exception) {
                    System.err.println(
                            "No se pudo notificar a " + entry.getKey()
                    );
                }
            }
        }

        System.out.println("[LOGIN] Usuario conectado: " + nickname);
    }

    @Override
    public void logout(String nickname, Current current) {

        ClientCallbackPrx removed = userManager.remove(nickname);

        if (removed == null) {
            return;
        }

        for (var entry : userManager.connectedUsers().entrySet()) {
            try {
                entry.getValue().userDisconnected(nickname);
            } catch (com.zeroc.Ice.LocalException exception) {
                System.err.println(
                        "No se pudo notificar la desconexión a "
                                + entry.getKey()
                );
            }
        }

        System.out.println("[LOGOUT] Usuario desconectado: " + nickname);
    }

    @Override
    public String[] getConnectedUsers(Current current) {
        return userManager.getNicknames().toArray(new String[0]);
    }

    @Override
    public void sendPrivateMessage(
            String sender,
            String receiver,
            String message,
            Current current
    ) throws UserNotFound {

        if (sender == null || !userManager.isConnected(sender)) {
            throw new UserNotFound(sender == null ? "" : sender);
        }

        ClientCallbackPrx receiverCallback = userManager.find(receiver);

        if (receiverCallback == null) {
            throw new UserNotFound(receiver == null ? "" : receiver);
        }

        try {
            receiverCallback.receivePrivateMessage(sender, message);
        } catch (com.zeroc.Ice.LocalException exception) {
            System.err.println(
                    "Error enviando mensaje privado a " + receiver
            );
            throw exception;
        }
    }

    // =========================
    // INTEGRANTE 2: SALAS Y ARCHIVOS
    // =========================

    @Override
    public void createRoom(
            String roomName,
            Current current
    ) throws RoomAlreadyExists {
        // TODO Integrante 2
    }

    @Override
    public void joinRoom(
            String nickname,
            String roomName,
            Current current
    ) throws RoomNotFound {
        // TODO Integrante 2
    }

    @Override
    public void leaveRoom(
            String nickname,
            String roomName,
            Current current
    ) throws RoomNotFound, NotInRoom {
        // TODO Integrante 2
    }

    @Override
    public String[] listRooms(Current current) {
        // TODO Integrante 2
        return new String[0];
    }

    @Override
    public void sendRoomMessage(
            String sender,
            String roomName,
            String message,
            Current current
    ) throws RoomNotFound, NotInRoom {
        // TODO Integrante 2
    }

    @Override
    public void sendFileChunk(
            String sender,
            TargetType targetType,
            String target,
            FileChunk chunk,
            Current current
    ) throws UserNotFound, RoomNotFound, NotInRoom {
        // TODO Integrante 2
    }

    // =========================
    // INTEGRANTE 3: LLAMADAS DE VOZ
    // =========================

    @Override
    public void startCall(
            String caller,
            String target,
            String ip,
            int port,
            Current current
    ) throws UserNotFound, CallError {
        // TODO Integrante 3
    }

    @Override
    public void acceptCall(
            String caller,
            String target,
            String ip,
            int port,
            Current current
    ) throws UserNotFound, CallError {
        // TODO Integrante 3
    }

    @Override
    public void rejectCall(
            String caller,
            String target,
            Current current
    ) throws UserNotFound, CallError {
        // TODO Integrante 3
    }

    @Override
    public void endCall(
            String user,
            String target,
            Current current
    ) throws UserNotFound, CallError {
        // TODO Integrante 3
    }

    @Override
    public void startGroupCall(
            String host,
            String roomName,
            Current current
    ) throws RoomNotFound, NotInRoom, CallError {
        // TODO Integrante 3
    }

    @Override
    public void joinGroupCall(
            String nickname,
            String roomName,
            String ip,
            int port,
            Current current
    ) throws RoomNotFound, NotInRoom, CallError {
        // TODO Integrante 3
    }

    @Override
    public void leaveGroupCall(
            String nickname,
            String roomName,
            Current current
    ) throws RoomNotFound, CallError {
        // TODO Integrante 3
    }

    @Override
    public void endGroupCall(
            String host,
            String roomName,
            Current current
    ) throws RoomNotFound, CallError {
        // TODO Integrante 3
    }
}
