package chat.multimedia.server.user;

import distributed.chat.ClientCallbackPrx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado de usuarios conectados.
 * Integrante 1 implementará login, logout, presencia y chat privado.
 */
public class UserManager {

    private final Map<String, ClientCallbackPrx> connectedUsers =
            new ConcurrentHashMap<>();

    public Map<String, ClientCallbackPrx> connectedUsers() {
        return connectedUsers;
    }
}
