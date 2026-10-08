package chat.multimedia.client;

import com.zeroc.Ice.Communicator;
import distributed.chat.ChatServicePrx;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClientProxyTest {

    @Test
    void createsChatServiceProxyWithExpectedIdentity() {
        try (Communicator communicator = new Communicator(new String[0])) {
            ChatServicePrx proxy = Client.createChatServiceProxy(communicator);

            assertNotNull(proxy);
            assertEquals("chatService", proxy.ice_getIdentity().name);
        }
    }
}
