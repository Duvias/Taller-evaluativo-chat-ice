package chat.multimedia.server.voice;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GroupCallSession {

    private final String roomName;
    private final String host;
    private final Map<String, InetSocketAddress> endpoints =
            new ConcurrentHashMap<>();
    private final Set<String> participants =
            ConcurrentHashMap.newKeySet();

    public GroupCallSession(
            String roomName,
            String host
    ) {
        this.roomName = roomName;
        this.host = host;
        participants.add(host);
    }

    public String getRoomName() {
        return roomName;
    }

    public String getHost() {
        return host;
    }

    public void addParticipant(
            String nickname,
            InetSocketAddress endpoint
    ) {
        participants.add(nickname);
        endpoints.put(nickname, endpoint);
    }

    public void removeParticipant(String nickname) {
        participants.remove(nickname);
        endpoints.remove(nickname);
    }

    public boolean hasParticipant(String nickname) {
        return participants.contains(nickname);
    }

    public Set<String> participants() {
        return Set.copyOf(participants);
    }

    public Map<String, InetSocketAddress> endpoints() {
        return Map.copyOf(endpoints);
    }
}
