package person.kinman.cogame.sb.event;

/**
 * 连接状态事件：封装连接面板的状态变化信息
 */
public class ConnectionStatusEvent {
    private final boolean isConnected; // 连接是否成功
    private final String message;      // 状态描述（如“连接成功”“端口错误”）

    public ConnectionStatusEvent(boolean isConnected, String message) {
        this.isConnected = isConnected;
        this.message = message;
    }

    public static ConnectionStatusEvent createSuccessEvent() {
        return new ConnectionStatusEvent(true, "连接成功");
    }

    public static ConnectionStatusEvent createErrorEvent(String message) {
        return new ConnectionStatusEvent(false, message);
    }

    // Getter方法
    public boolean isConnected() {
        return isConnected;
    }

    public String getMessage() {
        return message;
    }
}