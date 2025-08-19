package person.kinman.cogame.client.starter;

import person.kinman.cogame.client.contract.PanelId;
import person.kinman.cogame.client.ui.GameFrame;
import person.kinman.cogame.client.ui.UIManager;
import person.kinman.cogame.client.starter.initFactory.PanelFactory;

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
        GameFrame gameFrame = new GameFrame();
        UIManager uiManager = new UIManager(gameFrame);
        // 1. 创建所有面板并注册到UIManager，自动订阅UI事件总线
        PanelFactory.createAllPanelsAndRegisterTo(uiManager);
        // 3. 显示初始面板
        uiManager.showPanel(PanelId.START_PANEL);
        uiManager.showMainFrame();
    }
}
