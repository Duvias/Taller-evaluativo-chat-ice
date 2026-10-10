package chat.multimedia.client.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class MicrophoneCapture implements AutoCloseable {

    public static final float SAMPLE_RATE = 16000.0f;
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int CHANNELS = 1;
    public static final int FRAME_SIZE_BYTES = 640;

    private final Consumer<byte[]> frameConsumer;
    private final InputDevice inputDevice;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread captureThread;

    public MicrophoneCapture(Consumer<byte[]> frameConsumer) {
        this(
                frameConsumer,
                new JavaSoundInputDevice()
        );
    }

    MicrophoneCapture(
            Consumer<byte[]> frameConsumer,
            InputDevice inputDevice
    ) {
        this.frameConsumer = Objects.requireNonNull(
                frameConsumer,
                "frameConsumer"
        );
        this.inputDevice = Objects.requireNonNull(
                inputDevice,
                "inputDevice"
        );
    }

    public static AudioFormat audioFormat() {
        return new AudioFormat(
                SAMPLE_RATE,
                SAMPLE_SIZE_BITS,
                CHANNELS,
                true,
                false
        );
    }

    public synchronized void start() throws LineUnavailableException {
        if (running.get()) {
            return;
        }

        inputDevice.open(audioFormat());
        inputDevice.start();
        running.set(true);

        captureThread = new Thread(
                this::captureLoop,
                "voice-microphone-capture"
        );
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public boolean isRunning() {
        return running.get();
    }

    private void captureLoop() {
        byte[] buffer = new byte[FRAME_SIZE_BYTES];

        while (running.get()) {
            int bytesRead = inputDevice.read(
                    buffer,
                    0,
                    buffer.length
            );

            if (bytesRead < 0) {
                running.set(false);
                break;
            }

            if (bytesRead == 0) {
                continue;
            }

            frameConsumer.accept(
                    Arrays.copyOf(buffer, bytesRead)
            );
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);
        inputDevice.stop();
        inputDevice.close();

        if (captureThread == null
                || captureThread == Thread.currentThread()) {
            return;
        }

        try {
            captureThread.join(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        captureThread = null;
    }

    interface InputDevice {

        void open(AudioFormat format) throws LineUnavailableException;

        void start();

        int read(
                byte[] buffer,
                int offset,
                int length
        );

        void stop();

        void close();
    }

    private static final class JavaSoundInputDevice
            implements InputDevice {

        private TargetDataLine line;

        @Override
        public void open(AudioFormat format)
                throws LineUnavailableException {
            DataLine.Info info = new DataLine.Info(
                    TargetDataLine.class,
                    format
            );

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
        }

        @Override
        public void start() {
            line.start();
        }

        @Override
        public int read(
                byte[] buffer,
                int offset,
                int length
        ) {
            return line.read(buffer, offset, length);
        }

        @Override
        public void stop() {
            if (line != null) {
                line.stop();
            }
        }

        @Override
        public void close() {
            if (line != null) {
                line.close();
                line = null;
            }
        }
    }
}
