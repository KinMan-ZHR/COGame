package person.kinman.cogame.client.network;

/**
 * 连接状态事件：封装连接面板的状态变化信息
 *
 * @param isConnected 连接是否成功
 * @param message     状态描述（如“连接成功”“端口错误”）
 */
public record ConnectionStatusEvent(boolean isConnected, String message) {

    public static ConnectionStatusEvent createSuccessEvent() {
        return new ConnectionStatusEvent(true, "连接成功");
    }

    public static ConnectionStatusEvent createErrorEvent(String message) {
        return new ConnectionStatusEvent(false, message);
    }
}