package person.kinman.cogame.server.net;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import person.kinman.cogame.core.net.WsMessage;
import person.kinman.cogame.server.room.GameRoom;
import person.kinman.cogame.server.room.RoomManager;

import java.net.InetSocketAddress;

/**
 * WebSocket 对战服务器
 */
public class CoGameWebSocketServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(CoGameWebSocketServer.class);
    private final RoomManager roomManager = new RoomManager();

    public CoGameWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    public CoGameWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("客户端连接成功: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("客户端断开连接: {} (原因: {})", conn.getRemoteSocketAddress(), reason);
        roomManager.unbindPlayer(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            WsMessage msg = WsMessage.fromJson(message);
            if (msg == null || msg.getType() == null) return;

            switch (msg.getType()) {
                case WsMessage.TYPE_JOIN_ROOM -> {
                    String roomId = (msg.getRoomId() != null && !msg.getRoomId().trim().isEmpty())
                            ? msg.getRoomId().trim() : "default";
                    GameRoom room = roomManager.getOrCreateRoom(roomId);
                    boolean joined = room.addPlayer(conn, msg.getPlayerName());
                    if (joined) {
                        roomManager.bindPlayer(conn, room);
                    }
                }
                case WsMessage.TYPE_ACTION -> {
                    GameRoom room = roomManager.getRoomByPlayer(conn);
                    if (room != null && msg.getAction() != null) {
                        room.handleAction(conn, msg.getAction());
                    } else {
                        conn.send(WsMessage.error("未加入房间或指令为空！").toJson());
                    }
                }
                default -> {
                    logger.warn("未知消息类型: {}", msg.getType());
                }
            }
        } catch (Exception e) {
            logger.error("处理客户端消息异常", e);
            conn.send(WsMessage.error("消息解析错误: " + e.getMessage()).toJson());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("WebSocket 连接异常: {}", conn != null ? conn.getRemoteSocketAddress() : "null", ex);
    }

    @Override
    public void onStart() {
        logger.info("COGame WebSocket 服务器在端口 [{}] 启动成功！", getPort());
    }
}
