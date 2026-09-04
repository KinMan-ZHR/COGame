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
import person.kinman.cogame.server.security.SecurityGuard;
import person.kinman.cogame.server.user.UserManager;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * WebSocket 对战服务器 (深度集成应用级安全防御网：防 DDoS 连接轰炸、报文炸弹、高频刷屏与僵尸连接)
 */
public class CoGameWebSocketServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(CoGameWebSocketServer.class);

    private final UserManager userManager = new UserManager();
    private final RoomManager roomManager = new RoomManager();
    private final SecurityGuard securityGuard = new SecurityGuard();

    public CoGameWebSocketServer(int port) {
        this(new InetSocketAddress(port));
    }

    public CoGameWebSocketServer(InetSocketAddress address) {
        super(address);
        // 开启 30 秒 TCP 心跳失联检测，自动清退幽灵/假死连接
        this.setConnectionLostTimeout(30);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // 安全拦截：检查单 IP 并发连接数与黑名单状态
        if (!securityGuard.recordNewConnection(conn)) {
            logger.warn("[SECURITY] 拦截违规或超额连接: {}", conn.getRemoteSocketAddress());
            conn.close(1008, "安全策略拦截：该 IP 已达到并发连接数上限或已被临时封禁");
            return;
        }
        logger.info("客户端连接成功: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("客户端断开连接: {} (原因: {})", conn.getRemoteSocketAddress(), reason);
        securityGuard.recordConnectionClosed(conn);
        userManager.logout(conn);
        roomManager.unbindPlayer(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // 1. 安全防御：报文超长阻断 (防内存炸弹/OOM攻击)
        if (message != null && message.length() > SecurityGuard.MAX_MESSAGE_PAYLOAD_SIZE) {
            logger.warn("[SECURITY] 拦截超大恶意报文 ({} bytes): {}", message.length(), conn.getRemoteSocketAddress());
            securityGuard.recordViolation(conn, "超大恶意报文");
            conn.close(1009, "Message payload too large");
            return;
        }

        // 2. 安全防御：频率超限阻断 (防恶意狂发刷屏/打满CPU)
        if (!securityGuard.checkMessageRate(conn)) {
            conn.send(WsMessage.error("请求过于频繁，触发安全保护！").toJson());
            conn.close(1008, "Rate limit exceeded");
            return;
        }

        try {
            WsMessage msg = WsMessage.fromJson(message);
            if (msg == null || msg.getType() == null) return;

            // 3. 安全防御：输入清洗与合法性校验
            if (msg.getPlayerName() != null && !SecurityGuard.isValidPlayerName(msg.getPlayerName())) {
                conn.send(WsMessage.error("玩家昵称包含非法字符或长度不符合规范 (2~16字符)！").toJson());
                return;
            }
            if (msg.getRoomId() != null && !SecurityGuard.isValidRoomId(msg.getRoomId())) {
                conn.send(WsMessage.error("房间号格式非法 (仅允许字母、数字和中划线，32位以内)！").toJson());
                return;
            }
            if (msg.getBoardSize() > 0) {
                // 强制规格收敛在 6~13 范围
                msg.setBoardSize(Math.max(6, Math.min(13, msg.getBoardSize())));
            }

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
                    boolean joined = room.addPlayer(conn, msg.getPlayerName(), msg.getBoardSize(), msg.getPassword(), msg.getTurnPreference());
                    if (joined) {
                        roomManager.bindPlayer(conn, room);
                    }
                }
                case WsMessage.TYPE_JOIN_ROOM -> {
                    String roomId = (msg.getRoomId() != null && !msg.getRoomId().trim().isEmpty())
                            ? msg.getRoomId().trim() : "1001";
                    GameRoom room = roomManager.getRoom(roomId);
                    if (room == null) {
                        room = roomManager.getOrCreateRoom(roomId, msg.getPassword(), msg.getBoardSize());
                    }
                    boolean joined = room.addPlayer(conn, msg.getPlayerName(), msg.getBoardSize(), msg.getPassword(), msg.getTurnPreference());
                    if (joined) {
                        roomManager.bindPlayer(conn, room);
                    }
                }
                case WsMessage.TYPE_RANDOM_JOIN -> {
                    GameRoom room = roomManager.findRandomAvailableRoom();
                    if (room == null) {
                        conn.send(WsMessage.error("当前暂无等待中的公开房间，您可以点击【创建房间】邀请好友！").toJson());
                    } else {
                        boolean joined = room.addPlayer(conn, msg.getPlayerName(), room.getState().getRows(), null, msg.getTurnPreference());
                        if (joined) {
                            roomManager.bindPlayer(conn, room);
                        }
                    }
                }
                case WsMessage.TYPE_SET_PREFERENCE -> {
                    GameRoom room = roomManager.getRoomByPlayer(conn);
                    if (room != null && msg.getTurnPreference() != null) {
                        room.setPlayerPreference(conn, msg.getTurnPreference());
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
            securityGuard.recordViolation(conn, "非法畸形报文");
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
        logger.info("COGame WebSocket 服务器在端口 [{}] 启动成功！安全防护网已全面激活", getPort());
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public SecurityGuard getSecurityGuard() {
        return securityGuard;
    }
}
