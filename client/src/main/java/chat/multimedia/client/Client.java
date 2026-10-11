package chat.multimedia.client;

import chat.multimedia.client.cli.Cli;
import chat.multimedia.client.voice.VoiceClient;
import chat.multimedia.common.NetworkConfig;
import com.zeroc.Ice.Communicator;
import distributed.chat.ChatServicePrx;

public final class Client {

    private Client() {
    }

    public static void main(String[] args) {
        try (Communicator communicator = new Communicator(args)) {
            ChatServicePrx chatService = createChatServiceProxy(communicator);

            System.out.println("Chat multimedia CLI");
            System.out.println("Proxy Ice: " + NetworkConfig.chatServiceProxy());

            try (VoiceClient voiceClient = new VoiceClient(chatService)) {
                new Cli(
                        communicator,
                        chatService,
                        voiceClient
                ).run();
            }
        }
    }

    static ChatServicePrx createChatServiceProxy(Communicator communicator) {
        return ChatServicePrx.createProxy(
                communicator,
                NetworkConfig.chatServiceProxy()
        );
    }
}
