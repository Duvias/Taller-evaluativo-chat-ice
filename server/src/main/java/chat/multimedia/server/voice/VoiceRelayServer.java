package chat.multimedia.server.voice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VoiceRelayServer implements AutoCloseable {

    private static final int MAX_PACKET_SIZE = 8192;

    private final int requestedPort;
    private final Map<String, Map<String, InetSocketAddress>> rooms =
            new ConcurrentHashMap<>();
    private final Map<SocketAddress, String> roomByEndpoint =
            new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private DatagramSocket socket;
    private Thread receiverThread;

    public VoiceRelayServer(int requestedPort) {
        if (requestedPort < 0 || requestedPort > 65535) {
            throw new IllegalArgumentException(
                    "Relay port must be between 0 and 65535."
            );
        }

        this.requestedPort = requestedPort;
    }

    public synchronized void start() throws SocketException {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            socket = new DatagramSocket(requestedPort);
        } catch (SocketException exception) {
            running.set(false);
            throw exception;
        }

        receiverThread = new Thread(
                this::receiveLoop,
                "voice-group-relay"
        );
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public synchronized int getLocalPort() {
        if (socket == null) {
            return requestedPort;
        }

        return socket.getLocalPort();
    }

    public void register(
            String roomName,
            String nickname,
            String ip,
            int port
    ) {
        InetSocketAddress endpoint = new InetSocketAddress(ip, port);

        rooms.computeIfAbsent(
                roomName,
                key -> new ConcurrentHashMap<>()
        ).put(nickname, endpoint);

        roomByEndpoint.put(endpoint, roomName);
    }

    public void unregister(
            String roomName,
            String nickname
    ) {
        Map<String, InetSocketAddress> endpoints = rooms.get(roomName);

        if (endpoints == null) {
            return;
        }

        InetSocketAddress endpoint = endpoints.remove(nickname);

        if (endpoint != null) {
            roomByEndpoint.remove(endpoint);
        }

        if (endpoints.isEmpty()) {
            rooms.remove(roomName, endpoints);
        }
    }

    public void removeRoom(String roomName) {
        Map<String, InetSocketAddress> endpoints = rooms.remove(roomName);

        if (endpoints == null) {
            return;
        }

        for (InetSocketAddress endpoint : endpoints.values()) {
            roomByEndpoint.remove(endpoint);
        }
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

                String roomName = roomByEndpoint.get(
                        packet.getSocketAddress()
                );

                if (roomName == null) {
                    continue;
                }

                Map<String, InetSocketAddress> endpoints =
                        rooms.get(roomName);

                if (endpoints == null) {
                    continue;
                }

                for (InetSocketAddress endpoint : endpoints.values()) {
                    if (endpoint.equals(packet.getSocketAddress())) {
                        continue;
                    }

                    DatagramPacket forwarded = new DatagramPacket(
                            packet.getData(),
                            packet.getOffset(),
                            packet.getLength(),
                            endpoint
                    );

                    socket.send(forwarded);
                }
            } catch (SocketException exception) {
                if (running.get()) {
                    running.set(false);
                }
            } catch (IOException exception) {
                running.set(false);
            }
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);

        if (socket != null) {
            socket.close();
            socket = null;
        }

        rooms.clear();
        roomByEndpoint.clear();

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
}
