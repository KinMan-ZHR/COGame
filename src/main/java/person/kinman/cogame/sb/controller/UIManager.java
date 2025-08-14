package person.kinman.cogame.sb.controller;

import person.kinman.cogame.sb.contract.PanelId;
import person.kinman.cogame.sb.ui.BasePanel;
import person.kinman.cogame.sb.ui.GameFrame;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * UI管理器
 */
public class UIManager {
    private final GameFrame gameFrame;
    private final Map<PanelId, BasePanel> panelInstances;
    private final Map<PanelId, PanelFactory> panelFactories = new HashMap<>(); // 面板工厂（用于延迟创建）
    private final JPanel containerPanel;              // 面板容器（使用CardLayout）
    private PanelId currentPanelId;

    // 单例实例（volatile保证多线程可见性）
    private static volatile UIManager instance;

    // 私有构造函数：禁止外部直接实例化
    private UIManager() {
        this.gameFrame = new GameFrame();
        this.panelInstances = new HashMap<>();
        // 创建容器面板并使用CardLayout（这是管理器唯一负责的布局）
        this.containerPanel = new JPanel(new CardLayout());
        gameFrame.add(containerPanel, BorderLayout.CENTER);
    }

    public static UIManager getInstance() {
        if (instance == null) {
            synchronized (UIManager.class) {
                if (instance == null) {
                    instance = new UIManager();
                }
            }
        }
        return instance;
    }

    // 注册面板
    public void registerPanel(PanelId panelId, BasePanel panel) {
        panelInstances.put(panelId, panel);
        containerPanel.add(panel, panelId);
    }

    // 显示指定面板
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
    public void hidePanel(PanelId panelId) {
        if (panelInstances.containsKey(panelId)) {
            panelInstances.get(panelId).hideComponent();
            gameFrame.revalidate();
            gameFrame.repaint();
        }
    }

    // 获取当前面板
    public BasePanel getCurrentPanel() {
        return panelInstances.get(currentPanelId);
    }

    // 获取指定面板
    public BasePanel getPanel(PanelId panelId) {
        return panelInstances.get(panelId);
    }

    // 显示主窗口
    public void showMainFrame() {
        gameFrame.setVisible(true);
    }

    // 注册面板工厂（而非直接注册实例）：启动时调用
    public void registerPanelFactory(PanelId panelId, PanelFactory factory) {
        panelFactories.put(panelId, factory);
    }

    // 面板工厂接口：定义创建面板的方法
    @FunctionalInterface
    public interface PanelFactory {
        BasePanel create();
    }
}
