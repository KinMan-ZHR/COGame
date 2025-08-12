package person.kinman.cogame.client.ui.page;

import person.kinman.cogame.client.network.NetworkProxy;
import person.kinman.cogame.client.ui.GameLauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 联网对战面板 - 包含服务器配置和网络对战逻辑
 */
public class OnlineGamePanel extends JPanel {
    private GameLauncher mainFrame;
    private JTextField ipField;
    private JTextField portField;
    private NetworkProxy networkProxy;

    public OnlineGamePanel(GameLauncher mainFrame) {
        this.mainFrame = mainFrame;
        this.networkProxy = new NetworkProxy(
                this:: onServerMessage,
                this:: onConnectionStatusChanged
        );
        initUI();
    }

    private void initUI() {
        setBackground(new Color(40, 20, 60));
        setLayout(new BorderLayout());

        // 标题
        JLabel titleLabel = new JLabel("联网对战");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // 服务器配置区域
        add(createConfigPanel(), BorderLayout.CENTER);

        // 返回按钮
        JButton backBtn = createButton("返回主菜单", new Color(178, 34, 34));
        backBtn.addActionListener(e -> mainFrame.showPanel(GameLauncher.START_PANEL));
        
        JPanel backPanel = new JPanel();
        backPanel.setBackground(new Color(40, 20, 60));
        backPanel.add(backBtn);
        add(backPanel, BorderLayout.SOUTH);
    }

    /**
     * 创建服务器配置面板
     */
    private JPanel createConfigPanel() {
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(new Color(40, 20, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // 服务器IP输入
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel ipLabel = new JLabel("服务器IP: ");
        ipLabel.setForeground(Color.WHITE);
        configPanel.add(ipLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        ipField = new JTextField("localhost", 20);
        configPanel.add(ipField, gbc);

        // 服务器端口输入
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel portLabel = new JLabel("端口号: ");
        portLabel.setForeground(Color.WHITE);
        configPanel.add(portLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        portField = new JTextField("12345", 10);
        configPanel.add(portField, gbc);

        // 连接按钮
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton connectBtn = createButton("连接服务器", new Color(50, 205, 50));
        connectBtn.addActionListener(this::connectToServer);
        configPanel.add(connectBtn, gbc);

        // 断开连接按钮
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton disconnectBtn = createButton("断开连接", new Color(255, 140, 0));
        disconnectBtn.addActionListener(e -> networkProxy.disconnect());
        configPanel.add(disconnectBtn, gbc);

        return configPanel;
    }

    /**
     * 连接服务器逻辑
     */
    private void connectToServer(ActionEvent e) {
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();
        
        // 简单验证输入
        if (ip.isEmpty() || portStr.isEmpty()) {
            showError("请输入服务器IP和端口");
            return;
        }
        
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                showError("端口号必须在1024-65535之间");
                return;
            }
            
            // 实际开发中添加Socket连接代码
            networkProxy.connect(ip, port);
            
        } catch (NumberFormatException ex) {
            showError("端口号必须是数字");
        }
    }

    /**
     * 创建按钮
     */
    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        button.setPreferredSize(new Dimension(160, 50));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "错误", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 发送消息到服务器
     */
    public void sendMessage(String message) {
        networkProxy.sendMessage(message);
    }

    /**
     * 处理来自服务器的消息
     */
    private void onServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            // 处理服务器消息，如更新游戏状态等
            showInfo("收到消息: " + message);

            // 根据消息类型进行不同处理
            if (message.equals("对手已加入")) {
                showInfo("游戏开始！");
                // 切换到游戏对战界面
                 mainFrame.showPanel(GameLauncher.BATTLE_PANEL);
            }
        });
    }

    /**
     * 处理连接状态变化
     */
    private void onConnectionStatusChanged(boolean isConnected, String message) {
        SwingUtilities.invokeLater(() -> {
            if (isConnected) {
                showInfo(message);
            } else {
                showError(message);
            }
        });
    }

}
