package person.kinman.cogame.sb.ui;

import person.kinman.cogame.sb.contract.IPanel;
import person.kinman.cogame.sb.eventBus.GlobalEventBus;

import javax.swing.*;
import java.awt.*;

/**
 * 所有面板的基类
 */
public abstract class BasePanel extends JPanel implements IPanel {
    protected boolean isInitialized = false; // 初始化状态标记
    
    public BasePanel() {
        GlobalEventBus.getUiBus().register(this);
    }

    /**
     * 初始化面板（模板方法）
     * 确保init()仅执行一次，子类通过重写initComponents()实现具体初始化
     */
    @Override
    public final void init() {
        if (!isInitialized) {
            initUI(); // 子类实现UI组件初始化
            initListeners();
            isInitialized = true;
        }
    }

    /**
     * 显示面板
     * 显示前确保已初始化，显示后触发onShow()回调
     */
    @Override
    public void showComponent() {
        init();
        setVisible(true);
        onShow(); // 触发显示后回调
    }

    /**
     * 隐藏面板
     * 隐藏后触发onHide()回调
     */
    @Override
    public void hideComponent() {
        setVisible(false);
        onHide(); // 触发隐藏后回调
    }

    /**
     * 销毁面板（释放资源）
     * 子类可重写此方法释放特定资源（如网络连接、监听器）
     */
    @Override
    public void destroy() {
        removeAll(); // 移除所有子组件
        isInitialized = false; // 重置初始化状态
    }

    /**
     * 获取当前面板组件（自身）
     */
    @Override
    public Component getComponent() {
        return this;
    }

    /**
     * 检查是否已初始化
     */
    @Override
    public boolean isInitialized() {
        return isInitialized;
    }
    
    // 初始化事件监听器
    protected abstract void initListeners();

    // 初始化UI组件（子类实现）
    protected abstract void initUI();
    
    // 显示消息对话框
    protected void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

}
