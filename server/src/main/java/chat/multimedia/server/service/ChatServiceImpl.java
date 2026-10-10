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
import com.zeroc.Ice.LocalException;
import java.util.Set;

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

    @Override
    public void login(
            String nickname,
            ClientCallbackPrx callback,
            Current current
    ) throws UserAlreadyExists {
        // TODO Integrante 1
    }

    @Override
    public void logout(String nickname, Current current) {
        // TODO Integrante 1: después de retirar el callback del usuario, invocar
        // roomManager.removeUserFromAllRooms(nickname) para limpiar membresías.
    }

    @Override
    public String[] getConnectedUsers(Current current) {
        // TODO Integrante 1
        return new String[0];
    }

    @Override
    public void sendPrivateMessage(
            String sender,
            String receiver,
            String message,
            Current current
    ) throws UserNotFound {
        // TODO Integrante 1
    }

    @Override
    public void createRoom(
            String roomName,
            Current current
    ) throws RoomAlreadyExists {
        if (!roomManager.createRoom(roomName)) {
            throw new RoomAlreadyExists(roomName);
        }
    }

    @Override
    public void joinRoom(
            String nickname,
            String roomName,
            Current current
    ) throws RoomNotFound {
        if (!roomManager.joinRoom(nickname, roomName)) {
            throw new RoomNotFound(roomName);
        }
    }

    @Override
    public void leaveRoom(
            String nickname,
            String roomName,
            Current current
    ) throws RoomNotFound, NotInRoom {
        if (!roomManager.roomExists(roomName)) {
            throw new RoomNotFound(roomName);
        }
        if (!roomManager.isMember(nickname, roomName)) {
            throw new NotInRoom(nickname, roomName);
        }
        roomManager.leaveRoom(nickname, roomName);
    }

    @Override
    public String[] listRooms(Current current) {
        return roomManager.listRooms();
    }

    @Override
    public void sendRoomMessage(
            String sender,
            String roomName,
            String message,
            Current current
    ) throws RoomNotFound, NotInRoom {
        if (!roomManager.roomExists(roomName)) {
            throw new RoomNotFound(roomName);
        }
        if (!roomManager.isMember(sender, roomName)) {
            throw new NotInRoom(sender, roomName);
        }
        Set<String> members = roomManager.membersOf(roomName);
        for (String member : members) {
            if (member.equals(sender)) {
                continue; // Do not echo a room message to its sender.
            }
            ClientCallbackPrx callback = userManager.connectedUsers().get(member);
            if (callback != null) {
                try {
                    callback.receiveRoomMessage(roomName, sender, message);
                } catch (LocalException ignored) {
                    // A failed callback must not prevent delivery to other room members.
                }
            }
        }
    }

    @Override
    public void sendFileChunk(
            String sender,
            TargetType targetType,
            String target,
            FileChunk chunk,
            Current current
    ) throws UserNotFound, RoomNotFound, NotInRoom {
        fileManager.validateChunk(chunk);
        if (sender == null || !sender.equals(chunk.sender)) {
            throw new IllegalArgumentException("El remitente del fragmento no coincide con el usuario autenticado.");
        }

        if (targetType == TargetType.User) {
            ClientCallbackPrx senderCallback = userManager.connectedUsers().get(sender);
            if (senderCallback == null) {
                throw new UserNotFound(sender);
            }
            ClientCallbackPrx receiverCallback = userManager.connectedUsers().get(target);
            if (receiverCallback == null) {
                throw new UserNotFound(target);
            }
            try {
                receiverCallback.receiveFileChunk(targetType, target, chunk);
            } catch (LocalException exception) {
                throw new UserNotFound(target);
            }
            return;
        }

        if (targetType == TargetType.Room) {
            if (!roomManager.roomExists(target)) {
                throw new RoomNotFound(target);
            }
            if (!roomManager.isMember(sender, target)) {
                throw new NotInRoom(sender, target);
            }
            for (String member : roomManager.membersOf(target)) {
                if (member.equals(sender)) {
                    continue;
                }
                ClientCallbackPrx callback = userManager.connectedUsers().get(member);
                if (callback != null) {
                    try {
                        callback.receiveFileChunk(targetType, target, chunk);
                    } catch (LocalException ignored) {
                        // Continue forwarding to other members.
                    }
                }
            }
            return;
        }
        throw new IllegalArgumentException("Tipo de destino de archivo desconocido.");
    }

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
