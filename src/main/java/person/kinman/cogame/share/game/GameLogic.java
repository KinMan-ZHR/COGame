package person.kinman.cogame.share.game;

import person.kinman.cogame.GameMap;
import person.kinman.cogame.Player;

import java.util.ArrayList;
import java.util.List;

public class GameLogic {
    private GameMap map;
    private List<Player> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private boolean gameOver = false;
    
    public GameLogic() {
        this.map = new GameMap();
        initPlayers();
    }
    
    private void initPlayers() {
        // 初始化玩家
        players.add(new Player(1, "Player1"));
        players.add(new Player(2, "Player2"));
    }
    
    // 游戏逻辑方法
    public void movePlayer(int playerId, int direction) {
        Player player = getPlayerById(playerId);
        if (player != null && playerId == getCurrentPlayer().getId()) {
            // 处理移动逻辑
            player.move(direction);
            switchPlayer();
        }
    }
    
    // Getters and setters
    public GameMap getMap() { return map; }
    public List<Player> getPlayers() { return players; }
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
    public boolean isGameOver() { return gameOver; }
    
    // 其他游戏逻辑方法...
}