// shared/game/GameInterface.java
package person.kinman.cogame.share.game;

public interface GameInterface {
    void initGame();
    void movePlayer(int playerId, int direction);
    void rotatePlayer(int playerId);
    void lock(int playerId, int gridX, int gridY);
    boolean isGameOver();
    int getWinner();
}