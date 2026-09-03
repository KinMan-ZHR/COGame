package person.kinman.cogame.server.room;

import org.java_websocket.WebSocket;
import person.kinman.cogame.core.net.RoomSummaryDto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间全局管理器 (支持大厅房间列表、随机匹配检索与房间生命周期)
 */
public class RoomManager {
    private final ConcurrentHashMap<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocket, GameRoom> playerRoomMap = new ConcurrentHashMap<>();

    public GameRoom getOrCreateRoom(String roomId, String password, int boardSize) {
        return rooms.computeIfAbsent(roomId, id -> new GameRoom(id, password, boardSize));
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
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

    /**
     * 随机寻找一个未满且未设置密码的等待中房间
     */
    public GameRoom findRandomAvailableRoom() {
        for (GameRoom room : rooms.values()) {
            if (room.isWaiting() && !room.hasPassword()) {
                return room;
            }
        }
        return null;
    }

    /**
     * 获取当前所有非空房间的大厅列表摘要
     */
    public List<RoomSummaryDto> listRooms() {
        List<RoomSummaryDto> list = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            if (!room.isEmpty()) {
                list.add(room.getSummary());
            }
        }
        return list;
    }

    public int getRoomCount() {
        return rooms.size();
    }
}
