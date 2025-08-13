package person.kinman.cogame.client.ui.tools;

import com.google.common.eventbus.EventBus;
import person.kinman.cogame.client.contract.IPanel;
import person.kinman.cogame.client.ui.page.event.GlobalEventBus;

import javax.swing.*;
import java.awt.*;

/**
 * 面板基类 - 实现IPanel接口，封装通用生命周期逻辑
 * 所有具体面板（如连接面板、房间面板）都应继承此类
 */
public abstract class BasePanel extends JPanel implements IPanel {
    protected boolean isInitialized = false; // 初始化状态标记
    private final EventBus eventBus = GlobalEventBus.getInstance();
    public BasePanel() {
        super();
        setLayout(new BorderLayout()); // 默认使用BorderLayout，子类可重写
        setOpaque(true); // 默认可不透明，确保背景色生效
        eventBus.register(this);
        init();
    }

    /**
     * 初始化面板（模板方法）
     * 确保init()仅执行一次，子类通过重写initComponents()实现具体初始化
     */
    @Override
    public final void init() {
        if (!isInitialized) {
            initComponents(); // 子类实现UI组件初始化
            bindEvents(); // 子类实现事件绑定
            isInitialized = true;
        }
    }

    /**
     * 显示面板
     * 显示前确保已初始化，显示后触发onShow()回调
     */
    @Override
    public void showComponent() {
        if (!isInitialized) {
            throw new IllegalStateException("面板未初始化，请先调用init()");
        }
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

    // 以下为抽象方法，由子类实现具体逻辑

    /**
     * 初始化UI组件（子类实现）
     * 用于创建和布局子组件（如按钮、输入框）
     */
    protected abstract void initComponents();

    /**
     * 绑定事件监听器（子类实现）
     * 用于为组件绑定点击、输入等事件
     */
    protected abstract void bindEvents();
}
    