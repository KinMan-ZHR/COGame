package person.kinman.cogame.server.room;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.net.RoomSummaryDto;
import person.kinman.cogame.core.net.WsMessage;
import person.kinman.cogame.core.rule.GameEngine;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 联机对战房间实例 (支持密码保护、动态棋盘规格、观战列表预留与大厅状态广播)
 */
public class GameRoom {
    private static final Logger logger = LoggerFactory.getLogger(GameRoom.class);

    private final String roomId;
    private String password; // 密码，null 或空表示公开无密码
    private WebSocket p1Conn;
    private WebSocket p2Conn;
    private String p1Name = "玩家1";
    private String p2Name = "玩家2";
    private person.kinman.cogame.core.model.TurnOrderPreference p1Preference = person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
    private person.kinman.cogame.core.model.TurnOrderPreference p2Preference = person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
    private boolean p1RematchReady = false;
    private boolean p2RematchReady = false;
    private person.kinman.cogame.core.model.TurnOrderPreference p1RematchPref = person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
    private person.kinman.cogame.core.model.TurnOrderPreference p2RematchPref = person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
    private final GameState state;

    // 预留 v2.5 观战者会话集合
    private final CopyOnWriteArraySet<WebSocket> spectators = new CopyOnWriteArraySet<>();

    public GameRoom(String roomId) {
        this(roomId, null, 6);
    }

