package person.kinman.cogame.server.security;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务端应用级安全防护中心：
 * 1. 单 IP 并发连接数限制 (防连接池耗尽 / Slowloris)
 * 2. 报文尺寸上限阻断 (防超大内存炸弹 / OOM)
 * 3. 消息频率限流 (防高频指令刷屏 / CPU打满)
 * 4. 恶意攻击者动态临时黑名单 (封禁5分钟)
 * 5. 严格字段白名单与输入清洗
 */
public class SecurityGuard {
    private static final Logger logger = LoggerFactory.getLogger(SecurityGuard.class);

    public static final int MAX_CONNECTIONS_PER_IP = 8;        // 单 IP 最多 8 个同时活跃连接
    public static final int MAX_MESSAGE_PAYLOAD_SIZE = 16384;  // 单个 WebSocket 报文上限 16 KB
    public static final int MAX_MESSAGES_PER_SECOND = 25;      // 单连接每秒最多 25 条指令
    public static final long BAN_DURATION_MS = 5 * 60 * 1000L; // 违规封禁时长: 5 分钟
    public static final int VIOLATION_BAN_THRESHOLD = 3;       // 违规达 3 次触发封禁

    // IP -> 当前并发连接数
    private final ConcurrentHashMap<String, AtomicInteger> ipConnectionCount = new ConcurrentHashMap<>();
    // WebSocket -> IP
    private final ConcurrentHashMap<WebSocket, String> connIpMap = new ConcurrentHashMap<>();
    // WebSocket -> 限流窗口
    private final ConcurrentHashMap<WebSocket, RateWindow> connRateMap = new ConcurrentHashMap<>();
    // IP -> 封禁截止时间戳
    private final ConcurrentHashMap<String, Long> bannedIps = new ConcurrentHashMap<>();
    // IP -> 累计违规次数
    private final ConcurrentHashMap<String, AtomicInteger> ipViolations = new ConcurrentHashMap<>();

    private static class RateWindow {
        long secondEpoch = 0;
        int count = 0;
    }

    /**
     * 检查该 IP 是否处于封禁期
     */
    public boolean isIpBanned(String ip) {
        if (ip == null) return false;
        Long banUntil = bannedIps.get(ip);
        if (banUntil == null) return false;

        if (System.currentTimeMillis() > banUntil) {
            // 封禁到期解封
            bannedIps.remove(ip);
            ipViolations.remove(ip);
            logger.info("[SECURITY] IP [{}] 封禁到期自动解封", ip);
            return false;
        }
        return true;
    }

    /**
     * 记录新连接并校验单 IP 并发上限
     * @return true 允许连接；false 超限拒绝
     */
    public synchronized boolean recordNewConnection(WebSocket conn) {
        String ip = extractIp(conn);
        if (ip == null) return true;

        if (isIpBanned(ip)) {
            logger.warn("[SECURITY] 拦截黑名单 IP 连接尝试: {}", ip);
            return false;
        }

        AtomicInteger counter = ipConnectionCount.computeIfAbsent(ip, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > MAX_CONNECTIONS_PER_IP) {
            counter.decrementAndGet();
            logger.warn("[SECURITY] 拦截单 IP 超额连接: IP={}, 当前连接已达上限 {}", ip, MAX_CONNECTIONS_PER_IP);
            recordViolation(ip, "并发连接数超限");
            return false;
        }

        connIpMap.put(conn, ip);
        connRateMap.put(conn, new RateWindow());
        return true;
    }

    /**
     * 连接断开时释放计数
     */
    public void recordConnectionClosed(WebSocket conn) {
        String ip = connIpMap.remove(conn);
        connRateMap.remove(conn);
        if (ip != null) {
            AtomicInteger counter = ipConnectionCount.get(ip);
            if (counter != null) {
                if (counter.decrementAndGet() <= 0) {
                    ipConnectionCount.remove(ip);
                }
            }
        }
    }

    /**
     * 校验单连接指令发送频率
     * @return true 正常频率；false 频率超限
     */
    public synchronized boolean checkMessageRate(WebSocket conn) {
        RateWindow window = connRateMap.get(conn);
        if (window == null) {
            window = new RateWindow();
            connRateMap.put(conn, window);
        }

        long currentSec = System.currentTimeMillis() / 1000;
        if (window.secondEpoch != currentSec) {
            window.secondEpoch = currentSec;
            window.count = 1;
            return true;
        }

        window.count++;
        if (window.count > MAX_MESSAGES_PER_SECOND) {
            String ip = connIpMap.get(conn);
            logger.warn("[SECURITY] 检测到高频报文攻击: IP={}, 每秒报文数={}", ip, window.count);
            if (ip != null) {
                recordViolation(ip, "高频指令刷屏");
            }
            return false;
        }
        return true;
    }

    /**
     * 记录违规并在多次违规后实施封禁
     */
    public void recordViolation(WebSocket conn, String reason) {
        String ip = connIpMap.get(conn);
        if (ip != null) {
            recordViolation(ip, reason);
        }
    }

    public synchronized void recordViolation(String ip, String reason) {
        AtomicInteger v = ipViolations.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int count = v.incrementAndGet();
        logger.warn("[SECURITY] IP [{}] 触发安全违规 (理由: {}, 当前违规: {}/{})", ip, reason, count, VIOLATION_BAN_THRESHOLD);

        if (count >= VIOLATION_BAN_THRESHOLD) {
            long banUntil = System.currentTimeMillis() + BAN_DURATION_MS;
            bannedIps.put(ip, banUntil);
            logger.error("[SECURITY] IP [{}] 违规达到阈值，已被列入防火墙动态黑名单，封禁 5 分钟！", ip);
        }
    }

    /**
     * 校验房间号安全性 (只允许英文字母、数字和连字符，1~32位)
     */
    public static boolean isValidRoomId(String roomId) {
        if (roomId == null) return false;
        String trimmed = roomId.trim();
        return trimmed.matches("^[a-zA-Z0-9_-]{1,32}$");
    }

    /**
     * 校验玩家昵称安全性 (2~16位，不允许不可见控制字符)
     */
    public static boolean isValidPlayerName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 16) return false;
        // 过滤常见注入字符与控制符
        return !trimmed.matches(".*[\\p{Cntrl}<>\"'&].*");
    }

    public static String extractIp(WebSocket conn) {
        try {
            InetSocketAddress remote = conn.getRemoteSocketAddress();
            if (remote != null && remote.getAddress() != null) {
                return remote.getAddress().getHostAddress();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
