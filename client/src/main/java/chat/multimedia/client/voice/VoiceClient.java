package chat.multimedia.client.voice;

import chat.multimedia.common.NetworkConfig;
import distributed.chat.CallError;
import distributed.chat.ChatServicePrx;
import distributed.chat.NotInRoom;
import distributed.chat.RoomNotFound;
import distributed.chat.UserNotFound;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Objects;

public final class VoiceClient implements AutoCloseable {

    private enum State {
        IDLE,
        OUTGOING,
        INCOMING,
        ACTIVE,
        GROUP
    }

    private final ChatServicePrx chatService;

    private String nickname;
    private String peer;
    private String peerIp;
    private int peerPort = -1;
    private String groupRoom;
    private String announcedGroupRoom;
    private String relayIp;
    private int relayPort = -1;
    private boolean groupHost;
    private boolean muted;
    private State state = State.IDLE;
    private VoiceMedia media;

    public VoiceClient(ChatServicePrx chatService) {
        this.chatService = Objects.requireNonNull(chatService, "chatService");
    }

    public synchronized void setLocalNickname(String nickname) {
        if (state != State.IDLE) {
            throw new IllegalStateException("No puedes cambiar de usuario durante una llamada.");
        }
        this.nickname = requireText(nickname, "nickname");
    }

    public synchronized void startCall(String target)
            throws UserNotFound, CallError {
        requireIdle();
        String localNickname = requireNickname();
        String remoteUser = requireText(target, "usuario");
        VoiceMedia newMedia = createMedia();

        try {
            chatService.startCall(
                    localNickname,
                    remoteUser,
                    localIp(),
                    newMedia.getLocalPort()
            );
            media = newMedia;
            peer = remoteUser;
            state = State.OUTGOING;
        } catch (UserNotFound | CallError | RuntimeException exception) {
            newMedia.close();
            throw exception;
        }
    }

    public synchronized void acceptIncomingCall()
            throws UserNotFound, CallError {
        requireState(State.INCOMING, "No hay una llamada entrante pendiente.");
        VoiceMedia newMedia = createMedia();

        try {
            chatService.acceptCall(
                    peer,
                    requireNickname(),
                    localIp(),
                    newMedia.getLocalPort()
            );
            newMedia.start(peerIp, peerPort);
            media = newMedia;
            state = State.ACTIVE;
        } catch (UserNotFound | CallError | RuntimeException exception) {
            newMedia.close();
            throw exception;
        } catch (IOException | LineUnavailableException exception) {
            newMedia.close();
            throw new IllegalStateException(
                    "No se pudo iniciar el audio de la llamada.",
                    exception
            );
        }
    }

    public synchronized void rejectIncomingCall()
            throws UserNotFound, CallError {
        requireState(State.INCOMING, "No hay una llamada entrante pendiente.");
        chatService.rejectCall(peer, requireNickname());
        resetCall();
    }

    public synchronized void hangup()
            throws UserNotFound, CallError, RoomNotFound {
        if (state == State.IDLE) {
            throw new IllegalStateException("No hay una llamada activa.");
        }

        if (state == State.GROUP) {
            if (groupHost) {
                chatService.endGroupCall(requireNickname(), groupRoom);
            } else {
                chatService.leaveGroupCall(requireNickname(), groupRoom);
            }
            resetCall();
            return;
        }

        chatService.endCall(requireNickname(), peer);
        resetCall();
    }

    public synchronized boolean toggleMute() {
        if (state != State.ACTIVE && state != State.GROUP) {
            throw new IllegalStateException("El mute solo funciona durante una llamada activa.");
        }

        muted = !muted;
        if (media != null) {
            media.setMuted(muted);
        }
        return muted;
    }

    public synchronized void startGroupCall(String roomName)
            throws RoomNotFound, NotInRoom, CallError {
        requireIdle();
        String room = requireText(roomName, "sala");
        VoiceMedia newMedia = createMedia();

        try {
            chatService.startGroupCall(requireNickname(), room);
            chatService.joinGroupCall(
                    requireNickname(),
                    room,
                    localIp(),
                    newMedia.getLocalPort()
            );
            String targetIp = relayFor(room);
            int targetPort = relayPortFor(room);
            newMedia.start(targetIp, targetPort);
            media = newMedia;
            groupRoom = room;
            groupHost = true;
            state = State.GROUP;
        } catch (RoomNotFound | NotInRoom | CallError | RuntimeException exception) {
            newMedia.close();
            throw exception;
        } catch (IOException | LineUnavailableException exception) {
            newMedia.close();
            throw new IllegalStateException(
                    "No se pudo iniciar el audio grupal.",
                    exception
            );
        }
    }

