package chat.multimedia.server;

import chat.multimedia.common.NetworkConfig;
import chat.multimedia.server.service.ChatServiceImpl;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;

public final class Server {

    static final String ADAPTER_NAME = "ChatAdapter";
    static final Identity CHAT_SERVICE_IDENTITY = new Identity("chatService", "");

    private Server() {
    }

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();

        // Ice 3.8 for Java exposes public Communicator constructors.
        try (Communicator communicator = new Communicator(args)) {
            ObjectAdapter adapter = createAdapter(communicator);
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

    static ObjectAdapter createAdapter(Communicator communicator) {
        ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                ADAPTER_NAME,
                "tcp -h 0.0.0.0 -p " + NetworkConfig.ICE_PORT
        );

        adapter.add(
                new ChatServiceImpl(),
                CHAT_SERVICE_IDENTITY
        );

        return adapter;
    }
}
