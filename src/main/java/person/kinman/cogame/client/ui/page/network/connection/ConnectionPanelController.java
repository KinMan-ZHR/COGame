package person.kinman.cogame.client.ui.page.network.connection;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import person.kinman.cogame.client.network.ConnectionStatusEvent;
import person.kinman.cogame.client.network.NetworkProxy;
import person.kinman.cogame.client.ui.page.event.GlobalEventBus;

import java.util.function.Consumer;

/**
 * 连接控制器 - 仅负责服务器连接相关逻辑（单一职责）
 */
public class ConnectionPanelController {
    private final NetworkProxy networkProxy;
    private String currentIp; // 保存当前连接的IP（便于重连）
    private int currentPort;  // 保存当前连接的端口
    private final EventBus eventBus;
    // 连接状态回调（通知UI连接成功/失败、状态变化）
    private Consumer<Boolean> connectionStateUpdater; // true=连接成功，false=断开
    private Consumer<String> buttonStatusTextUpdater; // 状态文本更新（如“连接中...”）
    private Consumer<String> errorHandler;  // 错误提示（如端口无效）

    public ConnectionPanelController() {
        // 获取全局事件总线
        this.eventBus = GlobalEventBus.getInstance();
        this.networkProxy =  NetworkProxy.getInstance();
        eventBus.register(this);
    }

    @Subscribe
    public void onServerMessage(ConnectionStatusEvent event) {
        String message = event.getMessage();
        boolean isConnected = event.isConnected();
        buttonStatusTextUpdater.accept(message);
        connectionStateUpdater.accept(isConnected); // 通知UI连接状态变化
        eventBus.post(ConnectionStatusEvent.createSuccessEvent());
    }


    /**
     * 连接到服务器（核心职责：验证输入+发起连接）
     */
    public void connect(String ip, String portStr) {
        // 1. 验证IP和端口
        if (ip.isEmpty() || portStr.isEmpty()) {
            errorHandler.accept("请输入服务器IP和端口");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                errorHandler.accept("端口号必须在1024-65535之间");
                return;
            }

            // 2. 保存当前连接参数（便于后续重连）
            this.currentIp = ip;
            this.currentPort = port;

            // 3. 发起连接，更新状态
            buttonStatusTextUpdater.accept("正在连接到 " + ip + ":" + port + "...");
            networkProxy.connect(ip, port);

        } catch (NumberFormatException ex) {
            errorHandler.accept("端口号必须是数字");
        }
    }

    /**
     * 断开与服务器的连接
     */
    public void disconnect() {
        if (networkProxy.isConnected()) {
            networkProxy.disconnect();
            buttonStatusTextUpdater.accept("正在断开连接...");
        }
    }


    public String getCurrentIp() {
        return currentIp;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    // 回调设置方法
    public void setConnectionStateUpdater(Consumer<Boolean> updater) {
        this.connectionStateUpdater = updater;
    }

    public void setButtonStatusTextUpdater(Consumer<String> updater) {
        this.buttonStatusTextUpdater = updater;
    }

    public void setErrorHandler(Consumer<String> handler) {
        this.errorHandler = handler;
    }
}