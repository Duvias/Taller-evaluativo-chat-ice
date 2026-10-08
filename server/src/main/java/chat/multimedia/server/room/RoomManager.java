package chat.multimedia.server.room;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado de las salas.
 * Integrante 2 implementará creación, unión, salida, listado y broadcast.
 */
public class RoomManager {

    private final Map<String, Set<String>> rooms =
            new ConcurrentHashMap<>();

    public Map<String, Set<String>> rooms() {
        return rooms;
    }
}
