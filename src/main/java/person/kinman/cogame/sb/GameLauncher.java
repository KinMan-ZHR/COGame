package person.kinman.cogame.sb;

import person.kinman.cogame.sb.contract.PanelId;
import person.kinman.cogame.sb.controller.UIManager;

import javax.swing.*;


/**
 * @author KinMan谨漫
 * @date 2025/8/14 10:29
 */
public class GameLauncher {
    public static void main(String[] args) {
        // 确保Swing组件在EDT线程中初始化
        SwingUtilities.invokeLater(GameLauncher::initializeApplication);
    }

    private static void initializeApplication() {

        // 1. 创建所有面板并注册到UIManager，自动订阅UI事件总线
        PanelFactory.createAllPanels();

        // 2. 创建所有控制器（自动订阅事件总线）
        ControllerFactory.createAllControllers();

        // 3. 显示初始面板
        UIManager.getInstance().showPanel(PanelId.START_PANEL);
        UIManager.getInstance().showMainFrame();
    }
}
