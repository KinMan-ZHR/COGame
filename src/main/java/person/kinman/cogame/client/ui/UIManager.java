package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.contract.IUIManager;
import person.kinman.cogame.client.contract.PanelId;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * UI管理器
 */
public class UIManager implements IUIManager {
    private final GameFrame gameFrame;
    private final Map<PanelId, BasePanel> panelInstances;
    private final Map<PanelId, PanelFactory> panelFactories = new HashMap<>(); // 面板工厂（用于延迟创建）
    private final JPanel containerPanel;              // 面板容器（使用CardLayout）
    private PanelId currentPanelId;

    public UIManager(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
        this.panelInstances = new HashMap<>();
        // 创建容器面板并使用CardLayout（这是管理器唯一负责的布局）
        this.containerPanel = new JPanel(new CardLayout());
        gameFrame.add(containerPanel, BorderLayout.CENTER);
    }

    // 注册面板
    @Override
    public void registerPanel(PanelId panelId, BasePanel panel) {
        panelInstances.put(panelId, panel);
        containerPanel.add(panel, panelId.name());
    }

    // 显示指定面板
    @Override
    public void showPanel(PanelId panelId) {
        // 1. 如果面板未创建，通过工厂创建并初始化
        if (!panelInstances.containsKey(panelId) && panelFactories.containsKey(panelId)) {
            BasePanel panel = panelFactories.get(panelId).create(); // 调用工厂创建面板
            registerPanel(panelId, panel);
        }

        if (panelInstances.containsKey(panelId)) {
            currentPanelId = panelId;
            CardLayout cardLayout = (CardLayout) containerPanel.getLayout();
            cardLayout.show(containerPanel, panelId.name());
            // 更新当前面板名称
            currentPanelId = panelId;

            // 触发面板的刷新方法（由面板自己实现刷新逻辑）
            panelInstances.get(panelId).showComponent();
            gameFrame.revalidate();
            gameFrame.repaint();
        }
    }

    // 隐藏指定面板
    @Override
    public void hidePanel(PanelId panelId) {
        if (panelInstances.containsKey(panelId)) {
            panelInstances.get(panelId).hideComponent();
            gameFrame.revalidate();
            gameFrame.repaint();
        }
    }

    // 获取当前面板
    @Override
    public BasePanel getCurrentPanel() {
        return panelInstances.get(currentPanelId);
    }

    // 获取指定面板
    @Override
    public BasePanel getPanel(PanelId panelId) {
        return panelInstances.get(panelId);
    }

    // 显示主窗口
    @Override
    public void showMainFrame() {
        gameFrame.setVisible(true);
    }

    // 注册面板工厂（而非直接注册实例）：启动时调用
    @Override
    public void registerPanelFactory(PanelId panelId, PanelFactory factory) {
        panelFactories.put(panelId, factory);
    }

    @Override
    public void showErrorDialog(String msg) {
        JOptionPane.showMessageDialog(getCurrentPanel(), msg, "错误", JOptionPane.ERROR_MESSAGE);
    }
}
