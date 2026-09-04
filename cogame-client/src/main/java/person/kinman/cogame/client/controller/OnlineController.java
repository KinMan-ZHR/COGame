package person.kinman.cogame.client.controller;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.net.WsMessage;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 联机对战控制器：与远端 WebSocket 服务器同步 (支持密码保护与动态分配)
 */
public class OnlineController implements GameController {
    private final String serverUrl;
    private String roomId;
    private final String playerName;
    private final int boardSize;
    private final String password;
    private person.kinman.cogame.core.model.TurnOrderPreference turnPreference;

    private WebSocketClient wsClient;
    private GameState state;
    private int myPlayerId = 0; // 等待服务端分配 (1 or 2)
    private boolean gameStarted = false;

    private final List<Consumer<GameState>> stateListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> notificationListeners = new CopyOnWriteArrayList<>();
    private Consumer<GameState> onGameStarted;
    private Consumer<WsMessage> onRoomInfo;

    public OnlineController(String serverUrl, String roomId, String playerName) {
        this(serverUrl, roomId, playerName, 6, null, person.kinman.cogame.core.model.TurnOrderPreference.RANDOM);
    }

    public OnlineController(String serverUrl, String roomId, String playerName, int boardSize) {
        this(serverUrl, roomId, playerName, boardSize, null, person.kinman.cogame.core.model.TurnOrderPreference.RANDOM);
    }

    public OnlineController(String serverUrl, String roomId, String playerName, int boardSize, String password) {
        this(serverUrl, roomId, playerName, boardSize, password, person.kinman.cogame.core.model.TurnOrderPreference.RANDOM);
    }

    public OnlineController(String serverUrl, String roomId, String playerName, int boardSize, String password, person.kinman.cogame.core.model.TurnOrderPreference turnPreference) {
        this.serverUrl = serverUrl;
        this.roomId = (roomId != null && !roomId.trim().isEmpty()) ? roomId.trim() : null;
        this.playerName = playerName;
        this.boardSize = boardSize;
        this.password = password;
        this.turnPreference = (turnPreference != null) ? turnPreference : person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
        this.state = new GameState(boardSize);
        initConnection();
    }

    public void sendSetPreference(person.kinman.cogame.core.model.TurnOrderPreference preference) {
        this.turnPreference = (preference != null) ? preference : person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
        if (wsClient != null && wsClient.isOpen() && roomId != null) {
            wsClient.send(WsMessage.setPreference(roomId, this.turnPreference.getCode()).toJson());
            notifyMessage("已向服务器更新分先偏好: " + this.turnPreference.getDisplayName());
        }
    }

    public person.kinman.cogame.core.model.TurnOrderPreference getTurnPreference() {
        return turnPreference;
    }

