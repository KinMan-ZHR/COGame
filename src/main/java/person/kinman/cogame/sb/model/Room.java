package person.kinman.cogame.sb.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间模型类
 */
public class Room {
    private final int id;
    private String name;
    private final List<Player> players;
    private boolean isFull;
    private boolean isPlaying;
    
    public Room(int id, String name) {
        this.id = id;
        this.name = name;
        this.players = new ArrayList<>();
        this.isFull = false;
        this.isPlaying = false;
    }
    
    // 添加玩家
    public boolean addPlayer(Player player) {
        if (isFull) {
            return false;
        }
        
        players.add(player);
        isFull = players.size() >= 2; // 假设每个房间最多2人
        return true;
    }
    
    // 移除玩家
    public boolean removePlayer(String playerId) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId().equals(playerId)) {
                players.remove(i);
                isFull = false;
                return true;
            }
        }
        return false;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public List<Player> getPlayers() {
        return new ArrayList<>(players); // 返回副本，防止外部修改
    }
    
    public boolean isFull() {
        return isFull;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
}
