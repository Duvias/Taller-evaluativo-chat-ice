package chat.multimedia.server;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServerIceSmokeTest {

    @Test
    void registersChatServiceInObjectAdapter() {
        try (Communicator communicator = new Communicator(new String[0])) {
            ObjectAdapter adapter = Server.createAdapter(communicator);

            assertNotNull(adapter);
            assertEquals(Server.ADAPTER_NAME, adapter.getName());
        }
    }
}
