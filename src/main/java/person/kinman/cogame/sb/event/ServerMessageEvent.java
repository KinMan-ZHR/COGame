package person.kinman.cogame.sb.event;

/**
 * @author KinMan谨漫
 * @date 2025/8/13 17:21
 */
public class ServerMessageEvent {
    private final String message;
    public ServerMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
