package person.kinman.cogame.server.net;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import person.kinman.cogame.core.net.RoomSummaryDto;
import person.kinman.cogame.core.net.WsMessage;
import person.kinman.cogame.server.room.GameRoom;
import person.kinman.cogame.server.room.RoomManager;
import person.kinman.cogame.server.user.UserManager;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * WebSocket 对战服务器 (支持登录认证、唯一昵称校验、密码房间、大厅列表与随机匹配)
 */
public class CoGameWebSocketServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(CoGameWebSocketServer.class);

    private final UserManager userManager = new UserManager();
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
        userManager.logout(conn);
        roomManager.unbindPlayer(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            WsMessage msg = WsMessage.fromJson(message);
            if (msg == null || msg.getType() == null) return;

            switch (msg.getType()) {
                case WsMessage.TYPE_LOGIN -> {
                    UserManager.LoginResult result = userManager.login(conn, msg.getPlayerName());
                    if (result.success()) {
                        conn.send(WsMessage.loginSuccess(msg.getPlayerName()).toJson());
                    } else {
                        conn.send(WsMessage.loginFail(result.message()).toJson());
                    }
                }
                case WsMessage.TYPE_LIST_ROOMS -> {
                    List<RoomSummaryDto> rooms = roomManager.listRooms();
                    conn.send(WsMessage.roomsList(rooms).toJson());
                }
                case WsMessage.TYPE_CREATE_ROOM -> {
                    String roomId = (msg.getRoomId() != null && !msg.getRoomId().trim().isEmpty())
                            ? msg.getRoomId().trim() : String.valueOf((int) (Math.random() * 9000 + 1000));
                    GameRoom room = roomManager.getOrCreateRoom(roomId, msg.getPassword(), msg.getBoardSize());
                    boolean joined = room.addPlayer(conn, msg.getPlayerName(), msg.getBoardSize(), msg.getPassword());
                    if (joined) {
                        roomManager.bindPlayer(conn, room);
                    }
                }
                case WsMessage.TYPE_JOIN_ROOM -> {
                    String roomId = (msg.getRoomId() != null && !msg.getRoomId().trim().isEmpty())
                            ? msg.getRoomId().trim() : "1001";
                    GameRoom room = roomManager.getRoom(roomId);
                    if (room == null) {
                        // 若房间尚不存在，作为新房间创建
                        room = roomManager.getOrCreateRoom(roomId, msg.getPassword(), msg.getBoardSize());
                    }
                    boolean joined = room.addPlayer(conn, msg.getPlayerName(), msg.getBoardSize(), msg.getPassword());
                    if (joined) {
                        roomManager.bindPlayer(conn, room);
                    }
                }
                case WsMessage.TYPE_RANDOM_JOIN -> {
                    GameRoom room = roomManager.findRandomAvailableRoom();
                    if (room == null) {
                        conn.send(WsMessage.error("当前暂无等待中的公开房间，您可以点击【创建房间】邀请好友！").toJson());
                    } else {
                        boolean joined = room.addPlayer(conn, msg.getPlayerName(), room.getState().getRows(), null);
                        if (joined) {
                            roomManager.bindPlayer(conn, room);
                        }
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
        if (conn == null) {
            logger.error("服务器级致命异常，退出进程");
            System.exit(1);
        }
    }

    @Override
    public void onStart() {
        logger.info("COGame WebSocket 服务器在端口 [{}] 启动成功！", getPort());
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }
}
