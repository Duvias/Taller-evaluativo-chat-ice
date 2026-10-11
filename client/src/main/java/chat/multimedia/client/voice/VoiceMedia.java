package chat.multimedia.client.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public final class VoiceMedia implements AutoCloseable {

    private static final AudioFormat FORMAT = new AudioFormat(
            16000.0f,
            16,
            1,
            true,
            false
    );
    private static final int FRAME_BYTES = 640;
    private static final int MAX_PACKET_BYTES = 2048;

    private final DatagramSocket socket;

    private volatile boolean running;
    private volatile boolean muted;
    private volatile InetAddress remoteAddress;
    private volatile int remotePort;

    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private Thread senderThread;
    private Thread receiverThread;

    public VoiceMedia() throws SocketException {
        socket = new DatagramSocket(0);
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public synchronized void start(
            String remoteIp,
            int remotePort
    ) throws IOException, LineUnavailableException {
        setRemote(remoteIp, remotePort);

        if (running) {
            return;
        }

        DataLine.Info microphoneInfo = new DataLine.Info(
                TargetDataLine.class,
                FORMAT
        );
        DataLine.Info speakersInfo = new DataLine.Info(
                SourceDataLine.class,
                FORMAT
        );

        microphone = (TargetDataLine) AudioSystem.getLine(microphoneInfo);
        speakers = (SourceDataLine) AudioSystem.getLine(speakersInfo);
        microphone.open(FORMAT);
        speakers.open(FORMAT);
        microphone.start();
        speakers.start();
        running = true;

        senderThread = new Thread(this::sendLoop, "voice-udp-sender");
        receiverThread = new Thread(this::receiveLoop, "voice-udp-receiver");
        senderThread.setDaemon(true);
        receiverThread.setDaemon(true);
        senderThread.start();
        receiverThread.start();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    private void setRemote(
            String remoteIp,
            int remotePort
    ) throws IOException {
        if (remoteIp == null || remoteIp.isBlank()) {
            throw new IllegalArgumentException("La IP remota no puede estar vacía.");
        }
        if (remotePort < 1 || remotePort > 65535) {
            throw new IllegalArgumentException("Puerto UDP remoto inválido.");
        }

        remoteAddress = InetAddress.getByName(remoteIp);
        this.remotePort = remotePort;
    }

    private void sendLoop() {
        byte[] buffer = new byte[FRAME_BYTES];

        while (running) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead <= 0 || muted) {
                continue;
            }

            try {
                DatagramPacket packet = new DatagramPacket(
                        buffer,
                        bytesRead,
                        remoteAddress,
                        remotePort
                );
                socket.send(packet);
            } catch (IOException exception) {
                if (running) {
                    running = false;
                }
            }
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[MAX_PACKET_BYTES];

        while (running) {
            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length
            );

            try {
                socket.receive(packet);
                byte[] audio = Arrays.copyOfRange(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getOffset() + packet.getLength()
                );
                speakers.write(audio, 0, audio.length);
            } catch (SocketException exception) {
                if (running) {
                    running = false;
                }
            } catch (IOException exception) {
                running = false;
            }
        }
    }

    @Override
    public synchronized void close() {
        running = false;

        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }

        if (speakers != null) {
            speakers.stop();
            speakers.close();
            speakers = null;
        }

        socket.close();
        join(senderThread);
        join(receiverThread);
        senderThread = null;
        receiverThread = null;
    }

    private static void join(Thread thread) {
        if (thread == null || thread == Thread.currentThread()) {
            return;
        }

        try {
            thread.join(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
