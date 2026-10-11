package chat.multimedia.server.service;

import chat.multimedia.server.file.FileManager;
import chat.multimedia.server.room.RoomManager;
import chat.multimedia.server.user.UserManager;
import chat.multimedia.server.voice.CallState;
import chat.multimedia.server.voice.VoiceManager;
import distributed.chat.CallError;
import distributed.chat.ClientCallbackPrx;
import distributed.chat.UserNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceImplVoiceSignalingTest {

    private UserManager userManager;
    private VoiceManager voiceManager;
    private ChatServiceImpl service;
    private RecordingCallback alice;
    private RecordingCallback bob;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        voiceManager = new VoiceManager();
        service = new ChatServiceImpl(
                userManager,
                new RoomManager(),
                new FileManager(),
                voiceManager
        );

        alice = new RecordingCallback();
        bob = new RecordingCallback();

        userManager.connectedUsers().put("alice", alice.proxy());
        userManager.connectedUsers().put("bob", bob.proxy());
    }

    @Test
    void startCallNotifiesTargetThroughCallback() throws Exception {
        service.startCall(
                "alice",
                "bob",
                "10.0.0.10",
                5000,
                null
        );

        assertEquals("alice", bob.incomingCaller);
        assertEquals("10.0.0.10", bob.incomingIp);
        assertEquals(5000, bob.incomingPort);
        assertEquals(
                CallState.RINGING,
                voiceManager.getCall("alice").getState()
        );
    }

    @Test
    void acceptCallNotifiesCallerWithTargetUdpEndpoint() throws Exception {
        service.startCall(
                "alice",
                "bob",
                "10.0.0.10",
                5000,
                null
        );

        service.acceptCall(
                "alice",
                "bob",
                "10.0.0.20",
                5001,
                null
        );

        assertEquals("bob", alice.acceptedUser);
        assertEquals("10.0.0.20", alice.acceptedIp);
        assertEquals(5001, alice.acceptedPort);
        assertEquals(
                CallState.ACTIVE,
                voiceManager.getCall("alice").getState()
        );
    }

    @Test
    void rejectCallNotifiesCallerAndReleasesSession() throws Exception {
        service.startCall(
                "alice",
                "bob",
                "10.0.0.10",
                5000,
                null
        );

        service.rejectCall("alice", "bob", null);

        assertEquals("bob", alice.rejectedUser);
        assertFalse(voiceManager.isInCall("alice"));
        assertFalse(voiceManager.isInCall("bob"));
    }

    @Test
    void endCallNotifiesOtherParticipantAndReleasesSession() throws Exception {
        service.startCall(
                "alice",
                "bob",
                "10.0.0.10",
                5000,
                null
        );
        service.acceptCall(
                "alice",
                "bob",
                "10.0.0.20",
                5001,
                null
        );

        service.endCall("bob", "alice", null);

        assertEquals("bob", alice.endedByUser);
        assertFalse(voiceManager.isInCall("alice"));
        assertFalse(voiceManager.isInCall("bob"));
    }

    @Test
    void startCallRejectsDisconnectedTargetWithoutCreatingSession() {
        userManager.connectedUsers().remove("bob");

        UserNotFound exception = assertThrows(
                UserNotFound.class,
                () -> service.startCall(
                        "alice",
                        "bob",
                        "10.0.0.10",
                        5000,
                        null
                )
        );

        assertEquals("bob", exception.nickname);
        assertFalse(voiceManager.isInCall("alice"));
    }

    @Test
    void startCallConvertsBusyStateIntoSliceCallError() throws Exception {
        userManager.connectedUsers().put(
                "charlie",
                new RecordingCallback().proxy()
        );

        service.startCall(
                "alice",
                "bob",
                "10.0.0.10",
                5000,
                null
        );

        CallError exception = assertThrows(
                CallError.class,
                () -> service.startCall(
                        "charlie",
                        "bob",
                        "10.0.0.30",
                        5002,
                        null
                )
        );

        assertTrue(exception.reason.contains("already in a call"));
    }

    private static final class RecordingCallback {
        private String incomingCaller;
        private String incomingIp;
        private int incomingPort;

        private String acceptedUser;
        private String acceptedIp;
        private int acceptedPort;

        private String rejectedUser;
        private String endedByUser;

        private ClientCallbackPrx proxy() {
            return (ClientCallbackPrx) Proxy.newProxyInstance(
                    ClientCallbackPrx.class.getClassLoader(),
                    new Class<?>[]{ClientCallbackPrx.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "incomingCall" -> {
                                incomingCaller = (String) args[0];
                                incomingIp = (String) args[1];
                                incomingPort = (Integer) args[2];
                                return null;
                            }
                            case "callAccepted" -> {
                                acceptedUser = (String) args[0];
                                acceptedIp = (String) args[1];
                                acceptedPort = (Integer) args[2];
                                return null;
                            }
                            case "callRejected" -> {
                                rejectedUser = (String) args[0];
                                return null;
                            }
                            case "callEnded" -> {
                                endedByUser = (String) args[0];
                                return null;
                            }
                            case "toString" -> {
                                return "RecordingCallbackPrx";
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "equals" -> {
                                return proxy == args[0];
                            }
                            default -> throw new UnsupportedOperationException(
                                    "Unexpected proxy method: " + method.getName()
                            );
                        }
                    }
            );
        }
    }
}
