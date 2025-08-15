package person.kinman.cogame.sb.starter.initFactory;

import person.kinman.cogame.sb.contract.IUIManager;
import person.kinman.cogame.sb.network.NetworkProxy;
import person.kinman.cogame.sb.ui.UIController;
import person.kinman.cogame.sb.ui.page.connect.ServerConnectPanelController;
import person.kinman.cogame.sb.ui.page.start.StartPanelController;

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
