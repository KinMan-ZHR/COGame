package person.kinman.cogame.share.game;


import person.kinman.cogame.client.network.GameClient;
import person.kinman.cogame.common.GameMessage;

public class NetworkGameManager implements GameInterface {
    private final GameClient client;
    private GameLogic localGameLogic; // 本地缓存的游戏状态
    
    public NetworkGameManager(GameClient client) {
        this.client = client;
        this.localGameLogic = new GameLogic();
    }
    
    @Override
    public void initGame() {
        // 联网模式下由服务器初始化游戏
        client.sendMessage(new GameMessage(GameMessage.Type.INIT, -1, null));
    }
    
    @Override
    public void movePlayer(int playerId, int direction) {
        // 发送移动请求到服务器
        client.sendMessage(new GameMessage(GameMessage.Type.MOVE, playerId, direction));
    }

    @Override
    public void rotatePlayer(int playerId) {

    }

    @Override
    public void lock(int playerId, int gridX, int gridY) {

    }

    @Override
    public boolean isGameOver() {
        return false;
    }

    @Override
    public int getWinner() {
        return 0;
    }

}