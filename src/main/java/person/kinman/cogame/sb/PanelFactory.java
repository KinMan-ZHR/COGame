package person.kinman.cogame.sb;

import person.kinman.cogame.sb.contract.PanelId;
import person.kinman.cogame.sb.controller.UIManager;
import person.kinman.cogame.sb.ui.page.ConnectPanel;
import person.kinman.cogame.sb.ui.page.StartPanel;

/**
 * @author KinMan谨漫
 * @date 2025/8/14 17:10
 */
public class PanelFactory {
    public static void createAllPanels() {
        UIManager uiManager = UIManager.getInstance();
        // 核心面板：仍可直接创建（如启动面板，用户立即需要）
        uiManager.registerPanel(PanelId.START_PANEL, new StartPanel());
        // 非核心面板：注册工厂，延迟创建
        uiManager.registerPanelFactory(PanelId.SERVER_CONNECT_PANEL, ConnectPanel::new);
    }
}
