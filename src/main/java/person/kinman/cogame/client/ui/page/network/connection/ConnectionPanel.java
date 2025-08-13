package person.kinman.cogame.client.ui.page.network.connection;

import person.kinman.cogame.client.ui.tools.BasePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

/**
 * 连接面板 - 仅负责服务器连接相关的UI展示和用户交互
 */
public class ConnectionPanel extends BasePanel{
    private JTextField ipField;
    private JTextField portField;
    private JButton connectBtn;
    private JLabel statusLabel; // 替换弹窗的状态标签
    private final ConnectionPanelController controller; // 自身对应的控制器
    // 保存配置的节点（使用Java内置的Preferences）
    private static final Preferences PREFS = Preferences.userNodeForPackage(ConnectionPanel.class);

    public ConnectionPanel() {
        this.controller = new ConnectionPanelController();
    }
    /**
     * 初始化UI组件（子类实现）
     * 用于创建和布局子组件（如按钮、输入框）
     */
    @Override
    protected void initComponents() {
        buildConnectionPanelUI_1();
    }

    /**
     * 绑定事件监听器（子类实现）
     * 用于为组件绑定点击、输入等事件
     */
    @Override
    protected void bindEvents() {

        connectBtn.addActionListener(this::onConnectClicked);

        // 3. 绑定控制器的回调到UI更新（子面板自主处理）
        controller.setButtonStatusTextUpdater(this::setStatusText);
        controller.setErrorHandler(this::showError);
        controller.setConnectionStateUpdater(this::setConnectingState);
    }

    private void buildConnectionPanelUI_1() {
        setBackground(new Color(40, 20, 60));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        // 服务器IP输入
        JLabel ipLabel = new JLabel("服务器IP: ");
        ipLabel.setForeground(Color.WHITE);
        add(ipLabel, gbc);

        ipField = new JTextField("localhost", 20);
        add(ipField, gbc);

        // 服务器端口输入
        JLabel portLabel = new JLabel("端口号: ");
        portLabel.setForeground(Color.WHITE);
        add(portLabel, gbc);

        portField = new JTextField("12345", 10);
        add(portField, gbc);

        // 连接按钮
        connectBtn = createButton("连接服务器", new Color(50, 205, 50));
        add(connectBtn, gbc);

        // 状态标签（替代弹窗，显示连接状态/错误信息）
        statusLabel = new JLabel("请输入服务器信息并点击连接");
        statusLabel.setForeground(new Color(255, 255, 204)); // 浅黄色，醒目且不刺眼
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, gbc);

        // 从本地配置加载上次输入的IP和端口
        loadSavedConfig();
    }

    /**
     * 从本地配置加载上次保存的IP和端口
     */
    private void loadSavedConfig() {
        String savedIp = PREFS.get("last_ip", "localhost");
        String savedPort = PREFS.get("last_port", "12345");
        ipField.setText(savedIp);
        portField.setText(savedPort);
    }

    /**
     * 获取输入的IP地址
     */
    public String getIpAddress() {
        return ipField.getText().trim();
    }

    /**
     * 获取输入的端口号
     */
    public String getPort() {
        return portField.getText().trim();
    }

    /**
     * 显示后回调：聚焦到IP输入框
     */
    @Override
    public void onShow() {
        ipField.requestFocusInWindow();
    }

    /**
     * 隐藏后回调：保存当前输入的IP和端口（可选）
     */
    @Override
    public void onHide() {
        // 可在此处将IP和端口保存到本地配置
        PREFS.put("last_ip", ipField.getText().trim());
        PREFS.put("last_port", portField.getText().trim());
    }

    /**
     * 设置连接按钮状态
     */
    public void setConnectingState(boolean isConnecting) {
        SwingUtilities.invokeLater(() -> {
            if (isConnecting) {
                connectBtn.setText("连接中...");
                connectBtn.setEnabled(false);
                statusLabel.setForeground(new Color(255, 255, 204)); // 恢复正常状态色
                statusLabel.setText("正在连接到 " + ipField.getText() + ":" + portField.getText() + "...");
            } else {
                connectBtn.setText("连接服务器");
                connectBtn.setEnabled(true);
            }
        });
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

    /**
     * 连接按钮点击逻辑
     */
    private void onConnectClicked(ActionEvent e) {
        String ip = ipField.getText().trim();
        String port = portField.getText().trim();
        controller.connect(ip, port); // 调用控制器处理连接逻辑
        setConnectingState(true); // 禁用按钮，显示"连接中..."
    }

    public void setStatusText(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "错误", JOptionPane.ERROR_MESSAGE);
    }
}