package person.kinman.cogame.core.net;

import com.google.gson.Gson;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;

import java.util.List;

/**
 * WebSocket 通信消息载荷 (支持登录认证、房间列表、加锁房间与随机匹配)
 */
public class WsMessage {
    // 登录认证协议
    public static final String TYPE_LOGIN = "LOGIN";
    public static final String TYPE_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String TYPE_LOGIN_FAIL = "LOGIN_FAIL";

    // 房间大厅协议
    public static final String TYPE_LIST_ROOMS = "LIST_ROOMS";
    public static final String TYPE_ROOMS_LIST = "ROOMS_LIST";
    public static final String TYPE_CREATE_ROOM = "CREATE_ROOM";
    public static final String TYPE_JOIN_ROOM = "JOIN_ROOM";
    public static final String TYPE_RANDOM_JOIN = "RANDOM_JOIN";
    public static final String TYPE_SET_PREFERENCE = "SET_PREFERENCE";
    public static final String TYPE_ROOM_INFO = "ROOM_INFO";

    // 对战游戏协议
    public static final String TYPE_GAME_START = "GAME_START";
    public static final String TYPE_ACTION = "ACTION";
    public static final String TYPE_STATE_UPDATE = "STATE_UPDATE";
    public static final String TYPE_GAME_OVER = "GAME_OVER";
    public static final String TYPE_PLAYER_LEFT = "PLAYER_LEFT";
    public static final String TYPE_ERROR = "ERROR";

    private String type;
    private String roomId;
    private String playerName;
    private String password;
    private String turnPreference; // "FIRST", "SECOND", "RANDOM"
    private int assignedPlayerId; // 1 or 2
    private int boardSize = 6;
    private GameAction action;
    private GameState state;
    private String message;
    private List<RoomSummaryDto> rooms;

    private static final Gson gson = new Gson();

    public WsMessage() {}

    public WsMessage(String type) {
        this.type = type;
    }

    public static WsMessage login(String playerName) {
        WsMessage msg = new WsMessage(TYPE_LOGIN);
        msg.playerName = playerName;
        return msg;
    }

    public static WsMessage loginSuccess(String playerName) {
        WsMessage msg = new WsMessage(TYPE_LOGIN_SUCCESS);
        msg.playerName = playerName;
        msg.message = "登录成功！欢迎来到 COGame 对战世界";
        return msg;
    }

    public static WsMessage loginFail(String message) {
        WsMessage msg = new WsMessage(TYPE_LOGIN_FAIL);
        msg.message = message;
        return msg;
    }

    public static WsMessage listRooms() {
        return new WsMessage(TYPE_LIST_ROOMS);
    }

    public static WsMessage roomsList(List<RoomSummaryDto> rooms) {
        WsMessage msg = new WsMessage(TYPE_ROOMS_LIST);
        msg.rooms = rooms;
        return msg;
    }

    public static WsMessage createRoom(String roomId, String playerName, int boardSize, String password) {
        return createRoom(roomId, playerName, boardSize, password, "RANDOM");
    }

    public static WsMessage createRoom(String roomId, String playerName, int boardSize, String password, String turnPreference) {
        WsMessage msg = new WsMessage(TYPE_CREATE_ROOM);
        msg.roomId = roomId;
        msg.playerName = playerName;
        msg.boardSize = boardSize;
        msg.password = password;
        msg.turnPreference = turnPreference;
        return msg;
    }

    public static WsMessage joinRoom(String roomId, String playerName) {
        return joinRoom(roomId, playerName, 6, null, "RANDOM");
    }

    public static WsMessage joinRoom(String roomId, String playerName, int boardSize) {
        return joinRoom(roomId, playerName, boardSize, null, "RANDOM");
    }

    public static WsMessage joinRoom(String roomId, String playerName, int boardSize, String password) {
        return joinRoom(roomId, playerName, boardSize, password, "RANDOM");
    }

    public static WsMessage joinRoom(String roomId, String playerName, int boardSize, String password, String turnPreference) {
        WsMessage msg = new WsMessage(TYPE_JOIN_ROOM);
        msg.roomId = roomId;
        msg.playerName = playerName;
        msg.boardSize = boardSize;
        msg.password = password;
        msg.turnPreference = turnPreference;
        return msg;
    }

    public static WsMessage randomJoin(String playerName) {
        return randomJoin(playerName, "RANDOM");
    }

    public static WsMessage randomJoin(String playerName, String turnPreference) {
        WsMessage msg = new WsMessage(TYPE_RANDOM_JOIN);
        msg.playerName = playerName;
        msg.turnPreference = turnPreference;
        return msg;
    }

    public static WsMessage setPreference(String roomId, String turnPreference) {
        WsMessage msg = new WsMessage(TYPE_SET_PREFERENCE);
        msg.roomId = roomId;
        msg.turnPreference = turnPreference;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTurnPreference() {
        return turnPreference;
    }

    public void setTurnPreference(String turnPreference) {
        this.turnPreference = turnPreference;
    }

    public int getAssignedPlayerId() {
        return assignedPlayerId;
    }

    public void setAssignedPlayerId(int assignedPlayerId) {
        this.assignedPlayerId = assignedPlayerId;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
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

    public List<RoomSummaryDto> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomSummaryDto> rooms) {
        this.rooms = rooms;
    }
}
