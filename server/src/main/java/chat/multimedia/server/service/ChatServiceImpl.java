
package chat.multimedia.server.service;

import chat.multimedia.common.NetworkConfig;
import chat.multimedia.server.file.FileManager;
import chat.multimedia.server.room.RoomManager;
import chat.multimedia.server.user.UserManager;
import chat.multimedia.server.voice.GroupCallSession;
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

import java.util.Set;

public class ChatServiceImpl implements ChatService {

    private final UserManager userManager;
    private final RoomManager roomManager;
    @SuppressWarnings("unused")
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
                continue;
            }
            ClientCallbackPrx callback = userManager.connectedUsers().get(member);
            if (callback != null) {
                try {
                    callback.receiveRoomMessage(roomName, sender, message);
                } catch (LocalException ignored) {
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
        Set<String> roomMembers = requireRoom(roomName);
        requireRoomMember(host, roomName, roomMembers);
        requireConnectedGroupUser(host);

        try {
            voiceManager.startGroupCall(host, roomName);
        } catch (IllegalStateException exception) {
            throw new CallError(exception.getMessage());
        }

        for (String nickname : roomMembers) {
            ClientCallbackPrx callback =
                    userManager.connectedUsers().get(nickname);

            if (callback == null) {
                continue;
            }

            try {
                callback.groupCallStarted(
                        roomName,
                        host,
                        NetworkConfig.ICE_HOST,
                        voiceManager.getRelayPort()
                );
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Override
    public void joinGroupCall(
            String nickname,
            String roomName,
            String ip,
            int port,
            Current current
    ) throws RoomNotFound, NotInRoom, CallError {
        Set<String> roomMembers = requireRoom(roomName);
        requireRoomMember(nickname, roomName, roomMembers);
        ClientCallbackPrx joiningCallback =
                requireConnectedGroupUser(nickname);

        try {
            voiceManager.joinGroupCall(
                    nickname,
                    roomName,
                    ip,
                    port
            );
        } catch (IllegalStateException exception) {
            throw new CallError(exception.getMessage());
        }

        GroupCallSession groupCall =
                voiceManager.getGroupCall(roomName);

        if (groupCall == null) {
            throw new CallError(
                    "There is no active group call in "
                            + roomName
                            + "."
            );
        }

        try {
            joiningCallback.groupCallStarted(
                    roomName,
                    groupCall.getHost(),
                    NetworkConfig.ICE_HOST,
                    voiceManager.getRelayPort()
            );
        } catch (RuntimeException exception) {
            throw new CallError(
                    "Could not deliver the group relay endpoint."
            );
        }

        for (String participant :
                voiceManager.getGroupParticipants(roomName)) {
            if (participant.equals(nickname)) {
                continue;
            }

            ClientCallbackPrx callback =
                    userManager.connectedUsers().get(participant);

            if (callback == null) {
                continue;
            }

            try {
                callback.groupCallParticipantJoined(
                        roomName,
                        nickname
                );
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Override
    public void leaveGroupCall(
            String nickname,
            String roomName,
            Current current
    ) throws RoomNotFound, CallError {
        requireRoom(roomName);

        try {
            voiceManager.leaveGroupCall(
                    nickname,
                    roomName
            );
        } catch (IllegalStateException exception) {
            throw new CallError(exception.getMessage());
        }

        for (String participant :
                voiceManager.getGroupParticipants(roomName)) {
            ClientCallbackPrx callback =
                    userManager.connectedUsers().get(participant);

            if (callback == null) {
                continue;
            }

            try {
                callback.groupCallParticipantLeft(
                        roomName,
                        nickname
                );
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Override
    public void endGroupCall(
            String host,
            String roomName,
            Current current
    ) throws RoomNotFound, CallError {
        requireRoom(roomName);

        Set<String> participants =
                voiceManager.getGroupParticipants(roomName);

        try {
            voiceManager.endGroupCall(
                    host,
                    roomName
            );
        } catch (IllegalStateException exception) {
            throw new CallError(exception.getMessage());
        }

        for (String participant : participants) {
            ClientCallbackPrx callback =
                    userManager.connectedUsers().get(participant);

            if (callback == null) {
                continue;
            }

            try {
                callback.groupCallEnded(roomName);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private ClientCallbackPrx requireConnectedGroupUser(
            String nickname
    ) throws CallError {
        ClientCallbackPrx callback =
                userManager.connectedUsers().get(nickname);

        if (callback == null) {
            throw new CallError(
                    nickname + " is not connected."
            );
        }

        return callback;
    }

    private Set<String> requireRoom(
            String roomName
    ) throws RoomNotFound {
        Set<String> members = roomManager.rooms().get(roomName);

        if (members == null) {
            throw new RoomNotFound(roomName);
        }

        return members;
    }

    private void requireRoomMember(
            String nickname,
            String roomName,
            Set<String> members
    ) throws NotInRoom {
        if (!members.contains(nickname)) {
            throw new NotInRoom(
                    nickname,
                    roomName
            );
        }
    }
}
