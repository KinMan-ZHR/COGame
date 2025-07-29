package person.kinman.cogame.common;

import java.io.Serializable;

public class GameMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Type {
        INIT, CONNECT, DISCONNECT, MOVE, ROTATE, LOCK, SYNC, ERROR
    }
    
    private final Type type;
    private final int playerId;
    private final Object data;
    
    public GameMessage(Type type, int playerId, Object data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
    }
    
    // Getters
    public Type getType() { return type; }
    public int getPlayerId() { return playerId; }
    public Object getData() { return data; }
}