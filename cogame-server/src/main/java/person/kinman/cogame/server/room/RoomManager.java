package person.kinman.cogame.server.room;

import org.java_websocket.WebSocket;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间全局管理器
 */
public class RoomManager {
    private final ConcurrentHashMap<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocket, GameRoom> playerRoomMap = new ConcurrentHashMap<>();

    public GameRoom getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, GameRoom::new);
    }

    public void bindPlayer(WebSocket conn, GameRoom room) {
        playerRoomMap.put(conn, room);
    }

    public GameRoom getRoomByPlayer(WebSocket conn) {
        return playerRoomMap.get(conn);
    }

    public void unbindPlayer(WebSocket conn) {
        GameRoom room = playerRoomMap.remove(conn);
        if (room != null) {
            room.removePlayer(conn);
            if (room.isEmpty()) {
                rooms.remove(room.getRoomId());
            }
        }
    }
}
