package person.kinman.cogame.server.room;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.net.WsMessage;
import person.kinman.cogame.core.rule.GameEngine;

/**
 * 联机对战房间实例
 */
public class GameRoom {
    private static final Logger logger = LoggerFactory.getLogger(GameRoom.class);

    private final String roomId;
    private WebSocket p1Conn;
    private WebSocket p2Conn;
    private String p1Name = "玩家1";
    private String p2Name = "玩家2";
    private final GameState state;

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.state = new GameState();
    }

    public synchronized boolean addPlayer(WebSocket conn, String playerName) {
        return addPlayer(conn, playerName, 6);
    }

    public synchronized boolean addPlayer(WebSocket conn, String playerName, int requestedBoardSize) {
        if (p1Conn == null || p1Conn.isClosed()) {
            p1Conn = conn;
            if (playerName != null && !playerName.trim().isEmpty()) {
                p1Name = playerName;
            }
            if (requestedBoardSize >= 6 && requestedBoardSize <= 13) {
                state.setRows(requestedBoardSize);
                state.setCols(requestedBoardSize);
                state.reset();
            }
            state.getP1().setName(p1Name);
            logger.info("玩家1 [{}] 加入房间 [{}] (棋盘: {}x{})", p1Name, roomId, state.getRows(), state.getCols());

            // 通知玩家1已就绪，等待对手
            WsMessage waitMsg = new WsMessage(WsMessage.TYPE_ROOM_INFO);
            waitMsg.setRoomId(roomId);
            waitMsg.setMessage("已加入房间，棋盘规格 " + state.getRows() + "x" + state.getCols() + "，等待对手连接...");
            waitMsg.setAssignedPlayerId(1);
            waitMsg.setBoardSize(state.getRows());
            conn.send(waitMsg.toJson());
            return true;
        } else if (p2Conn == null || p2Conn.isClosed()) {
            p2Conn = conn;
            if (playerName != null && !playerName.trim().isEmpty()) {
                p2Name = playerName;
            }
            state.getP2().setName(p2Name);
            logger.info("玩家2 [{}] 加入房间 [{}]，游戏开始！", p2Name, roomId);

            // 双方到齐，重置棋盘并向双方广播 GAME_START
            state.reset();
            state.getP1().setName(p1Name);
            state.getP2().setName(p2Name);

            // 给P1发开始消息
            WsMessage startMsgP1 = WsMessage.gameStart(roomId, 1, state);
            p1Conn.send(startMsgP1.toJson());

            // 给P2发开始消息
            WsMessage startMsgP2 = WsMessage.gameStart(roomId, 2, state);
            p2Conn.send(startMsgP2.toJson());
            return true;
        } else {
            // 房间已满
            conn.send(WsMessage.error("房间 [" + roomId + "] 已满！").toJson());
            return false;
        }
    }

    public synchronized void handleAction(WebSocket conn, GameAction action) {
        int playerId = getPlayerId(conn);
        if (playerId == 0) {
            conn.send(WsMessage.error("未认证的玩家连接！").toJson());
            return;
        }

        if (action.getType() == GameAction.Type.RESET) {
            state.reset();
            state.getP1().setName(p1Name);
            state.getP2().setName(p2Name);
            broadcast(WsMessage.stateUpdate(state));
            return;
        }

        if (state.isOver()) {
            conn.send(WsMessage.error("对局已结束！").toJson());
            return;
        }

        if (state.getCurrentTurn() != playerId) {
            conn.send(WsMessage.error("当前是对手的回合，请稍候！").toJson());
            return;
        }

        boolean success = GameEngine.executeAction(state, playerId, action);
        if (success) {
            if (state.isOver()) {
                logger.info("房间 [{}] 对战结束: {}", roomId, state.getWinReason());
                broadcast(WsMessage.gameOver(state));
            } else {
                broadcast(WsMessage.stateUpdate(state));
            }
        }
    }

    public synchronized void removePlayer(WebSocket conn) {
        if (conn == p1Conn) {
            logger.info("玩家1离开房间 [{}]", roomId);
            p1Conn = null;
            if (p2Conn != null && p2Conn.isOpen()) {
                p2Conn.send(WsMessage.playerLeft("对手已离开房间！").toJson());
            }
        } else if (conn == p2Conn) {
            logger.info("玩家2离开房间 [{}]", roomId);
            p2Conn = null;
            if (p1Conn != null && p1Conn.isOpen()) {
                p1Conn.send(WsMessage.playerLeft("对手已离开房间！").toJson());
            }
        }
    }

    public synchronized boolean isEmpty() {
        boolean p1Dead = (p1Conn == null || p1Conn.isClosed());
        boolean p2Dead = (p2Conn == null || p2Conn.isClosed());
        return p1Dead && p2Dead;
    }

    public int getPlayerId(WebSocket conn) {
        if (conn == p1Conn) return 1;
        if (conn == p2Conn) return 2;
        return 0;
    }

    private void broadcast(WsMessage message) {
        String json = message.toJson();
        if (p1Conn != null && p1Conn.isOpen()) {
            p1Conn.send(json);
        }
        if (p2Conn != null && p2Conn.isOpen()) {
            p2Conn.send(json);
        }
    }

    public String getRoomId() {
        return roomId;
    }
}
