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
        // TODO Integrante 1
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
