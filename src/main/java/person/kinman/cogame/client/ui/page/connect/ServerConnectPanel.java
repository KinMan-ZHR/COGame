package person.kinman.cogame.client.ui.page.connect;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import person.kinman.cogame.client.contract.PanelId;
import person.kinman.cogame.client.event.ConnectionStatusEvent;
import person.kinman.cogame.client.event.SwitchToPanelEvent;
import person.kinman.cogame.client.eventBus.GlobalEventBus;
import person.kinman.cogame.client.ui.BasePanel;
import person.kinman.cogame.client.ui.button.JButtonFactory;
import person.kinman.cogame.client.ui.page.connect.enums.ConnectionButtonStatus;
import person.kinman.cogame.client.ui.page.connect.events.ServerConnectRequestEvent;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * 连接面板
 */
public class ServerConnectPanel extends BasePanel {
    private static final Logger log = LoggerFactory.getLogger(ServerConnectPanel.class);
    private JTextField ipField;
    private JTextField portField;
    private JButton connectBtn;
    private JLabel statusLabel; // 替换弹窗的状态标签
    private JButton backBtn;
    private ConnectionButtonStatus buttonStatus = ConnectionButtonStatus.DISCONNECTED;
    // 保存配置的节点（使用Java内置的Preferences）
    private static final Preferences PREFS = Preferences.userNodeForPackage(ServerConnectPanel.class);

    // 定义按钮颜色常量
    private static final Color CONNECT_COLOR = new Color(50, 205, 50); // 绿色
    private static final Color DISCONNECT_COLOR = new Color(220, 50, 50); // 红色
    private static final Color CONNECTING_COLOR = new Color(255, 165, 0); // 橙色

    public ServerConnectPanel() {

    }

    @Override
    protected void initListeners() {
        backBtn.addActionListener(e -> {
            GlobalEventBus.getUiBus().post(new SwitchToPanelEvent(PanelId.START_PANEL));
        });
        connectBtn.addActionListener(e -> {
            if (buttonStatus == ConnectionButtonStatus.DISCONNECTED) {
                // 当前未连接，发送连接请求
                updateConnectingState(ConnectionButtonStatus.CONNECTING);
                GlobalEventBus.getUiBus().post(new ServerConnectRequestEvent(true, ipField.getText(), portField.getText()));
            } else if (buttonStatus == ConnectionButtonStatus.CONNECTED) {
                // 当前已连接，发送断开连接请求
                // 这里需要添加断开连接的逻辑
                updateConnectingState(ConnectionButtonStatus.DISCONNECTING);
                GlobalEventBus.getUiBus().post(new ServerConnectRequestEvent(false, ipField.getText(), portField.getText()));
            }
        });

    }

    @Subscribe
    public void handleConnectStatusEvent(ConnectionStatusEvent event) {
        updateConnectingState(event.isConnected() ? ConnectionButtonStatus.CONNECTED : ConnectionButtonStatus.DISCONNECTED);
    }

    /**
     * 初始化UI面板
     */
    @Override
    protected void initUI() {
        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());

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

        // 右上角返回按钮
        backBtn = JButtonFactory.createBackButton();
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        backPanel.setOpaque(false); // 透明背景
        backPanel.add(backBtn);
        topPanel.add(backPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 主内容区域
        JPanel mainContent = buildCenterPanel();
        add(mainContent, BorderLayout.CENTER);
    }

    /**
     * 设置连接按钮状态
     */
    public void updateConnectingState(ConnectionButtonStatus connectionButtonStatus) {
        SwingUtilities.invokeLater(() -> {
            switch (connectionButtonStatus){
                case CONNECTING:
                    connectBtn.setText("连接中...");
                    connectBtn.setBackground(CONNECTING_COLOR);
                    connectBtn.setEnabled(false);
                    statusLabel.setForeground(new Color(255, 255, 204));
                    statusLabel.setText("正在连接到 " + ipField.getText() + ":" + portField.getText() + "...");
                    break;

                case CONNECTED:
                    connectBtn.setText("断开连接");
                    connectBtn.setBackground(DISCONNECT_COLOR);
                    connectBtn.setEnabled(true);
                    statusLabel.setForeground(new Color(144, 238, 144)); // 浅绿色
                    statusLabel.setText("已成功连接到服务器");
                    break;

                case DISCONNECTED:
                    connectBtn.setText("连接服务器");
                    connectBtn.setBackground(CONNECT_COLOR);
                    connectBtn.setEnabled(true);
                    statusLabel.setForeground(new Color(255, 255, 204));
                    statusLabel.setText("请输入服务器信息并点击连接");
                    break;

                case DISCONNECTING:
                    connectBtn.setText("正在断开连接...");
                    connectBtn.setBackground(CONNECTING_COLOR);
                    connectBtn.setEnabled(false);
                    statusLabel.setForeground(new Color(255, 255, 204));
                    statusLabel.setText("正在断开连接...");
                    break;
            }
            if (buttonStatus == ConnectionButtonStatus.CONNECTING && connectionButtonStatus == ConnectionButtonStatus.DISCONNECTED){
                statusLabel.setForeground(new Color(255, 99, 71)); // 浅红色
                statusLabel.setText("连接失败，请检查服务器信息");
            }
            if (buttonStatus == ConnectionButtonStatus.DISCONNECTING && connectionButtonStatus == ConnectionButtonStatus.CONNECTED){
                    statusLabel.setForeground(new Color(255, 99, 71)); // 浅红色
                    statusLabel.setText("断开连接失败");
            }
            buttonStatus = connectionButtonStatus;
            if (buttonStatus == ConnectionButtonStatus.CONNECTED){
                GlobalEventBus.getUiBus().post(new SwitchToPanelEvent(PanelId.ROOM_LIST_PANEL));
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
     * 加载上次输入的IP和端口
     */
    private void loadSavedConfig() {
        String savedIp = PREFS.get("last_ip", "localhost");
        String savedPort = PREFS.get("last_port", "12345");
        ipField.setText(savedIp);
        portField.setText(savedPort);
    }

    /**
     * 主面板
     */
    private JPanel buildCenterPanel(){
        JPanel mainContent = new JPanel(new GridBagLayout());
        mainContent.setBackground(new Color(40, 20, 60));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        // 服务器IP输入
        JLabel ipLabel = new JLabel("服务器IP: ");
        ipLabel.setForeground(Color.WHITE);
        mainContent.add(ipLabel, gbc);

        ipField = new JTextField("localhost", 20);
        mainContent.add(ipField, gbc);

        // 服务器端口输入
        JLabel portLabel = new JLabel("端口号: ");
        portLabel.setForeground(Color.WHITE);
        mainContent.add(portLabel, gbc);

        portField = new JTextField("12345", 10);
        mainContent.add(portField, gbc);

        // 连接按钮 - 初始为绿色"连接服务器"
        connectBtn = createButton("连接服务器", CONNECT_COLOR);
        mainContent.add(connectBtn, gbc);

        // 状态标签
        statusLabel = new JLabel("请输入服务器信息并点击连接");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        statusLabel.setForeground(new Color(255, 255, 204)); // 浅黄色
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainContent.add(statusLabel, gbc);

        // 从本地配置加载上次输入的IP和端口
        loadSavedConfig();
        return mainContent;
    }
}
    