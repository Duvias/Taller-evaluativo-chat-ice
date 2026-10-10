package chat.multimedia.client.voice;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrophoneCaptureTest {

    @Test
    void usesExpectedVoiceAudioFormat() {
        AudioFormat format = MicrophoneCapture.audioFormat();

        assertEquals(16000.0f, format.getSampleRate());
        assertEquals(16, format.getSampleSizeInBits());
        assertEquals(1, format.getChannels());
        assertTrue(
                format.getEncoding().equals(
                        AudioFormat.Encoding.PCM_SIGNED
                )
        );
        assertFalse(format.isBigEndian());
    }

    @Test
    void capturesAudioFrameFromInputDevice() throws Exception {
        byte[] expectedFrame = new byte[]{1, 2, 3, 4};
        FakeInputDevice inputDevice = new FakeInputDevice();
        inputDevice.enqueue(expectedFrame);
        inputDevice.finish();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> received = new AtomicReference<>();

        try (
                MicrophoneCapture capture = new MicrophoneCapture(
                        frame -> {
                            received.set(frame);
                            latch.countDown();
                        },
                        inputDevice
                )
        ) {
            capture.start();

            assertTrue(
                    latch.await(2, TimeUnit.SECONDS)
            );
            assertArrayEquals(
                    expectedFrame,
                    received.get()
            );
            assertTrue(inputDevice.opened);
            assertTrue(inputDevice.started);
        }
    }

    @Test
    void startingTwiceDoesNotOpenDeviceTwice() throws Exception {
        FakeInputDevice inputDevice = new FakeInputDevice();

        try (
                MicrophoneCapture capture = new MicrophoneCapture(
                        frame -> {
                        },
                        inputDevice
                )
        ) {
            capture.start();
            capture.start();

            assertEquals(1, inputDevice.openCount);
            assertEquals(1, inputDevice.startCount);
        }
    }

    @Test
    void closeStopsAndClosesInputDevice() throws Exception {
        FakeInputDevice inputDevice = new FakeInputDevice();
        MicrophoneCapture capture = new MicrophoneCapture(
                frame -> {
                },
                inputDevice
        );

        capture.start();

        assertTrue(capture.isRunning());

        capture.close();

        assertFalse(capture.isRunning());
        assertTrue(inputDevice.stopped);
        assertTrue(inputDevice.closed);
    }

    private static final class FakeInputDevice
            implements MicrophoneCapture.InputDevice {

        private final Queue<byte[]> frames = new ArrayDeque<>();

        private boolean opened;
        private boolean started;
        private boolean stopped;
        private boolean closed;
        private boolean finished;
        private int openCount;
        private int startCount;

        void enqueue(byte[] frame) {
            frames.add(frame.clone());
        }

        void finish() {
            finished = true;
        }

        @Override
        public synchronized void open(AudioFormat format) {
            opened = true;
            openCount++;
        }

        @Override
        public synchronized void start() {
            started = true;
            startCount++;
        }

        @Override
        public synchronized int read(
                byte[] buffer,
                int offset,
                int length
        ) {
            byte[] frame = frames.poll();

            if (frame != null) {
                int count = Math.min(frame.length, length);
                System.arraycopy(
                        frame,
                        0,
                        buffer,
                        offset,
                        count
                );
                return count;
            }

            if (finished) {
                return -1;
            }

            try {
                wait(10);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return -1;
            }

            return 0;
        }

        @Override
        public synchronized void stop() {
            stopped = true;
            notifyAll();
        }

        @Override
        public synchronized void close() {
            closed = true;
            notifyAll();
        }
    }
}
