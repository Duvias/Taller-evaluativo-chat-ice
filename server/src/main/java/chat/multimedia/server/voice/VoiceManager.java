package chat.multimedia.server.voice;

import chat.multimedia.common.NetworkConfig;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceManager implements AutoCloseable {

    private final Map<String, CallSession> callsByUser =
            new ConcurrentHashMap<>();
    private final Map<String, GroupCallSession> groupCalls =
            new ConcurrentHashMap<>();
    private final Map<String, String> groupRoomByUser =
            new ConcurrentHashMap<>();
    private final VoiceRelayServer relayServer;

    public VoiceManager() {
        this(new VoiceRelayServer(NetworkConfig.UDP_RELAY_PORT));
    }

    VoiceManager(VoiceRelayServer relayServer) {
        this.relayServer = relayServer;
    }

    public synchronized CallSession startCall(
            String caller,
            String target,
            String callerIp,
            int callerPort
    ) {
        if (caller.equals(target)) {
            throw new IllegalStateException(
                    "A user cannot call themselves."
            );
        }

        requireAvailableForDirectCall(caller);
        requireAvailableForDirectCall(target);

        CallSession session = new CallSession(
                caller,
                target,
                callerIp,
                callerPort
        );

        callsByUser.put(caller, session);
        callsByUser.put(target, session);

        return session;
    }

    public synchronized CallSession acceptCall(
            String caller,
            String target,
            String targetIp,
            int targetPort
    ) {
        CallSession session = callsByUser.get(caller);

        validateCall(session, caller, target);

        if (session.getState() != CallState.RINGING) {
            throw new IllegalStateException(
                    "Call is not waiting for acceptance."
            );
        }

        session.accept(targetIp, targetPort);

        return session;
    }

    public synchronized void rejectCall(
            String caller,
            String target
    ) {
        CallSession session = callsByUser.get(caller);

        validateCall(session, caller, target);
        removeSession(session);
    }

    public synchronized void endCall(
            String user,
            String otherUser
    ) {
        CallSession session = callsByUser.get(user);

        if (session == null) {
            throw new IllegalStateException(
                    user + " is not in a call."
            );
        }

        if (!session.involves(otherUser)) {
            throw new IllegalStateException(
                    "Users are not in the same call."
            );
        }

        removeSession(session);
    }

    public synchronized GroupCallSession startGroupCall(
            String host,
            String roomName
    ) {
        if (groupCalls.containsKey(roomName)) {
            throw new IllegalStateException(
                    "A group call is already active in " + roomName + "."
            );
        }

        requireAvailableForGroupCall(host);

        try {
            relayServer.start();
        } catch (SocketException exception) {
            throw new IllegalStateException(
                    "Could not start the UDP voice relay.",
                    exception
            );
        }

        GroupCallSession session = new GroupCallSession(
                roomName,
                host
        );

        groupCalls.put(roomName, session);
        groupRoomByUser.put(host, roomName);

        return session;
    }

    public synchronized GroupCallSession joinGroupCall(
            String nickname,
            String roomName,
            String ip,
            int port
    ) {
        GroupCallSession session = requireGroupCall(roomName);

        String currentRoom = groupRoomByUser.get(nickname);

        if (currentRoom != null && !currentRoom.equals(roomName)) {
            throw new IllegalStateException(
                    nickname + " is already in another group call."
            );
        }

        if (callsByUser.containsKey(nickname)) {
            throw new IllegalStateException(
                    nickname + " is already in a direct call."
            );
        }

        InetSocketAddress endpoint = new InetSocketAddress(ip, port);

        session.addParticipant(nickname, endpoint);
        groupRoomByUser.put(nickname, roomName);
        relayServer.register(roomName, nickname, ip, port);

        return session;
    }

    public synchronized void leaveGroupCall(
            String nickname,
            String roomName
    ) {
        GroupCallSession session = requireGroupCall(roomName);

        if (session.getHost().equals(nickname)) {
            throw new IllegalStateException(
                    "The host must end the group call."
            );
        }

        if (!session.hasParticipant(nickname)) {
            throw new IllegalStateException(
                    nickname + " is not in the group call."
            );
        }

        session.removeParticipant(nickname);
        groupRoomByUser.remove(nickname, roomName);
        relayServer.unregister(roomName, nickname);
    }

    public synchronized Set<String> endGroupCall(
            String host,
            String roomName
    ) {
        GroupCallSession session = requireGroupCall(roomName);

        if (!session.getHost().equals(host)) {
            throw new IllegalStateException(
                    "Only the host can end the group call."
            );
        }

        Set<String> participants = session.participants();

        for (String nickname : participants) {
            groupRoomByUser.remove(nickname, roomName);
        }

        groupCalls.remove(roomName);
        relayServer.removeRoom(roomName);

        return participants;
    }

    public CallSession getCall(String nickname) {
        return callsByUser.get(nickname);
    }

    public GroupCallSession getGroupCall(String roomName) {
        return groupCalls.get(roomName);
    }

    public Set<String> getGroupParticipants(String roomName) {
        GroupCallSession session = groupCalls.get(roomName);

        if (session == null) {
            return Set.of();
        }

        return session.participants();
    }

    public int getRelayPort() {
        return relayServer.getLocalPort();
    }

    public boolean isInCall(String nickname) {
        return callsByUser.containsKey(nickname);
    }

    public boolean isInGroupCall(String nickname) {
        return groupRoomByUser.containsKey(nickname);
    }

    @Override
    public synchronized void close() {
        callsByUser.clear();
        groupCalls.clear();
        groupRoomByUser.clear();
        relayServer.close();
    }

    private void requireAvailableForDirectCall(String nickname) {
        if (callsByUser.containsKey(nickname)) {
            throw new IllegalStateException(
                    nickname + " is already in a call."
            );
        }

        if (groupRoomByUser.containsKey(nickname)) {
            throw new IllegalStateException(
                    nickname + " is already in a group call."
            );
        }
    }

    private void requireAvailableForGroupCall(String nickname) {
        if (callsByUser.containsKey(nickname)) {
            throw new IllegalStateException(
                    nickname + " is already in a direct call."
            );
        }

        if (groupRoomByUser.containsKey(nickname)) {
            throw new IllegalStateException(
                    nickname + " is already in a group call."
            );
        }
    }

    private GroupCallSession requireGroupCall(String roomName) {
        GroupCallSession session = groupCalls.get(roomName);

        if (session == null) {
            throw new IllegalStateException(
                    "There is no active group call in " + roomName + "."
            );
        }

        return session;
    }

    private void validateCall(
            CallSession session,
            String caller,
            String target
    ) {
        if (session == null) {
            throw new IllegalStateException(
                    "Call does not exist."
            );
        }

        if (!session.getCaller().equals(caller)
                || !session.getTarget().equals(target)) {
            throw new IllegalStateException(
                    "Invalid call participants."
            );
        }
    }

    private void removeSession(CallSession session) {
        callsByUser.remove(session.getCaller());
        callsByUser.remove(session.getTarget());
    }
}