    private void initConnection() {
        try {
            URI uri = new URI(serverUrl);
            wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    if (roomId != null) {
                        notifyMessage("连接成功，正在加入房间 [" + roomId + "] (规格: " + boardSize + "x" + boardSize + ", 分先: " + turnPreference.getDisplayName() + ")...");
                        WsMessage joinMsg = WsMessage.joinRoom(roomId, playerName, boardSize, password, turnPreference.getCode());
                        send(joinMsg.toJson());
                    } else {
                        notifyMessage("连接成功，正在为您随机匹配开放房间 (分先: " + turnPreference.getDisplayName() + ")...");
                        WsMessage randomMsg = WsMessage.randomJoin(playerName, turnPreference.getCode());
                        send(randomMsg.toJson());
                    }
                }

                @Override
                public void onMessage(String message) {
                    try {
                        WsMessage msg = WsMessage.fromJson(message);
                        if (msg == null) return;

                        switch (msg.getType()) {
                            case WsMessage.TYPE_ROOM_INFO -> {
                                if (msg.getRoomId() != null) {
                                    roomId = msg.getRoomId();
                                }
                                myPlayerId = msg.getAssignedPlayerId();
                                if (msg.getBoardSize() >= 6 && msg.getBoardSize() <= 13) {
                                    state = new GameState(msg.getBoardSize());
                                    notifyState();
                                }
                                notifyMessage(msg.getMessage() != null ? msg.getMessage() : "等待其他玩家加入...");
                                if (onRoomInfo != null) {
                                    onRoomInfo.accept(msg);
                                }
                            }
                            case WsMessage.TYPE_GAME_START -> {
                                gameStarted = true;
                                if (msg.getRoomId() != null) {
                                    roomId = msg.getRoomId();
                                }
                                myPlayerId = msg.getAssignedPlayerId();
                                if (msg.getState() != null) {
                                    state = msg.getState();
                                }
                                String notice = (msg.getMessage() != null && !msg.getMessage().isEmpty())
                                        ? msg.getMessage()
                                        : "⚔️ 对局开战！您是 " + (myPlayerId == 1 ? "先手(P1)" : "后手(P2)");
                                notifyMessage(notice);
                                notifyState();
                                if (onGameStarted != null) {
                                    onGameStarted.accept(state);
                                }
                            }
                            case WsMessage.TYPE_STATE_UPDATE -> {
                                if (msg.getState() != null) {
                                    state = msg.getState();
                                    notifyState();
                                }
                            }
                            case WsMessage.TYPE_GAME_OVER -> {
                                if (msg.getState() != null) {
                                    state = msg.getState();
                                    notifyState();
                                    notifyMessage("🏆 游戏结束！" + state.getWinReason());
                                }
                            }
                            case WsMessage.TYPE_REMATCH_INFO -> {
                                if (msg.getMessage() != null) {
                                    notifyMessage("🔔 " + msg.getMessage());
                                }
                            }
                            case WsMessage.TYPE_PLAYER_LEFT -> {
                                notifyMessage("⚠️ 对手已离开房间！" + (msg.getMessage() != null ? msg.getMessage() : ""));
                            }
                            case WsMessage.TYPE_ERROR -> {
                                notifyMessage("❌ 提示: " + msg.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    notifyMessage("⚠️ 与服务器连接断开: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    notifyMessage("❌ 网络连接异常: " + (ex != null ? ex.getMessage() : "未知"));
                }
            };

            notifyMessage("正在连接服务器: " + serverUrl + " ...");
            wsClient.connect();
        } catch (Exception e) {
            notifyMessage("连接初始化失败: " + e.getMessage());
        }
    }

    @Override
    public void handleUserAction(GameAction action) {
        if (!gameStarted || state.isOver()) {
            if (action.getType() == GameAction.Type.RESET && wsClient != null && wsClient.isOpen()) {
                wsClient.send(WsMessage.action(action).toJson());
            }
            return;
        }

        if (state.getCurrentTurn() != myPlayerId) {
            notifyMessage("当前是对手的回合，请稍候！");
            return;
        }

        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(WsMessage.action(action).toJson());
        } else {
            notifyMessage("未连接到服务器！");
        }
    }

    @Override
    public void resetGame() {
        resetGameWithPreference(this.turnPreference);
    }

    @Override
    public void resetGameWithPreference(person.kinman.cogame.core.model.TurnOrderPreference pref) {
        this.turnPreference = (pref != null) ? pref : person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
        if (wsClient != null && wsClient.isOpen() && roomId != null) {
            wsClient.send(WsMessage.rematchRequest(roomId, this.turnPreference.getCode()).toJson());
            notifyMessage("已向对手发送新一局分先请求 (" + this.turnPreference.getDisplayName() + ")，等待对手就绪...");
        } else if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(WsMessage.action(GameAction.reset()).toJson());
        } else {
            notifyMessage("未连接到服务器！");
        }
    }

    @Override
    public void swapTurnOrder() {
        person.kinman.cogame.core.model.TurnOrderPreference newPref =
                (myPlayerId == 1) ? person.kinman.cogame.core.model.TurnOrderPreference.SECOND
                                  : person.kinman.cogame.core.model.TurnOrderPreference.FIRST;
        resetGameWithPreference(newPref);
    }

    @Override
    public person.kinman.cogame.core.model.TurnOrderPreference getCurrentPreference() {
        return turnPreference;
    }

    private void notifyState() {
        for (Consumer<GameState> l : stateListeners) {
            try { l.accept(state); } catch (Exception ignored) {}
        }
    }

    private void notifyMessage(String msg) {
        for (Consumer<String> l : notificationListeners) {
            try { l.accept(msg); } catch (Exception ignored) {}
        }
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    @Override
    public GameState getGameState() {
        return state;
    }

    @Override
    public int getMyPlayerId() {
        return myPlayerId;
    }

    @Override
    public String getModeName() {
        String roomTag = (roomId != null) ? " [房号:" + roomId + "]" : "";
        return "网络联机对战" + roomTag;
    }

    @Override
    public void setOnStateChanged(Consumer<GameState> listener) {
        if (listener != null) {
            this.stateListeners.add(listener);
        }
    }

    @Override
    public void setOnNotification(Consumer<String> listener) {
        if (listener != null) {
            this.notificationListeners.add(listener);
        }
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public String getPassword() {
        return password;
    }

    public void setOnGameStarted(Consumer<GameState> listener) {
        this.onGameStarted = listener;
    }

    public void setOnRoomInfo(Consumer<WsMessage> listener) {
        this.onRoomInfo = listener;
    }

    @Override
    public void close() {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }
}
