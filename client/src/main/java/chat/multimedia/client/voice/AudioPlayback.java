package chat.multimedia.client.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AudioPlayback implements AutoCloseable {

    private final OutputDevice outputDevice;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AudioPlayback() {
        this(new JavaSoundOutputDevice());
    }

    AudioPlayback(OutputDevice outputDevice) {
        this.outputDevice = Objects.requireNonNull(
                outputDevice,
                "outputDevice"
        );
    }

    public synchronized void start() throws LineUnavailableException {
        if (running.get()) {
            return;
        }

        outputDevice.open(MicrophoneCapture.audioFormat());
        outputDevice.start();
        running.set(true);
    }

    public void play(byte[] frame) {
        if (!running.get()) {
            throw new IllegalStateException(
                    "Audio playback is not running."
            );
        }

        Objects.requireNonNull(frame, "frame");

        if (frame.length == 0) {
            throw new IllegalArgumentException(
                    "Audio frame cannot be empty."
            );
        }

        int offset = 0;

        while (offset < frame.length && running.get()) {
            int written = outputDevice.write(
                    frame,
                    offset,
                    frame.length - offset
            );

            if (written <= 0) {
                throw new IllegalStateException(
                        "Audio output device did not accept data."
                );
            }

            offset += written;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public synchronized void close() {
        if (!running.getAndSet(false)) {
            outputDevice.close();
            return;
        }

        outputDevice.drain();
        outputDevice.stop();
        outputDevice.close();
    }

    interface OutputDevice {

        void open(AudioFormat format) throws LineUnavailableException;

        void start();

        int write(
                byte[] buffer,
                int offset,
                int length
        );

        void drain();

        void stop();

        void close();
    }

    private static final class JavaSoundOutputDevice
            implements OutputDevice {

        private SourceDataLine line;

        @Override
        public void open(AudioFormat format)
                throws LineUnavailableException {
            DataLine.Info info = new DataLine.Info(
                    SourceDataLine.class,
                    format
            );

            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
        }

        @Override
        public void start() {
            line.start();
        }

        @Override
        public int write(
                byte[] buffer,
                int offset,
                int length
        ) {
            return line.write(buffer, offset, length);
        }

        @Override
        public void drain() {
            if (line != null) {
                line.drain();
            }
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
