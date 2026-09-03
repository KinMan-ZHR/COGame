package person.kinman.cogame.server.user;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户会话管理器：维护全服唯一在线昵称与连接映射
 */
public class UserManager {
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    private final ConcurrentHashMap<String, WebSocket> activeUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocket, String> connUserMap = new ConcurrentHashMap<>();

    public record LoginResult(boolean success, String message) {}

    public synchronized LoginResult login(WebSocket conn, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return new LoginResult(false, "玩家昵称不能为空！");
        }

        String name = nickname.trim();
        if (name.length() < 2 || name.length() > 16) {
            return new LoginResult(false, "昵称长度须在 2 ~ 16 个字符之间！");
        }

        WebSocket existingConn = activeUsers.get(name);
        if (existingConn != null && existingConn.isOpen() && existingConn != conn) {
            logger.warn("登录被拒：昵称 [{}] 已被连接 [{}] 占用", name, existingConn.getRemoteSocketAddress());
            return new LoginResult(false, "昵称【" + name + "】已被其他在线玩家使用，请更换！");
        }

        // 若该连接之前已登录过其他昵称，先注销旧昵称
        String oldName = connUserMap.get(conn);
        if (oldName != null && !oldName.equals(name)) {
            activeUsers.remove(oldName);
        }

        activeUsers.put(name, conn);
        connUserMap.put(conn, name);
        logger.info("玩家 [{}] 登录成功 (连接: {})", name, conn.getRemoteSocketAddress());
        return new LoginResult(true, "登录成功");
    }

    public synchronized void logout(WebSocket conn) {
        String name = connUserMap.remove(conn);
        if (name != null) {
            activeUsers.remove(name);
            logger.info("玩家 [{}] 注销离线", name);
        }
    }

    public String getNickname(WebSocket conn) {
        return connUserMap.get(conn);
    }

    public boolean isLoggedIn(WebSocket conn) {
        return connUserMap.containsKey(conn);
    }

    public int getOnlineCount() {
        return activeUsers.size();
    }
}