    public GameRoom(String roomId, String password, int boardSize) {
        this.roomId = roomId;
        this.password = (password != null && !password.trim().isEmpty()) ? password.trim() : null;
        int size = Math.max(6, Math.min(13, boardSize));
        this.state = new GameState(size);
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    public boolean checkPassword(String candidate) {
        if (!hasPassword()) return true;
        if (candidate == null) return false;
        return password.equals(candidate.trim());
    }

    public void setPassword(String password) {
        this.password = (password != null && !password.trim().isEmpty()) ? password.trim() : null;
    }

    public synchronized void setPlayerPreference(WebSocket conn, String preference) {
        person.kinman.cogame.core.model.TurnOrderPreference pref = person.kinman.cogame.core.model.TurnOrderPreference.fromCode(preference);
        if (conn == p1Conn) {
            p1Preference = pref;
            logger.info("房间 [{}] 房主 [{}] 更新分先意愿为: {}", roomId, p1Name, pref.getDisplayName());
        } else if (conn == p2Conn) {
            p2Preference = pref;
            logger.info("房间 [{}] 玩家2 [{}] 更新分先意愿为: {}", roomId, p2Name, pref.getDisplayName());
        }
    }

    public synchronized boolean addPlayer(WebSocket conn, String playerName, int requestedBoardSize, String candidatePassword) {
        return addPlayer(conn, playerName, requestedBoardSize, candidatePassword, "RANDOM");
    }

    public synchronized boolean addPlayer(WebSocket conn, String playerName, int requestedBoardSize, String candidatePassword, String turnPreference) {
        // 校验密码
        if (!checkPassword(candidatePassword)) {
            logger.warn("玩家 [{}] 加入房间 [{}] 密码错误", playerName, roomId);
            conn.send(WsMessage.error("房间密码错误，加入失败！").toJson());
            return false;
        }

        if (p1Conn == null || p1Conn.isClosed()) {
            p1Conn = conn;
            if (playerName != null && !playerName.trim().isEmpty()) {
                p1Name = playerName.trim();
            }
            p1Preference = person.kinman.cogame.core.model.TurnOrderPreference.fromCode(turnPreference);
            if (requestedBoardSize >= 6 && requestedBoardSize <= 13) {
                state.setRows(requestedBoardSize);
                state.setCols(requestedBoardSize);
                state.reset();
            }
            state.getP1().setName(p1Name);
            logger.info("玩家1 (房主) [{}] 加入房间 [{}] (棋盘: {}x{}, 加锁: {}, 分先意愿: {})",
                    p1Name, roomId, state.getRows(), state.getCols(), hasPassword(), p1Preference.getDisplayName());

            // 通知玩家1已就绪，等待对手
            WsMessage waitMsg = new WsMessage(WsMessage.TYPE_ROOM_INFO);
            waitMsg.setRoomId(roomId);
            waitMsg.setMessage("房间 [" + roomId + "] 创建成功，棋盘规格 " + state.getRows() + "x" + state.getCols() + "，等待对手加入...");
            waitMsg.setAssignedPlayerId(1);
            waitMsg.setBoardSize(state.getRows());
            conn.send(waitMsg.toJson());
            return true;
        } else if (p2Conn == null || p2Conn.isClosed()) {
            String guestName = (playerName != null && !playerName.trim().isEmpty()) ? playerName.trim() : "玩家2";
            p2Preference = person.kinman.cogame.core.model.TurnOrderPreference.fromCode(turnPreference);

            logger.info("玩家 [{}] 加入房间 [{}]，分先意愿: {} (房主意愿: {})",
                    guestName, roomId, p2Preference.getDisplayName(), p1Preference.getDisplayName());

            // 分先仲裁：双方均选先手则掷骰，或根据互补意愿判定
            int chosenFirst = person.kinman.cogame.core.model.TurnOrderPreference.resolveFirstPlayer(p1Preference, p2Preference, new java.util.Random());
            String desc = person.kinman.cogame.core.model.TurnOrderPreference.getResolutionDescription(p1Name, p1Preference, guestName, p2Preference, chosenFirst);

            if (chosenFirst == 1) {
                // 房主为 P1(先手)，挑战者为 P2(后手)
                p2Conn = conn;
                p2Name = guestName;
            } else {
                // 挑战者为 P1(先手)，原房主转为 P2(后手)
                WebSocket hostConn = p1Conn;
                String hostName = p1Name;
                person.kinman.cogame.core.model.TurnOrderPreference hostPref = p1Preference;

                p1Conn = conn;
                p1Name = guestName;
                p1Preference = p2Preference;

                p2Conn = hostConn;
                p2Name = hostName;
                p2Preference = hostPref;
            }

            // 双方就绪，重置棋盘并向双方广播带有仲裁通知的 GAME_START
            state.reset();
            state.getP1().setName(p1Name);
            state.getP2().setName(p2Name);

            logger.info("房间 [{}] 对局开战！分配结果: P1(先手)={}, P2(后手)={} [{}]", roomId, p1Name, p2Name, desc);

            // 给 P1 发送开始消息
            WsMessage startMsgP1 = WsMessage.gameStart(roomId, 1, state);
            startMsgP1.setMessage(desc);
            p1Conn.send(startMsgP1.toJson());

            // 给 P2 发送开始消息
            WsMessage startMsgP2 = WsMessage.gameStart(roomId, 2, state);
            startMsgP2.setMessage(desc);
            p2Conn.send(startMsgP2.toJson());

            // 向观战者广播对局开始
            broadcastToSpectators(WsMessage.stateUpdate(state));
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
            handleRematchRequest(conn, "RANDOM");
            return;
        }

        if (state.isOver()) {
            conn.send(WsMessage.error("对局已结束，请点击【新一局】重新分先开战！").toJson());
            return;
        }

        boolean ok = GameEngine.executeAction(state, playerId, action);
        if (ok) {
            if (state.isOver()) {
                logger.info("房间 [{}] 对局决出胜负: 胜者={}, 原因={}", roomId, state.getWinner(), state.getWinReason());
                broadcast(WsMessage.gameOver(state));
            } else {
                broadcast(WsMessage.stateUpdate(state));
            }
        } else {
            conn.send(WsMessage.error("无效的行动操作！").toJson());
        }
    }

    public synchronized void handleRematchRequest(WebSocket conn, String turnPreferenceCode) {
        int playerId = getPlayerId(conn);
        if (playerId == 0) return;

        person.kinman.cogame.core.model.TurnOrderPreference pref =
                person.kinman.cogame.core.model.TurnOrderPreference.fromCode(turnPreferenceCode);
        if (playerId == 1) {
            p1RematchReady = true;
            p1RematchPref = pref;
            logger.info("房间 [{}] 玩家1 [{}] 准备新一局，分先意愿: {}", roomId, p1Name, pref.getDisplayName());
        } else {
            p2RematchReady = true;
            p2RematchPref = pref;
            logger.info("房间 [{}] 玩家2 [{}] 准备新一局，分先意愿: {}", roomId, p2Name, pref.getDisplayName());
        }

        // 如果只有一位玩家在房间内，直接重置
        if (getPlayerCount() == 1) {
            state.reset();
            state.getP1().setName(p1Name);
            p1RematchReady = false;
            p2RematchReady = false;
            broadcast(WsMessage.stateUpdate(state));
            return;
        }

        // 双方均已就绪新一局，开始局内分先仲裁！
        if (p1RematchReady && p2RematchReady) {
            int chosenFirst = person.kinman.cogame.core.model.TurnOrderPreference.resolveFirstPlayer(
                    p1RematchPref, p2RematchPref, new java.util.Random());
            String desc = person.kinman.cogame.core.model.TurnOrderPreference.getResolutionDescription(
                    p1Name, p1RematchPref, p2Name, p2RematchPref, chosenFirst);

            if (chosenFirst == 2) {
                // 原 P2 获胜执先，成为新局 P1
                WebSocket tempConn = p1Conn;
                String tempName = p1Name;

                p1Conn = p2Conn;
                p1Name = p2Name;
                p1Preference = p2RematchPref;

                p2Conn = tempConn;
                p2Name = tempName;
                p2Preference = p1RematchPref;
            } else {
                p1Preference = p1RematchPref;
                p2Preference = p2RematchPref;
            }

            p1RematchReady = false;
            p2RematchReady = false;

            state.reset();
            state.getP1().setName(p1Name);
            state.getP2().setName(p2Name);

            logger.info("房间 [{}] 新一局开战！分配结果: P1(先手)={}, P2(后手)={} [{}]", roomId, p1Name, p2Name, desc);

            if (p1Conn != null && p1Conn.isOpen()) {
                WsMessage startMsgP1 = WsMessage.gameStart(roomId, 1, state);
                startMsgP1.setMessage("⚔️ 新一局开战！" + desc);
                p1Conn.send(startMsgP1.toJson());
            }

            if (p2Conn != null && p2Conn.isOpen()) {
                WsMessage startMsgP2 = WsMessage.gameStart(roomId, 2, state);
                startMsgP2.setMessage("⚔️ 新一局开战！" + desc);
                p2Conn.send(startMsgP2.toJson());
            }

            broadcastToSpectators(WsMessage.stateUpdate(state));
        } else {
            String readyPlayer = (playerId == 1) ? p1Name : p2Name;
            String waitingMsg = "玩家 [" + readyPlayer + "] 已就绪新一局 (分先: " + pref.getDisplayName() + ")，等待对手就绪...";
            WsMessage infoMsg = WsMessage.rematchInfo(waitingMsg);
            broadcast(infoMsg);
        }
    }

    public synchronized void removePlayer(WebSocket conn) {
        p1RematchReady = false;
        p2RematchReady = false;
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
        spectators.remove(conn);
    }

    public int getPlayerCount() {
        int count = 0;
        if (p1Conn != null && p1Conn.isOpen()) count++;
        if (p2Conn != null && p2Conn.isOpen()) count++;
        return count;
    }

    public synchronized boolean isWaiting() {
        return getPlayerCount() == 1 && !state.isOver();
    }

    public synchronized boolean isEmpty() {
        return getPlayerCount() == 0 && spectators.isEmpty();
    }

    public int getPlayerId(WebSocket conn) {
        if (conn == p1Conn) return 1;
        if (conn == p2Conn) return 2;
        return 0;
    }

    public RoomSummaryDto getSummary() {
        String status = isWaiting() ? "WAITING" : (getPlayerCount() == 2 ? "PLAYING" : "EMPTY");
        return new RoomSummaryDto(
                roomId,
                p1Name,
                state.getRows(),
                getPlayerCount(),
                hasPassword(),
                status
        );
    }

    public void addSpectator(WebSocket conn) {
        spectators.add(conn);
        WsMessage initMsg = WsMessage.stateUpdate(state);
        initMsg.setMessage("您正在以观战者身份观看对局");
        conn.send(initMsg.toJson());
    }

    public void removeSpectator(WebSocket conn) {
        spectators.remove(conn);
    }

    private void broadcast(WsMessage message) {
        String json = message.toJson();
        if (p1Conn != null && p1Conn.isOpen()) {
            p1Conn.send(json);
        }
        if (p2Conn != null && p2Conn.isOpen()) {
            p2Conn.send(json);
        }
        broadcastToSpectators(message);
    }

    private void broadcastToSpectators(WsMessage message) {
        if (spectators.isEmpty()) return;
        String json = message.toJson();
        for (WebSocket ws : spectators) {
            if (ws.isOpen()) {
                ws.send(json);
            } else {
                spectators.remove(ws);
            }
        }
    }

    public String getRoomId() {
        return roomId;
    }

    public GameState getState() {
        return state;
    }

    public String getP1Name() {
        return p1Name;
    }

    public String getP2Name() {
        return p2Name;
    }
}
