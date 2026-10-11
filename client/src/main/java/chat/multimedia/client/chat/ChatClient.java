package chat.multimedia.client.chat;

import distributed.chat.ChatServicePrx;

/**
 * Fachada para operaciones de chat del cliente.
 * Integrante 1 completará este módulo.
 */
public class ChatClient {

    private final ChatServicePrx chatService;

    public ChatClient(ChatServicePrx chatService) {
        this.chatService = chatService;
    }

    public ChatServicePrx chatService() {
        return chatService;
    }
}
