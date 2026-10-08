package chat.multimedia.server;

import chat.multimedia.common.NetworkConfig;
import chat.multimedia.server.service.ChatServiceImpl;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;

public final class Server {

    private Server() {
    }

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();

        try (Communicator communicator = new Communicator(args)) {
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "ChatAdapter",
                    "tcp -h 0.0.0.0 -p " + NetworkConfig.ICE_PORT
            );

            adapter.add(
                    new ChatServiceImpl(),
                    new Identity("chatService", "")
            );

            adapter.activate();

            System.out.printf(
                    "Chat server listening on TCP port %d.%n",
                    NetworkConfig.ICE_PORT
            );

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down chat server...");
                communicator.shutdown();

                try {
                    mainThread.join();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }));

            communicator.waitForShutdown();
        }
    }
}
