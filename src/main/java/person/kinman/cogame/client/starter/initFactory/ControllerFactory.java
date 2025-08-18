package person.kinman.cogame.client.starter.initFactory;

import person.kinman.cogame.client.contract.IUIManager;
import person.kinman.cogame.client.network.NetworkProxy;
import person.kinman.cogame.client.ui.UIController;
import person.kinman.cogame.client.ui.page.connect.ServerConnectPanelController;
import person.kinman.cogame.client.ui.page.start.StartPanelController;

/**
 * @author KinMan谨漫
 * @date 2025/8/14 17:43
 */
public class ControllerFactory {
    public static void createAllControllers(IUIManager uiManager){
        NetworkProxy networkProxy = new NetworkProxy();
        new UIController(uiManager);
        new StartPanelController();
        new ServerConnectPanelController(networkProxy);
    }
}
