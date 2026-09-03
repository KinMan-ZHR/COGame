package person.kinman.cogame.core.net;

import com.google.gson.Gson;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;

/**
 * WebSocket 通信消息载荷
 */
public class WsMessage {
    public static final String TYPE_JOIN_ROOM = "JOIN_ROOM";
    public static final String TYPE_ROOM_INFO = "ROOM_INFO";
    public static final String TYPE_GAME_START = "GAME_START";
    public static final String TYPE_ACTION = "ACTION";
    public static final String TYPE_STATE_UPDATE = "STATE_UPDATE";
    public static final String TYPE_GAME_OVER = "GAME_OVER";
    public static final String TYPE_PLAYER_LEFT = "PLAYER_LEFT";
    public static final String TYPE_ERROR = "ERROR";

    private String type;
    private String roomId;
    private String playerName;
    private int assignedPlayerId; // 1 or 2
    private GameAction action;
    private GameState state;
    private String message;

    private static final Gson gson = new Gson();

    public WsMessage() {}

    public WsMessage(String type) {
        this.type = type;
    }

    public static WsMessage joinRoom(String roomId, String playerName) {
        WsMessage msg = new WsMessage(TYPE_JOIN_ROOM);
        msg.roomId = roomId;
        msg.playerName = playerName;
        return msg;
    }

    public static WsMessage gameStart(String roomId, int assignedPlayerId, GameState state) {
        WsMessage msg = new WsMessage(TYPE_GAME_START);
        msg.roomId = roomId;
        msg.assignedPlayerId = assignedPlayerId;
        msg.state = state;
        return msg;
    }

    public static WsMessage action(GameAction action) {
        WsMessage msg = new WsMessage(TYPE_ACTION);
        msg.action = action;
        return msg;
    }

    public static WsMessage stateUpdate(GameState state) {
        WsMessage msg = new WsMessage(TYPE_STATE_UPDATE);
        msg.state = state;
        return msg;
    }

    public static WsMessage gameOver(GameState state) {
        WsMessage msg = new WsMessage(TYPE_GAME_OVER);
        msg.state = state;
        return msg;
    }

    public static WsMessage error(String message) {
        WsMessage msg = new WsMessage(TYPE_ERROR);
        msg.message = message;
        return msg;
    }

    public static WsMessage playerLeft(String message) {
        WsMessage msg = new WsMessage(TYPE_PLAYER_LEFT);
        msg.message = message;
        return msg;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static WsMessage fromJson(String json) {
        return gson.fromJson(json, WsMessage.class);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getAssignedPlayerId() {
        return assignedPlayerId;
    }

    public void setAssignedPlayerId(int assignedPlayerId) {
        this.assignedPlayerId = assignedPlayerId;
    }

    public GameAction getAction() {
        return action;
    }

    public void setAction(GameAction action) {
        this.action = action;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
