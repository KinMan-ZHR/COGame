package person.kinman.cogame.sb.contract;

import person.kinman.cogame.sb.ui.BasePanel;
import person.kinman.cogame.sb.ui.UIManager;

/**
 * @author KinMan谨漫
 * @date 2025/8/15 10:36
 */
public interface IUIManager {
    // 注册面板
    void registerPanel(PanelId panelId, BasePanel panel);

    // 显示指定面板
    void showPanel(PanelId panelId);

    // 隐藏指定面板
    void hidePanel(PanelId panelId);

    // 获取当前面板
    BasePanel getCurrentPanel();

    // 获取指定面板
    BasePanel getPanel(PanelId panelId);

    // 显示主窗口
    void showMainFrame();

    // 注册面板工厂（而非直接注册实例）：启动时调用
    void registerPanelFactory(PanelId panelId, UIManager.PanelFactory factory);

    void showErrorDialog(String msg);

    // 面板工厂接口：定义创建面板的方法
    @FunctionalInterface
    interface PanelFactory {
        BasePanel create();
    }
}
