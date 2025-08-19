package person.kinman.cogame.client.ui.page.connect.events;

/**
 * @author KinMan谨漫
 * @date 2025/8/15 09:47
 */
public record ServerConnectRequestEvent(boolean connect, String ip, String port) {
}
