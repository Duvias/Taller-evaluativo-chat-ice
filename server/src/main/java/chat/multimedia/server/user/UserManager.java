
package chat.multimedia.server.user;

import distributed.chat.ClientCallbackPrx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Administra los usuarios conectados y sus callbacks.
 */
public class UserManager {

    private final Map<String, ClientCallbackPrx> connectedUsers =
            new ConcurrentHashMap<>();

    /**
     * Registra un usuario si su nickname no está ocupado.
     *
     * @return true si se registró; false si ya existía.
     */
    public boolean register(
            String nickname,
            ClientCallbackPrx callback
    ) {
        return connectedUsers.putIfAbsent(nickname, callback) == null;
    }

    /**
     * Elimina un usuario y devuelve su callback.
     */
    public ClientCallbackPrx remove(String nickname) {
        return connectedUsers.remove(nickname);
    }

    /**
     * Busca el callback de un usuario conectado.
     */
    public ClientCallbackPrx find(String nickname) {
        return connectedUsers.get(nickname);
    }

    /**
     * Comprueba si un usuario está conectado.
     */
    public boolean isConnected(String nickname) {
        return connectedUsers.containsKey(nickname);
    }

    /**
     * Obtiene los nicknames conectados en orden alfabético.
     */
    public List<String> getNicknames() {
        List<String> nicknames =
                new ArrayList<>(connectedUsers.keySet());

        nicknames.sort(String.CASE_INSENSITIVE_ORDER);

        return nicknames;
    }

    /**
     * Permite que otros componentes del servidor
     * consulten los callbacks registrados.
     */
    public Map<String, ClientCallbackPrx> connectedUsers() {
        return connectedUsers;
    }
}
