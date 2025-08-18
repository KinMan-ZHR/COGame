package person.kinman.cogame.client.ui.page.connect;

import com.google.common.eventbus.Subscribe;
import person.kinman.cogame.client.event.SystemErrorDialogEvent;
import person.kinman.cogame.client.eventBus.GlobalEventBus;
import person.kinman.cogame.client.contract.Connectable;
import person.kinman.cogame.client.ui.BaseController;
import person.kinman.cogame.client.ui.page.connect.enums.ConnectionButtonStatus;
import person.kinman.cogame.client.ui.page.connect.events.ServerConnectRequestEvent;
import person.kinman.cogame.client.ui.page.connect.events.ServerConnectResponseEvent;
import person.kinman.cogame.client.ui.page.connect.events.ServerDisconnectRequestEvent;

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
        if (!networkProxy.isConnected()) {
            connect(event.ip(), event.port());
        }
        if (networkProxy.isConnected()){
            GlobalEventBus.getUiBus().post(new ServerConnectResponseEvent(ConnectionButtonStatus.CONNECTED));
        } else {
            GlobalEventBus.getUiBus().post(new ServerConnectResponseEvent(ConnectionButtonStatus.CONNECT_FAILED));
        }
    }

    @Subscribe
    public void handleDisconnectRequestEvent(ServerDisconnectRequestEvent event){
        if (networkProxy.isConnected()) {
            disconnect();
        }

        if (networkProxy.isConnected()){
            GlobalEventBus.getUiBus().post(new ServerConnectResponseEvent(ConnectionButtonStatus.DISCONNECT_FAILED));
        } else {
            GlobalEventBus.getUiBus().post(new ServerConnectResponseEvent(ConnectionButtonStatus.DISCONNECTED));
        }
    }

    /**
     * 连接到服务器（核心职责：验证输入+发起连接）
     */
    public void connect(String ip, String portStr) {
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
    }

    /**
     * 断开与服务器的连接
     */
    public void disconnect() {
        if (networkProxy.isConnected()) {
            networkProxy.disconnect();
        }
    }
}
