package chat.multimedia.server.voice;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceRelayServerTest {

    @Test
    void forwardsAudioToOtherParticipantInSameRoom()
            throws Exception {
        try (
                VoiceRelayServer relay = new VoiceRelayServer(0);
                DatagramSocket sender = new DatagramSocket(0);
                DatagramSocket receiver = new DatagramSocket(0)
        ) {
            relay.start();
            receiver.setSoTimeout(2000);

            relay.register(
                    "general",
                    "alice",
                    "127.0.0.1",
                    sender.getLocalPort()
            );
            relay.register(
                    "general",
                    "bob",
                    "127.0.0.1",
                    receiver.getLocalPort()
            );

            byte[] audio = new byte[]{1, 2, 3, 4};

            sender.send(
                    new DatagramPacket(
                            audio,
                            audio.length,
                            InetAddress.getByName("127.0.0.1"),
                            relay.getLocalPort()
                    )
            );

            byte[] buffer = new byte[64];
            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length
            );

            receiver.receive(packet);

            assertArrayEquals(
                    audio,
                    Arrays.copyOf(
                            packet.getData(),
                            packet.getLength()
                    )
            );
            assertTrue(relay.isRunning());
        }
    }
}