    public synchronized void joinGroupCall(String roomName)
            throws RoomNotFound, NotInRoom, CallError {
        requireIdle();
        String room = requireText(roomName, "sala");
        VoiceMedia newMedia = createMedia();

        try {
            chatService.joinGroupCall(
                    requireNickname(),
                    room,
                    localIp(),
                    newMedia.getLocalPort()
            );
            String targetIp = relayFor(room);
            int targetPort = relayPortFor(room);
            newMedia.start(targetIp, targetPort);
            media = newMedia;
            groupRoom = room;
            groupHost = false;
            state = State.GROUP;
        } catch (RoomNotFound | NotInRoom | CallError | RuntimeException exception) {
            newMedia.close();
            throw exception;
        } catch (IOException | LineUnavailableException exception) {
            newMedia.close();
            throw new IllegalStateException(
                    "No se pudo iniciar el audio grupal.",
                    exception
            );
        }
    }

    public synchronized void leaveGroupCall()
            throws RoomNotFound, CallError {
        requireState(State.GROUP, "No estás en una llamada grupal.");
        chatService.leaveGroupCall(requireNickname(), groupRoom);
        resetCall();
    }

    public synchronized void endGroupCall()
            throws RoomNotFound, CallError {
        requireState(State.GROUP, "No estás en una llamada grupal.");
        if (!groupHost) {
            throw new IllegalStateException("Solo el anfitrión puede finalizar la llamada grupal.");
        }
        chatService.endGroupCall(requireNickname(), groupRoom);
        resetCall();
    }

    public synchronized void onIncomingCall(
            String caller,
            String ip,
            int port
    ) {
        if (state != State.IDLE) {
            return;
        }

        peer = caller;
        peerIp = ip;
        peerPort = port;
        state = State.INCOMING;
    }

    public synchronized void onCallAccepted(
            String user,
            String ip,
            int port
    ) {
        if (state != State.OUTGOING || !Objects.equals(peer, user)) {
            return;
        }

        peerIp = ip;
        peerPort = port;

        try {
            media.start(ip, port);
            state = State.ACTIVE;
        } catch (IOException | LineUnavailableException exception) {
            resetCall();
            throw new IllegalStateException(
                    "No se pudo iniciar el audio de la llamada.",
                    exception
            );
        }
    }

    public synchronized void onCallRejected(String user) {
        if (state == State.OUTGOING && Objects.equals(peer, user)) {
            resetCall();
        }
    }

    public synchronized void onCallEnded(String user) {
        if (Objects.equals(peer, user)) {
            resetCall();
        }
    }

    public synchronized void onGroupCallStarted(
            String roomName,
            String host,
            String relayIp,
            int relayPort
    ) {
        announcedGroupRoom = roomName;
        this.relayIp = relayIp;
        this.relayPort = relayPort;
    }

    public synchronized void onGroupCallEnded(String roomName) {
        if (state == State.GROUP && Objects.equals(groupRoom, roomName)) {
            resetCall();
        }
        if (Objects.equals(announcedGroupRoom, roomName)) {
            announcedGroupRoom = null;
            relayIp = null;
            relayPort = -1;
        }
    }

    public synchronized void logoutCleanup() {
        try {
            if (state == State.GROUP) {
                if (groupHost) {
                    chatService.endGroupCall(requireNickname(), groupRoom);
                } else {
                    chatService.leaveGroupCall(requireNickname(), groupRoom);
                }
            } else if (state != State.IDLE && peer != null) {
                chatService.endCall(requireNickname(), peer);
            }
        } catch (Exception ignored) {
        } finally {
            resetCall();
            nickname = null;
        }
    }

    @Override
    public synchronized void close() {
        resetCall();
        nickname = null;
    }

    private VoiceMedia createMedia() {
        try {
            return new VoiceMedia();
        } catch (SocketException exception) {
            throw new IllegalStateException(
                    "No se pudo abrir el socket UDP de voz.",
                    exception
            );
        }
    }

    private String relayFor(String roomName) {
        if (Objects.equals(roomName, announcedGroupRoom)
                && relayIp != null
                && !relayIp.isBlank()) {
            return relayIp;
        }
        return NetworkConfig.UDP_RELAY_HOST;
    }

    private int relayPortFor(String roomName) {
        if (Objects.equals(roomName, announcedGroupRoom)
                && relayPort > 0) {
            return relayPort;
        }
        return NetworkConfig.UDP_RELAY_PORT;
    }

    private String requireNickname() {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalStateException("Primero inicia sesión.");
        }
        return nickname;
    }

    private void requireIdle() {
        requireState(State.IDLE, "Ya hay una llamada en curso.");
    }

    private void requireState(State expected, String message) {
        if (state != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void resetCall() {
        if (media != null) {
            media.close();
            media = null;
        }
        peer = null;
        peerIp = null;
        peerPort = -1;
        groupRoom = null;
        groupHost = false;
        muted = false;
        state = State.IDLE;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacío.");
        }
        return value.trim();
    }

    private static String localIp() {
        String configured = System.getProperty("chat.voice.host");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException exception) {
            return "127.0.0.1";
        }
    }
}
