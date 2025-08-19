package person.kinman.cogame.client.starter.initFactory;

import person.kinman.cogame.client.contract.PanelId;
import person.kinman.cogame.client.network.NetworkProxy;
import person.kinman.cogame.client.ui.UIController;
import person.kinman.cogame.client.ui.UIManager;
import person.kinman.cogame.client.ui.page.connect.ServerConnectPanel;
import person.kinman.cogame.client.ui.page.connect.ServerConnectPanelController;
import person.kinman.cogame.client.ui.page.room.RoomPanel;
import person.kinman.cogame.client.ui.page.room.RoomPanelController;
import person.kinman.cogame.client.ui.page.start.StartPanel;
import person.kinman.cogame.client.ui.page.start.StartPanelController;

/**
 * @author KinMan谨漫
 * @date 2025/8/14 17:10
 */
public class PanelFactory {
    public static void createAllPanelsAndRegisterTo(UIManager uiManager) {
        NetworkProxy networkProxy = new NetworkProxy();
        new UIController(uiManager);
        new StartPanelController();
        new ServerConnectPanelController(networkProxy);
        // 核心面板：仍可直接创建（如启动面板，用户立即需要）
        uiManager.registerPanel(PanelId.START_PANEL, new StartPanel());
        // 非核心面板：注册工厂，延迟创建
        uiManager.registerPanelFactory(PanelId.SERVER_CONNECT_PANEL, ServerConnectPanel::new);

        uiManager.registerPanel(PanelId.ROOM_LIST_PANEL, new RoomPanel(new RoomPanelController(networkProxy)));
    }
}
