package person.kinman.cogame.client.ui.page.connect;

import com.google.common.eventbus.Subscribe;
import person.kinman.cogame.client.event.SystemErrorDialogEvent;
import person.kinman.cogame.client.eventBus.GlobalEventBus;
import person.kinman.cogame.client.contract.Connectable;
import person.kinman.cogame.client.ui.BaseController;
import person.kinman.cogame.client.ui.page.connect.events.ServerConnectRequestEvent;

/**
 * @author KinMan谨漫
 * @date 2025/8/15 09:49
 */
public class ServerConnectPanelController extends BaseController {
    private final Connectable networkProxy;

    public ServerConnectPanelController(Connectable networkProxy){
        this.networkProxy = networkProxy;
    }

    @Subscribe
    public void handleConnectRequestEvent(ServerConnectRequestEvent event){
        if (event.connect()){
            connect(event.ip(), event.port());
        } else {
            disconnect();
        }
    }

    /**
     * 连接到服务器（核心职责：验证输入+发起连接）
     */
    public void connect(String ip, String portStr) {
        new Thread(() -> {
        // 1. 验证IP和端口
        if (ip.isEmpty() || portStr.isEmpty()) {
            GlobalEventBus.getUiBus().post(new SystemErrorDialogEvent("请输入服务器IP和端口"));
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                GlobalEventBus.getUiBus().post(new SystemErrorDialogEvent("端口号必须在1024-65535之间"));
                return;
            }
            networkProxy.connect(ip, port);

        } catch (NumberFormatException ex) {
            GlobalEventBus.getUiBus().post(new SystemErrorDialogEvent(("端口号必须是数字")));
        }
        }, "ConnectThread").start();
    }

    /**
     * 断开与服务器的连接
     */
    public void disconnect() {
        // 2. 在后台线程执行实际断开操作
        new Thread(() -> {
            if (networkProxy.isConnected()) {
                networkProxy.disconnect();
            }
            // 断开结果会通过NetworkProxy发布的事件更新UI
        }, "DisconnectThread").start();
    }
}
