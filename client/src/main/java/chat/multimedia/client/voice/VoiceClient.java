package chat.multimedia.client.voice;

import distributed.chat.CallError;
import distributed.chat.ChatServicePrx;
import distributed.chat.NotInRoom;
import distributed.chat.RoomNotFound;
import distributed.chat.UserNotFound;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class VoiceClient implements AutoCloseable {

    private final VoiceSignalingGateway signalingGateway;

    private String localNickname;
    private String peerNickname;
    private String remoteIp;
    private int remotePort = -1;
    private String groupRoom;
    private String relayIp;
    private int relayPort = -1;
    private VoiceCallState state = VoiceCallState.IDLE;
    private volatile boolean muted;

    private UdpVoiceTransport transport;
    private MicrophoneCapture microphone;
    private AudioPlayback playback;

    public VoiceClient(ChatServicePrx chatService) {
        this(new IceVoiceSignalingGateway(
                Objects.requireNonNull(chatService, "chatService")
        ));
    }

    VoiceClient(VoiceSignalingGateway signalingGateway) {
        this.signalingGateway = Objects.requireNonNull(
                signalingGateway,
                "signalingGateway"
        );
    }

    public synchronized void setLocalNickname(String nickname) {
        requireIdle("Cannot change nickname while a call is in progress.");
        localNickname = requireText(nickname, "nickname");
    }

    public synchronized void startCall(String target)
            throws UserNotFound, CallError {
        prepareTransport();

        try {
            startCall(
                    target,
                    resolveLocalIp(),
                    transport.getLocalPort()
            );
        } catch (RuntimeException | UserNotFound | CallError exception) {
            cleanupMedia();
            throw exception;
        }
    }

    public synchronized void startCall(
            String target,
            String localIp,
            int localPort
    ) throws UserNotFound, CallError {
        requireIdle("A call is already in progress.");
        String caller = requireLocalNickname();
        String validatedTarget = requireText(target, "target");
        validateEndpoint(localIp, localPort);

        signalingGateway.startCall(
                caller,
                validatedTarget,
                localIp,
                localPort
        );

        peerNickname = validatedTarget;
        remoteIp = null;
        remotePort = -1;
        state = VoiceCallState.OUTGOING_RINGING;
    }

    public synchronized void acceptIncomingCall()
            throws UserNotFound, CallError {
        prepareTransport();

        try {
            acceptIncomingCall(
                    resolveLocalIp(),
                    transport.getLocalPort()
            );
        } catch (RuntimeException | UserNotFound | CallError exception) {
            cleanupMedia();
            throw exception;
        }
    }

    public synchronized void acceptIncomingCall(
            String localIp,
            int localPort
    ) throws UserNotFound, CallError {
        requireState(
                VoiceCallState.INCOMING_RINGING,
                "There is no incoming call to accept."
        );
        validateEndpoint(localIp, localPort);

        signalingGateway.acceptCall(
                peerNickname,
                requireLocalNickname(),
                localIp,
                localPort
        );

        state = VoiceCallState.ACTIVE;

        if (transport != null) {
            startMedia();
        }
    }

    public synchronized void rejectIncomingCall()
            throws UserNotFound, CallError {
        requireState(
                VoiceCallState.INCOMING_RINGING,
                "There is no incoming call to reject."
        );

        signalingGateway.rejectCall(
                peerNickname,
                requireLocalNickname()
        );

        cleanupAndReset();
    }

    public synchronized void endCall()
            throws UserNotFound, CallError {
        if (state == VoiceCallState.IDLE) {
            throw new IllegalStateException(
                    "There is no call to end."
            );
        }

        if (state == VoiceCallState.GROUP_ACTIVE) {
            try {
                signalingGateway.leaveGroupCall(
                        requireLocalNickname(),
                        groupRoom
                );
            } catch (RoomNotFound exception) {
                throw new CallError(
                        "The group call room no longer exists."
                );
            }

            cleanupAndReset();
            return;
        }

        signalingGateway.endCall(
                requireLocalNickname(),
                peerNickname
        );

        cleanupAndReset();
    }

    public synchronized boolean toggleMute() {
        if (state != VoiceCallState.ACTIVE
                && state != VoiceCallState.GROUP_ACTIVE) {
            throw new IllegalStateException(
                    "Mute is available only during an active voice call."
            );
        }

        muted = !muted;
        return muted;
    }

    public void startGroupCall(String roomName)
            throws RoomNotFound, NotInRoom, CallError {
        String nickname;
        String validatedRoom;
        String localIp;
        int localPort;

        synchronized (this) {
            requireIdle("A call is already in progress.");
            nickname = requireLocalNickname();
            validatedRoom = requireText(roomName, "roomName");
            prepareTransport();
            localIp = resolveLocalIp();
            localPort = transport.getLocalPort();
        }

        try {
            signalingGateway.startGroupCall(
                    nickname,
                    validatedRoom
            );

            signalingGateway.joinGroupCall(
                    nickname,
                    validatedRoom,
                    localIp,
                    localPort
            );

            synchronized (this) {
                requireRelayEndpoint(validatedRoom);
                groupRoom = validatedRoom;
                state = VoiceCallState.GROUP_ACTIVE;
                startMedia();
            }
        } catch (RuntimeException
                 | RoomNotFound
                 | NotInRoom
                 | CallError exception) {
            synchronized (this) {
                cleanupAndReset();
            }
            throw exception;
        }
    }

    public void joinGroupCall(String roomName)
            throws RoomNotFound, NotInRoom, CallError {
        String nickname;
        String validatedRoom;
        String localIp;
        int localPort;

        synchronized (this) {
            requireIdle("A call is already in progress.");
            nickname = requireLocalNickname();
            validatedRoom = requireText(roomName, "roomName");
            prepareTransport();
            localIp = resolveLocalIp();
            localPort = transport.getLocalPort();
        }

        try {
            signalingGateway.joinGroupCall(
                    nickname,
                    validatedRoom,
                    localIp,
                    localPort
            );

            synchronized (this) {
                requireRelayEndpoint(validatedRoom);
                groupRoom = validatedRoom;
                state = VoiceCallState.GROUP_ACTIVE;
                startMedia();
            }
        } catch (RuntimeException
                 | RoomNotFound
                 | NotInRoom
                 | CallError exception) {
            synchronized (this) {
                cleanupAndReset();
            }
            throw exception;
        }
    }

    public void leaveGroupCall()
            throws RoomNotFound, CallError {
        String nickname;
        String roomName;

        synchronized (this) {
            requireState(
                    VoiceCallState.GROUP_ACTIVE,
                    "There is no active group call to leave."
            );
            nickname = requireLocalNickname();
            roomName = groupRoom;
        }

        signalingGateway.leaveGroupCall(
                nickname,
                roomName
        );

        synchronized (this) {
            cleanupAndReset();
        }
    }

    public void endGroupCall()
            throws RoomNotFound, CallError {
        String nickname;
        String roomName;

        synchronized (this) {
            requireState(
                    VoiceCallState.GROUP_ACTIVE,
                    "There is no active group call to end."
            );
            nickname = requireLocalNickname();
            roomName = groupRoom;
        }

        signalingGateway.endGroupCall(
                nickname,
                roomName
        );

        synchronized (this) {
            cleanupAndReset();
        }
    }

    public synchronized void onIncomingCall(
            String caller,
            String ip,
            int port
    ) {
        requireIdle("Cannot receive a second call while already busy.");
        validateEndpoint(ip, port);

        peerNickname = requireText(caller, "caller");
        remoteIp = ip;
        remotePort = port;
        state = VoiceCallState.INCOMING_RINGING;
    }

    public synchronized void onCallAccepted(
            String user,
            String ip,
            int port
    ) {
        requireState(
                VoiceCallState.OUTGOING_RINGING,
                "No outgoing call is waiting for acceptance."
        );
        requireExpectedPeer(user);
        validateEndpoint(ip, port);

        remoteIp = ip;
        remotePort = port;
        state = VoiceCallState.ACTIVE;

        if (transport != null) {
            startMedia();
        }
    }

    public synchronized void onCallRejected(String user) {
        requireState(
                VoiceCallState.OUTGOING_RINGING,
                "No outgoing call is waiting for rejection."
        );
        requireExpectedPeer(user);
        cleanupAndReset();
    }

    public synchronized void onCallEnded(String user) {
        if (state == VoiceCallState.IDLE) {
            return;
        }

        requireExpectedPeer(user);
        cleanupAndReset();
    }

    public synchronized void onGroupCallStarted(
            String roomName,
            String host,
            String ip,
            int port
    ) {
        requireText(host, "host");
        validateEndpoint(ip, port);

        groupRoom = requireText(roomName, "roomName");
        relayIp = ip;
        relayPort = port;
    }

    public synchronized void onGroupCallEnded(String roomName) {
        if (groupRoom == null) {
            return;
        }

        if (groupRoom.equals(roomName)) {
            cleanupAndReset();
        }
    }

    public synchronized VoiceCallState getState() {
        return state;
    }

    public synchronized String getLocalNickname() {
        return localNickname;
    }

    public synchronized String getPeerNickname() {
        return peerNickname;
    }

    public synchronized String getRemoteIp() {
        return remoteIp;
    }

    public synchronized int getRemotePort() {
        return remotePort;
    }

    public synchronized String getGroupRoom() {
        return groupRoom;
    }

    public synchronized boolean isBusy() {
        return state != VoiceCallState.IDLE;
    }

    public boolean isMuted() {
        return muted;
    }

    @Override
    public synchronized void close() {
        cleanupAndReset();
    }

    private void prepareTransport() {
        if (transport != null) {
            return;
        }

        try {
            transport = new UdpVoiceTransport(
                    0,
                    this::handleIncomingFrame
            );
            transport.start();
        } catch (IOException exception) {
            cleanupMedia();
            throw new IllegalStateException(
                    "Could not open the UDP voice socket.",
                    exception
            );
        }
    }

    private void startMedia() {
        if (playback != null || microphone != null) {
            return;
        }

        try {
            playback = new AudioPlayback();
            playback.start();

            microphone = new MicrophoneCapture(
                    this::handleCapturedFrame
            );
            microphone.start();
        } catch (LineUnavailableException exception) {
            cleanupMedia();
            throw new IllegalStateException(
                    "Could not open the audio device.",
                    exception
            );
        }
    }

    private void handleCapturedFrame(byte[] frame) {
        if (muted) {
            return;
        }

        String targetIp;
        int targetPort;

        synchronized (this) {
            if (state == VoiceCallState.GROUP_ACTIVE) {
                targetIp = relayIp;
                targetPort = relayPort;
            } else if (state == VoiceCallState.ACTIVE) {
                targetIp = remoteIp;
                targetPort = remotePort;
            } else {
                return;
            }
        }

        if (targetIp == null || targetPort < 1 || transport == null) {
            return;
        }

        try {
            transport.send(frame, targetIp, targetPort);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not send voice audio through UDP.",
                    exception
            );
        }
    }

    private void handleIncomingFrame(byte[] frame) {
        AudioPlayback currentPlayback;

        synchronized (this) {
            currentPlayback = playback;
        }

        if (currentPlayback != null && currentPlayback.isRunning()) {
            currentPlayback.play(frame);
        }
    }

    private void requireRelayEndpoint(String roomName) {
        if (relayIp == null
                || relayPort < 1
                || !roomName.equals(groupRoom)) {
            throw new IllegalStateException(
                    "The group voice relay endpoint was not received."
            );
        }
    }

    private String requireLocalNickname() {
        if (localNickname == null || localNickname.isBlank()) {
            throw new IllegalStateException(
                    "A local nickname must be configured before using voice calls."
            );
        }

        return localNickname;
    }

    private void requireIdle(String message) {
        if (state != VoiceCallState.IDLE) {
            throw new IllegalStateException(message);
        }
    }

    private void requireState(
            VoiceCallState expected,
            String message
    ) {
        if (state != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void requireExpectedPeer(String nickname) {
        String validatedNickname = requireText(nickname, "nickname");

        if (!validatedNickname.equals(peerNickname)) {
            throw new IllegalStateException(
                    "Unexpected call participant: " + validatedNickname
            );
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank."
            );
        }

        return value;
    }

    private static void validateEndpoint(String ip, int port) {
        requireText(ip, "ip");

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "port must be between 1 and 65535."
            );
        }
    }

    private static String resolveLocalIp() {
        String configured = System.getProperty("chat.voice.host");

        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException exception) {
            return "127.0.0.1";
        }
    }

    private void cleanupAndReset() {
        cleanupMedia();
        peerNickname = null;
        remoteIp = null;
        remotePort = -1;
        groupRoom = null;
        relayIp = null;
        relayPort = -1;
        muted = false;
        state = VoiceCallState.IDLE;
    }

    private void cleanupMedia() {
        if (microphone != null) {
            microphone.close();
            microphone = null;
        }

        if (transport != null) {
            transport.close();
            transport = null;
        }

        if (playback != null) {
            playback.close();
            playback = null;
        }
    }
}
