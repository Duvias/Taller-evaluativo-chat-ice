package chat.multimedia.server.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RoomManager {

    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();

    public boolean createRoom(String roomName) {
        String name = normalizeRoomName(roomName);
        return rooms.putIfAbsent(name, ConcurrentHashMap.newKeySet()) == null;
    }

    public boolean joinRoom(String nickname, String roomName) {
        Set<String> members = rooms.get(normalizeRoomName(roomName));
        if (members == null) {
            return false;
        }
        members.add(normalizeNickname(nickname));
        return true;
    }

    public boolean leaveRoom(String nickname, String roomName) {
        Set<String> members = rooms.get(normalizeRoomName(roomName));
        if (members == null) {
            return false;
        }
        members.remove(normalizeNickname(nickname));
        return true;
    }

    public boolean roomExists(String roomName) {
        return rooms.containsKey(normalizeRoomName(roomName));
    }

    public boolean isMember(String nickname, String roomName) {
        Set<String> members = rooms.get(normalizeRoomName(roomName));
        return members != null && members.contains(normalizeNickname(nickname));
    }

    public Set<String> membersOf(String roomName) {
        Set<String> members = rooms.get(normalizeRoomName(roomName));
        return members == null ? Set.of() : Set.copyOf(members);
    }

    public String[] listRooms() {
        List<String> names = new ArrayList<>(rooms.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names.toArray(String[]::new);
    }

    public Map<String, Set<String>> rooms() {
        return Map.copyOf(rooms);
    }

    /** Removes a disconnected user from every room. */
    public void removeUserFromAllRooms(String nickname) {
        String name = normalizeNickname(nickname);
        rooms.values().forEach(members -> members.remove(name));
    }

    private static String normalizeRoomName(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            throw new IllegalArgumentException("El nombre de la sala no puede estar vacío.");
        }
        return roomName.trim();
    }

    private static String normalizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("El nickname no puede estar vacío.");
        }
        return nickname.trim();
    }
}
