package person.kinman.cogame.client.contract;

/**
 * @author KinMan谨漫
 * @date 2025/8/15 10:15
 */
public interface Connectable {
    void connect(String ip, int port);

    void disconnect();

    boolean isConnected();
}
