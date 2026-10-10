package chat.multimedia.client.voice;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UdpVoiceTransportTest {

    @Test
    void sendsAndReceivesVoiceFrame() throws Exception {
        CountDownLatch receivedLatch = new CountDownLatch(1);
        AtomicReference<byte[]> receivedFrame = new AtomicReference<>();

        try (
                UdpVoiceTransport receiver = new UdpVoiceTransport(
                        0,
                        frame -> {
                            receivedFrame.set(frame);
                            receivedLatch.countDown();
                        }
                );
                UdpVoiceTransport sender = new UdpVoiceTransport(
                        0,
                        frame -> {
                        }
                )
        ) {
            receiver.start();
            sender.start();

            byte[] frame = "voice-frame".getBytes(
                    StandardCharsets.UTF_8
            );

            sender.send(
                    frame,
                    "127.0.0.1",
                    receiver.getLocalPort()
            );

            assertTrue(
                    receivedLatch.await(2, TimeUnit.SECONDS)
            );
            assertArrayEquals(frame, receivedFrame.get());
        }
    }

    @Test
    void requiresTransportToBeStartedBeforeSending()
            throws Exception {
        try (
                UdpVoiceTransport transport =
                        new UdpVoiceTransport(0, frame -> {
                        })
        ) {
            assertThrows(
                    IllegalStateException.class,
                    () -> transport.send(
                            new byte[]{1, 2, 3},
                            "127.0.0.1",
                            5000
                    )
            );
        }
    }

    @Test
    void rejectsOversizedVoiceFrames() throws Exception {
        try (
                UdpVoiceTransport transport =
                        new UdpVoiceTransport(0, frame -> {
                        })
        ) {
            transport.start();

            byte[] frame =
                    new byte[UdpVoiceTransport.MAX_PACKET_SIZE + 1];

            assertThrows(
                    IllegalArgumentException.class,
                    () -> transport.send(
                            frame,
                            "127.0.0.1",
                            5000
                    )
            );
        }
    }

    @Test
    void closeStopsTransport() throws Exception {
        UdpVoiceTransport transport =
                new UdpVoiceTransport(0, frame -> {
                });

        transport.start();

        assertTrue(transport.isRunning());

        transport.close();

        assertFalse(transport.isRunning());
    }
}
