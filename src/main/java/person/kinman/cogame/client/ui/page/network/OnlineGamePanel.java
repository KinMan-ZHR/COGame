package person.kinman.cogame.client.ui.page.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import person.kinman.cogame.client.ui.GameLauncher;
import person.kinman.cogame.client.ui.page.network.connection.ConnectionPanelController;
import person.kinman.cogame.client.ui.page.network.connection.ConnectionPanel;
import person.kinman.cogame.client.ui.page.network.room.RoomPanelController;
import person.kinman.cogame.client.ui.page.network.room.RoomPanel;
import person.kinman.cogame.client.ui.tools.BasePanel;
import person.kinman.cogame.client.ui.tools.JButtonFactory;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * 联网对战容器面板 - 整合连接面板和房间面板，不包含业务逻辑
 */
public class OnlineGamePanel extends BasePanel {
    private static final Logger log = LoggerFactory.getLogger(OnlineGamePanel.class);
    private GameLauncher mainFrame;
    private ConnectionPanel connectionPanel;
    private RoomPanel roomPanel;

    public OnlineGamePanel(GameLauncher mainFrame) {
        this.mainFrame = mainFrame;
        // 注册订阅者（主面板自身）

    }

    /**
     * 初始化UI组件（子类实现）
     * 用于创建和布局子组件（如按钮、输入框）
     */
    @Override
    protected void initComponents() {
      initUI();
    }

    /**
     * 绑定事件监听器（子类实现）
     * 用于为组件绑定点击、输入等事件
     */
    @Override
    protected void bindEvents() {

    }

    /**
     * 面板显示后的回调：在{@link #showComponent()}执行完毕、面板完全可见后调用
     * 可在此处执行显示后的异步操作（如网络请求、动画启动）
     * 示例：游戏面板显示后开始倒计时、房间面板显示后请求房间列表
     */
    @Override
    public void onShow() {
        super.onShow();
    }

    /**
     * 面板隐藏后的回调：在{@link #hideComponent()}执行完毕、面板完全不可见后调用
     * 可在此处执行隐藏后的收尾操作（如保存当前状态到本地）
     * 示例：房间面板隐藏后保存当前输入的服务器IP和端口
     */
    @Override
    public void onHide() {
        super.onHide();
    }

    /**
     * 初始化UI面板
     */
    private void initUI() {
        setBorder(BorderFactory.createEmptyBorder());

        // 顶部区域：标题 + 右上角返回按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(40, 20, 60));
        topPanel.setOpaque(true);

        // 标题
        JLabel titleLabel = new JLabel("联网对战");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
        titleLabel.setForeground(new Color(255, 215, 0)); // 金色标题
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        // 右上角返回按钮（矢量图标）
        JButton backBtn = JButtonFactory.createBackButton();
        backBtn.addActionListener(e -> onBackClicked());
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        backPanel.setOpaque(false); // 透明背景
        backPanel.add(backBtn);
        topPanel.add(backPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 主内容区域
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBackground(new Color(40, 20, 60));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(mainContent, BorderLayout.CENTER);

        // 创建子面板
        connectionPanel = new ConnectionPanel();
        roomPanel = new RoomPanel();

        // 默认显示连接面板
        mainContent.add(connectionPanel.getComponent(), BorderLayout.CENTER);
    }

    /**
     * 返回主菜单
     */
    private void onBackClicked() {
        mainFrame.showPanel(GameLauncher.START_PANEL);
    }

    /**
     * 导航到其他面板
     */
    private void navigateToPanel(String panelType) {
        switch (panelType) {
            case "GAME_PANEL":
                // 切换到游戏面板
                mainFrame.showPanel(GameLauncher.BATTLE_PANEL);
                break;
            case "END_PANEL":
                // 切换到结束面板
//                mainFrame.showPanel(GameLauncher.);
                log.info("切换到结束面板");
                break;
        }
    }

    /**
     * 显示错误消息
     */
    private void showError(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "错误", JOptionPane.ERROR_MESSAGE)
        );
        connectionPanel.setConnectingState(false);
    }

    /**
     * 显示信息消息
     */
    private void showInfo(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "提示", JOptionPane.INFORMATION_MESSAGE)
        );
    }

    /**
     * 创建按钮的工具方法
     */
    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        button.setPreferredSize(new Dimension(160, 40));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }
}
    