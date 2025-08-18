package person.kinman.cogame.client.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏数据模型类，存储游戏全局数据
 */
public class GameData {
    private Player currentPlayer;
    private List<Room> rooms;
    private ServerInfo serverInfo;
    private Room currentRoom;

    public GameData() {
        this.rooms = new ArrayList<>();
        this.serverInfo = new ServerInfo("localhost", 8888); // 默认服务器信息
    }

    // Getters and Setters
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public List<Room> getRooms() {
        return new ArrayList<>(rooms); // 返回副本，防止外部修改
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = new ArrayList<>(rooms);
    }

    public void addRoom(Room room) {
        this.rooms.add(room);
    }

    public void removeRoom(int roomId) {
        rooms.removeIf(room -> room.getId() == roomId);
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }
}
