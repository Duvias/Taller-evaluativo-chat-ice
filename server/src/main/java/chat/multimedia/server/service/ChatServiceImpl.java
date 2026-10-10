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
        this(
                new UserManager(),
                new RoomManager(),
                new FileManager(),
                new VoiceManager()
        );
    }

    ChatServiceImpl(
            UserManager userManager,
            RoomManager roomManager,
            FileManager fileManager,
            VoiceManager voiceManager
    ) {
        this.userManager = userManager;
        this.roomManager = roomManager;
        this.fileManager = fileManager;
        this.voiceManager = voiceManager;
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
        requireConnectedUser(caller);
        ClientCallbackPrx targetCallback = requireConnectedUser(target);

        try {
            voiceManager.startCall(caller, target, ip, port);
        } catch (IllegalStateException exception) {
            throw new CallError(exception.getMessage());
        }

        try {
            targetCallback.incomingCall(caller, ip, port);
        } catch (RuntimeException exception) {
            rollbackPendingCall(caller, target);
            throw new CallError(
                    "Could not notify " + target + " about the incoming call."
            );
        }
    }

    @Override
    public void acceptCall(
            String caller,
            String target,
            String ip,
            int port,
            Current current
    ) throws UserNotFound, CallError {
        ClientCallbackPrx callerCallback = requireConnectedUser(caller);
        requireConnectedUser(target);

        try {
            voiceManager.acceptCall(caller, target, ip, port);
        } catch (IllegalStateException exception) {
            throw new CallError(exception.getMessage());
        }

        try {
            callerCallback.callAccepted(target, ip, port);
        } catch (RuntimeException exception) {
            rollbackActiveCall(target, caller);
            throw new CallError(
                    "Could not notify " + caller + " that the call was accepted."
            );
        }
    }

    @Override
    public void rejectCall(
            String caller,
            String target,
            Current current
    ) throws UserNotFound, CallError {
        ClientCallbackPrx callerCallback = requireConnectedUser(caller);
        requireConnectedUser(target);

        try {
            voiceManager.rejectCall(caller, target);
        } catch (IllegalStateException exception) {
            throw new CallError(exception.getMessage());
        }

        try {
            callerCallback.callRejected(target);
        } catch (RuntimeException exception) {
            throw new CallError(
                    "The call was rejected, but " + caller + " could not be notified."
            );
        }
    }

    @Override
    public void endCall(
            String user,
            String target,
            Current current
    ) throws UserNotFound, CallError {
        requireConnectedUser(user);
        ClientCallbackPrx targetCallback = requireConnectedUser(target);

        try {
            voiceManager.endCall(user, target);
        } catch (IllegalStateException exception) {
            throw new CallError(exception.getMessage());
        }

        try {
            targetCallback.callEnded(user);
        } catch (RuntimeException exception) {
            throw new CallError(
                    "The call ended, but " + target + " could not be notified."
            );
        }
    }

    private ClientCallbackPrx requireConnectedUser(
            String nickname
    ) throws UserNotFound {
        ClientCallbackPrx callback =
                userManager.connectedUsers().get(nickname);

        if (callback == null) {
            throw new UserNotFound(nickname);
        }

        return callback;
    }

    private void rollbackPendingCall(String caller, String target) {
        try {
            voiceManager.rejectCall(caller, target);
        } catch (IllegalStateException ignored) {
        }
    }

    private void rollbackActiveCall(String user, String target) {
        try {
            voiceManager.endCall(user, target);
        } catch (IllegalStateException ignored) {
        }
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
