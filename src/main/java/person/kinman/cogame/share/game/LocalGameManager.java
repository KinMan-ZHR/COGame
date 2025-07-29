// shared/game/LocalGameManager.java
package person.kinman.cogame.share.game;

public class LocalGameManager implements GameInterface {
    private GameLogic gameLogic;
    
    public LocalGameManager() {
        this.gameLogic = new GameLogic();
    }
    
    @Override
    public void initGame() {

    }
    
    @Override
    public void movePlayer(int playerId, int direction) {
        gameLogic.movePlayer(playerId, direction);
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

    // 其他接口方法实现...
}