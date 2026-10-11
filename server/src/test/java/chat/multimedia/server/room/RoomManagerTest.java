package chat.multimedia.server.room;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomManagerTest {

    @Test
    void createsRoomsAndRejectsDuplicateNames() {
        RoomManager manager = new RoomManager();

        assertTrue(manager.createRoom("general"));
        assertFalse(manager.createRoom("general"));
        assertArrayEquals(new String[]{"general"}, manager.listRooms());
    }

    @Test
    void tracksMembershipWithoutMixingRooms() {
        RoomManager manager = new RoomManager();
        manager.createRoom("general");
        manager.createRoom("games");

        assertTrue(manager.joinRoom("ana", "general"));
        assertTrue(manager.joinRoom("luis", "games"));
        assertTrue(manager.isMember("ana", "general"));
        assertFalse(manager.isMember("ana", "games"));
        assertEquals(1, manager.membersOf("general").size());
        assertEquals(1, manager.membersOf("games").size());
    }

    @Test
    void removesMembershipAndCanCleanUpDisconnectedUsers() {
        RoomManager manager = new RoomManager();
        manager.createRoom("general");
        manager.createRoom("games");
        manager.joinRoom("ana", "general");
        manager.joinRoom("ana", "games");

        assertTrue(manager.leaveRoom("ana", "general"));
        assertFalse(manager.isMember("ana", "general"));
        assertTrue(manager.isMember("ana", "games"));

        manager.removeUserFromAllRooms("ana");
        assertFalse(manager.isMember("ana", "games"));
    }

    @Test
    void joiningUnknownRoomFailsAndRoomNamesAreSorted() {
        RoomManager manager = new RoomManager();
        manager.createRoom("zeta");
        manager.createRoom("alpha");

        assertFalse(manager.joinRoom("ana", "missing"));
        assertArrayEquals(new String[]{"alpha", "zeta"}, manager.listRooms());
    }
}
