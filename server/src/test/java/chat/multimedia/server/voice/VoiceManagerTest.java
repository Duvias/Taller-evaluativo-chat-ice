package chat.multimedia.server.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceManagerTest {

    @Test
    void startsAndAcceptsOneToOneCall() {
        VoiceManager manager = new VoiceManager();

        CallSession session = manager.startCall(
                "alice",
                "bob",
                "127.0.0.1",
                5000
        );

        assertEquals(CallState.RINGING, session.getState());
        assertTrue(manager.isInCall("alice"));
        assertTrue(manager.isInCall("bob"));

        CallSession accepted = manager.acceptCall(
                "alice",
                "bob",
                "127.0.0.1",
                5001
        );

        assertEquals(CallState.ACTIVE, accepted.getState());
        assertEquals("127.0.0.1", accepted.getTargetIp());
        assertEquals(5001, accepted.getTargetPort());
    }

    @Test
    void rejectingCallReleasesBothUsers() {
        VoiceManager manager = new VoiceManager();
        manager.startCall("alice", "bob", "127.0.0.1", 5000);

        manager.rejectCall("alice", "bob");

        assertFalse(manager.isInCall("alice"));
        assertFalse(manager.isInCall("bob"));
    }

    @Test
    void endingCallReleasesBothUsers() {
        VoiceManager manager = new VoiceManager();
        manager.startCall("alice", "bob", "127.0.0.1", 5000);
        manager.acceptCall("alice", "bob", "127.0.0.1", 5001);

        manager.endCall("bob", "alice");

        assertFalse(manager.isInCall("alice"));
        assertFalse(manager.isInCall("bob"));
    }

    @Test
    void preventsCallingYourself() {
        VoiceManager manager = new VoiceManager();

        assertThrows(
                IllegalStateException.class,
                () -> manager.startCall("alice", "alice", "127.0.0.1", 5000)
        );
    }

    @Test
    void preventsUserFromBeingInTwoCalls() {
        VoiceManager manager = new VoiceManager();
        manager.startCall("alice", "bob", "127.0.0.1", 5000);

        assertThrows(
                IllegalStateException.class,
                () -> manager.startCall("charlie", "bob", "127.0.0.1", 5002)
        );
    }
}
