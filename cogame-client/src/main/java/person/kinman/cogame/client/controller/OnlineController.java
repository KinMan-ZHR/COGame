package person.kinman.cogame.client.controller;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.net.WsMessage;

import java.net.URI;
import java.util.function.Consumer;

/**
 * 联机对战控制器：与远端 WebSocket 服务器同步
 */
public class OnlineController implements GameController {
    private final String serverUrl;
    private final String roomId;
    private final String playerName;
    private final int boardSize;

    private WebSocketClient wsClient;
    private GameState state;
    private int myPlayerId = 0; // 等待服务端分配 (1 or 2)
    private boolean gameStarted = false;

    private Consumer<GameState> onStateChanged;
    private Consumer<String> onNotification;

    public OnlineController(String serverUrl, String roomId, String playerName) {
        this(serverUrl, roomId, playerName, 6);
    }

    public OnlineController(String serverUrl, String roomId, String playerName, int boardSize) {
        this.serverUrl = serverUrl;
        this.roomId = roomId;
        this.playerName = playerName;
        this.boardSize = boardSize;
        this.state = new GameState(boardSize);
        initConnection();
    }

    private void initConnection() {
        try {
            URI uri = new URI(serverUrl);
            wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    notifyMessage("连接服务器成功，正在加入房间 [" + roomId + "] (棋盘规格: " + boardSize + "x" + boardSize + ")...");
                    // 发送加入房间消息
                    WsMessage joinMsg = WsMessage.joinRoom(roomId, playerName, boardSize);
                    send(joinMsg.toJson());
                }

                @Override
                public void onMessage(String message) {
                    try {
                        WsMessage msg = WsMessage.fromJson(message);
                        if (msg == null) return;

                        switch (msg.getType()) {
                            case WsMessage.TYPE_ROOM_INFO -> {
                                myPlayerId = msg.getAssignedPlayerId();
                                notifyMessage(msg.getMessage() != null ? msg.getMessage() : "等待其他玩家加入...");
                            }
                            case WsMessage.TYPE_GAME_START -> {
                                gameStarted = true;
                                myPlayerId = msg.getAssignedPlayerId();
                                if (msg.getState() != null) {
                                    state = msg.getState();
                                }
                                notifyMessage("对手已就绪，游戏开始！你是 " + (myPlayerId == 1 ? "先手(P1)" : "后手(P2)"));
                                notifyState();
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
                                    notifyMessage("游戏结束！" + state.getWinReason());
                                }
                            }
                            case WsMessage.TYPE_PLAYER_LEFT -> {
                                notifyMessage("提示: " + msg.getMessage());
                            }
                            case WsMessage.TYPE_ERROR -> {
                                notifyMessage("错误: " + msg.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    notifyMessage("与服务器连接断开: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    notifyMessage("网络连接异常: " + ex.getMessage());
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
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(WsMessage.action(GameAction.reset()).toJson());
        }
    }

    private void notifyState() {
        if (onStateChanged != null) {
            onStateChanged.accept(state);
        }
    }

    private void notifyMessage(String msg) {
        if (onNotification != null) {
            onNotification.accept(msg);
        }
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
        return "网络联机对战 [房间: " + roomId + " | 身位: " + (myPlayerId == 0 ? "连接中" : (myPlayerId == 1 ? "P1(先手)" : "P2(后手)")) + "]";
    }

    @Override
    public void setOnStateChanged(Consumer<GameState> listener) {
        this.onStateChanged = listener;
    }

    @Override
    public void setOnNotification(Consumer<String> listener) {
        this.onNotification = listener;
    }

    @Override
    public void close() {
        if (wsClient != null) {
            wsClient.close();
        }
    }
}
