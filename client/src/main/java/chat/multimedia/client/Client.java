package chat.multimedia.client;

import chat.multimedia.client.cli.Cli;
import chat.multimedia.common.NetworkConfig;
import com.zeroc.Ice.Communicator;
import distributed.chat.ChatServicePrx;

public final class Client {

    private Client() {
    }

    public static void main(String[] args) {
        try (Communicator communicator = new Communicator(args)) {
            ChatServicePrx chatService = ChatServicePrx.createProxy(
                    communicator,
                    NetworkConfig.chatServiceProxy()
            );

            System.out.println("Chat multimedia CLI");
            System.out.println("Proxy Ice: " + NetworkConfig.chatServiceProxy());

            new Cli(communicator, chatService).run();
        }
    }
}
