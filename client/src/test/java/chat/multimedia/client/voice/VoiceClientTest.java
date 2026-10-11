package chat.multimedia.client.voice;

import distributed.chat.CallError;
import distributed.chat.NotInRoom;
import distributed.chat.RoomNotFound;
import distributed.chat.UserNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceClientTest {

    private RecordingGateway gateway;
    private VoiceClient voiceClient;

    @BeforeEach
    void setUp() {
        gateway = new RecordingGateway();
        voiceClient = new VoiceClient(gateway);
        voiceClient.setLocalNickname("alice");
    }

    @Test
    void startsOutgoingCallThroughIceGateway() throws Exception {
        voiceClient.startCall("bob", "10.0.0.10", 5000);

        assertEquals("alice", gateway.startCaller);
        assertEquals("bob", gateway.startTarget);
        assertEquals("10.0.0.10", gateway.startIp);
        assertEquals(5000, gateway.startPort);
        assertEquals(
                VoiceCallState.OUTGOING_RINGING,
                voiceClient.getState()
        );
        assertEquals("bob", voiceClient.getPeerNickname());
    }

    @Test
    void incomingCallStoresRemoteEndpoint() {
        voiceClient.onIncomingCall("bob", "10.0.0.20", 5001);

        assertEquals(
                VoiceCallState.INCOMING_RINGING,
                voiceClient.getState()
        );
        assertEquals("bob", voiceClient.getPeerNickname());
        assertEquals("10.0.0.20", voiceClient.getRemoteIp());
        assertEquals(5001, voiceClient.getRemotePort());
    }

    @Test
    void acceptingIncomingCallUsesExpectedParticipantOrder() throws Exception {
        voiceClient.onIncomingCall("bob", "10.0.0.20", 5001);

        voiceClient.acceptIncomingCall("10.0.0.10", 5000);

        assertEquals("bob", gateway.acceptCaller);
        assertEquals("alice", gateway.acceptTarget);
        assertEquals("10.0.0.10", gateway.acceptIp);
        assertEquals(5000, gateway.acceptPort);
        assertEquals(VoiceCallState.ACTIVE, voiceClient.getState());
    }

    @Test
    void acceptedOutgoingCallBecomesActiveAndStoresRemoteEndpoint()
            throws Exception {
        voiceClient.startCall("bob", "10.0.0.10", 5000);

        voiceClient.onCallAccepted("bob", "10.0.0.20", 5001);

        assertEquals(VoiceCallState.ACTIVE, voiceClient.getState());
        assertEquals("10.0.0.20", voiceClient.getRemoteIp());
        assertEquals(5001, voiceClient.getRemotePort());
    }

    @Test
    void rejectingIncomingCallReturnsToIdle() throws Exception {
        voiceClient.onIncomingCall("bob", "10.0.0.20", 5001);

        voiceClient.rejectIncomingCall();

        assertEquals("bob", gateway.rejectCaller);
        assertEquals("alice", gateway.rejectTarget);
        assertEquals(VoiceCallState.IDLE, voiceClient.getState());
        assertNull(voiceClient.getPeerNickname());
        assertFalse(voiceClient.isBusy());
    }

    @Test
    void endingActiveCallReturnsToIdle() throws Exception {
        voiceClient.startCall("bob", "10.0.0.10", 5000);
        voiceClient.onCallAccepted("bob", "10.0.0.20", 5001);

        voiceClient.endCall();

        assertEquals("alice", gateway.endUser);
        assertEquals("bob", gateway.endTarget);
        assertEquals(VoiceCallState.IDLE, voiceClient.getState());
        assertNull(voiceClient.getRemoteIp());
        assertEquals(-1, voiceClient.getRemotePort());
    }

    @Test
    void remoteHangupReturnsToIdle() throws Exception {
        voiceClient.startCall("bob", "10.0.0.10", 5000);
        voiceClient.onCallAccepted("bob", "10.0.0.20", 5001);

        voiceClient.onCallEnded("bob");

        assertEquals(VoiceCallState.IDLE, voiceClient.getState());
        assertFalse(voiceClient.isBusy());
    }

    @Test
    void doesNotAllowSecondCallWhileBusy() throws Exception {
        voiceClient.startCall("bob", "10.0.0.10", 5000);

        assertThrows(
                IllegalStateException.class,
                () -> voiceClient.startCall(
                        "charlie",
                        "10.0.0.10",
                        5002
                )
        );
        assertTrue(voiceClient.isBusy());
    }

    @Test
    void requiresNicknameBeforeStartingCall() {
        try (
                VoiceClient clientWithoutNickname =
                        new VoiceClient(gateway)
        ) {
            assertThrows(
                    IllegalStateException.class,
                    () -> clientWithoutNickname.startCall(
                            "bob",
                            "10.0.0.10",
                            5000
                    )
            );
        }
    }


    @Test
    void togglesMuteDuringActiveCall() throws Exception {
        voiceClient.startCall("bob", "10.0.0.10", 5000);
        voiceClient.onCallAccepted("bob", "10.0.0.20", 5001);

        assertTrue(voiceClient.toggleMute());
        assertTrue(voiceClient.isMuted());
        assertFalse(voiceClient.toggleMute());
        assertFalse(voiceClient.isMuted());
    }

    private static final class RecordingGateway
            implements VoiceSignalingGateway {

        private String startCaller;
        private String startTarget;
        private String startIp;
        private int startPort;

        private String acceptCaller;
        private String acceptTarget;
        private String acceptIp;
        private int acceptPort;

        private String rejectCaller;
        private String rejectTarget;

        private String endUser;
        private String endTarget;

        @Override
        public void startCall(
                String caller,
                String target,
                String ip,
                int port
        ) throws UserNotFound, CallError {
            startCaller = caller;
            startTarget = target;
            startIp = ip;
            startPort = port;
        }

        @Override
        public void acceptCall(
                String caller,
                String target,
                String ip,
                int port
        ) throws UserNotFound, CallError {
            acceptCaller = caller;
            acceptTarget = target;
            acceptIp = ip;
            acceptPort = port;
        }

        @Override
        public void rejectCall(
                String caller,
                String target
        ) throws UserNotFound, CallError {
            rejectCaller = caller;
            rejectTarget = target;
        }

        @Override
        public void endCall(
                String user,
                String target
        ) throws UserNotFound, CallError {
            endUser = user;
            endTarget = target;
        }

        @Override
        public void startGroupCall(
                String host,
                String roomName
        ) throws RoomNotFound, NotInRoom, CallError {
        }

        @Override
        public void joinGroupCall(
                String nickname,
                String roomName,
                String ip,
                int port
        ) throws RoomNotFound, NotInRoom, CallError {
        }

        @Override
        public void leaveGroupCall(
                String nickname,
                String roomName
        ) throws RoomNotFound, CallError {
        }

        @Override
        public void endGroupCall(
                String host,
                String roomName
        ) throws RoomNotFound, CallError {
        }
    }
}
