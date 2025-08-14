package person.kinman.cogame.sb.ui.page;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import person.kinman.cogame.client.ui.page.network.connection.ConnectionPanel;
import person.kinman.cogame.sb.event.BackToMainFrameEvent;
import person.kinman.cogame.sb.eventBus.GlobalEventBus;
import person.kinman.cogame.sb.ui.BasePanel;
import person.kinman.cogame.client.ui.tools.JButtonFactory;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * 连接面板和房间面板
 */
public class ConnectPanel extends BasePanel {
    private static final Logger log = LoggerFactory.getLogger(ConnectPanel.class);
    private JTextField ipField;
    private JTextField portField;
    private JButton connectBtn;
    private JLabel statusLabel; // 替换弹窗的状态标签
    private JButton backBtn;
    // 保存配置的节点（使用Java内置的Preferences）
    private static final Preferences PREFS = Preferences.userNodeForPackage(ConnectionPanel.class);

    public ConnectPanel() {

    }

    @Override
    protected void initListeners() {
        backBtn.addActionListener(e -> {
            GlobalEventBus.getUiBus().post(new BackToMainFrameEvent());
        });

    }


    /**
     * 初始化UI面板
     */
    @Override
    protected void initUI() {
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
     * 显示信息消息
     */
    private void showInfo(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "提示", JOptionPane.INFORMATION_MESSAGE)
        );
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
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBackground(new Color(40, 20, 60));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(mainContent, BorderLayout.CENTER);

        // 创建子面板
        mainContent.setLayout(new GridBagLayout());
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

        // 连接按钮
        connectBtn = createButton("连接服务器", new Color(50, 205, 50));
        mainContent.add(connectBtn, gbc);

        // 状态标签（替代弹窗，显示连接状态/错误信息）
        statusLabel = new JLabel("请输入服务器信息并点击连接");
        statusLabel.setForeground(new Color(255, 255, 204)); // 浅黄色，醒目且不刺眼
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainContent.add(statusLabel, gbc);
        // 从本地配置加载上次输入的IP和端口
        loadSavedConfig();
        return mainContent;
    }
}
    