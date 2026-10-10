package chat.multimedia.client.voice;

import distributed.chat.CallError;
import distributed.chat.ChatServicePrx;
import distributed.chat.UserNotFound;

import java.util.Objects;

public class VoiceClient {

    private final VoiceSignalingGateway signalingGateway;

    private String localNickname;
    private String peerNickname;
    private String remoteIp;
    private int remotePort = -1;
    private VoiceCallState state = VoiceCallState.IDLE;

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
        this.localNickname = requireText(nickname, "nickname");
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

        resetCallState();
    }

    public synchronized void endCall()
            throws UserNotFound, CallError {
        if (state == VoiceCallState.IDLE) {
            throw new IllegalStateException("There is no call to end.");
        }

        signalingGateway.endCall(
                requireLocalNickname(),
                peerNickname
        );

        resetCallState();
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
    }

    public synchronized void onCallRejected(String user) {
        requireState(
                VoiceCallState.OUTGOING_RINGING,
                "No outgoing call is waiting for rejection."
        );
        requireExpectedPeer(user);
        resetCallState();
    }

    public synchronized void onCallEnded(String user) {
        if (state == VoiceCallState.IDLE) {
            return;
        }

        requireExpectedPeer(user);
        resetCallState();
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

    public synchronized boolean isBusy() {
        return state != VoiceCallState.IDLE;
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
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
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

    private void resetCallState() {
        peerNickname = null;
        remoteIp = null;
        remotePort = -1;
        state = VoiceCallState.IDLE;
    }
}
