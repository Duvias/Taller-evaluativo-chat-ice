package chat.multimedia.client.voice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class UdpVoiceTransport implements AutoCloseable {

    public static final int MAX_PACKET_SIZE = 8192;

    private final DatagramSocket socket;
    private final Consumer<byte[]> frameConsumer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread receiverThread;

    public UdpVoiceTransport(
            int localPort,
            Consumer<byte[]> frameConsumer
    ) throws SocketException {
        validatePortAllowZero(localPort);
        this.frameConsumer = Objects.requireNonNull(
                frameConsumer,
                "frameConsumer"
        );
        this.socket = new DatagramSocket(localPort);
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        receiverThread = new Thread(
                this::receiveLoop,
                "voice-udp-receiver-" + socket.getLocalPort()
        );
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public void send(
            byte[] frame,
            String remoteIp,
            int remotePort
    ) throws IOException {
        if (!running.get()) {
            throw new IllegalStateException(
                    "UDP voice transport is not running."
            );
        }

        Objects.requireNonNull(frame, "frame");

        if (frame.length == 0) {
            throw new IllegalArgumentException(
                    "Voice frame cannot be empty."
            );
        }

        if (frame.length > MAX_PACKET_SIZE) {
            throw new IllegalArgumentException(
                    "Voice frame exceeds maximum UDP packet size."
            );
        }

        validatePort(remotePort);

        if (remoteIp == null || remoteIp.isBlank()) {
            throw new IllegalArgumentException(
                    "Remote IP cannot be blank."
            );
        }

        InetAddress address = InetAddress.getByName(remoteIp);

        DatagramPacket packet = new DatagramPacket(
                frame,
                frame.length,
                address,
                remotePort
        );

        socket.send(packet);
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public boolean isRunning() {
        return running.get();
    }

    private void receiveLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];

        while (running.get()) {
            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length
            );

            try {
                socket.receive(packet);

                byte[] frame = Arrays.copyOfRange(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getOffset() + packet.getLength()
                );

                frameConsumer.accept(frame);
            } catch (SocketException exception) {
                if (running.get()) {
                    running.set(false);
                }
            } catch (IOException exception) {
                running.set(false);
            } catch (RuntimeException exception) {
                running.set(false);
            }
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);
        socket.close();

        if (receiverThread == null
                || receiverThread == Thread.currentThread()) {
            return;
        }

        try {
            receiverThread.join(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        receiverThread = null;
    }

    private static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "Port must be between 1 and 65535."
            );
        }
    }

    private static void validatePortAllowZero(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException(
                    "Local port must be between 0 and 65535."
            );
        }
    }
}
