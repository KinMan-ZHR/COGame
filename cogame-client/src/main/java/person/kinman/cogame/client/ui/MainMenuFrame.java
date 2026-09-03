package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.controller.AiController;
import person.kinman.cogame.client.controller.LocalController;
import person.kinman.cogame.client.controller.OnlineController;

import javax.swing.*;
import java.awt.*;

/**
 * 启动主菜单：模式选择器
 */
public class MainMenuFrame extends JFrame {

    public MainMenuFrame() {
        this.setTitle("COGame - 《端脑》隔断棋盘博弈 (Ver 2.0)");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(480, 420);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(44, 62, 80));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // 标题
        JLabel titleLabel = new JLabel("端 脑 · 6×6 封 锁 博 弈");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(new Color(236, 240, 241));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Die Now Grid Disconnection Game");
        subLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        subLabel.setForeground(new Color(189, 195, 199));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(subLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 35)));

        // 模式按钮
        JButton btnLocal = createStyledButton("① 单机双人对战 (Local 2P)", new Color(52, 152, 219));
        btnLocal.addActionListener(e -> {
            new GameFrame(new LocalController()).display();
        });

        JButton btnAi = createStyledButton("② 人机挑战模式 (vs 端脑AI)", new Color(46, 204, 113));
        btnAi.addActionListener(e -> {
            new GameFrame(new AiController()).display();
        });

        JButton btnOnline = createStyledButton("③ 网络联机对战 (Online PvP)", new Color(155, 89, 182));
        btnOnline.addActionListener(e -> {
            showOnlineDialog();
        });

        mainPanel.add(btnLocal);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(btnAi);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(btnOnline);

        this.add(mainPanel);
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(360, 45));
        btn.setPreferredSize(new Dimension(360, 45));
        return btn;
    }

    private void showOnlineDialog() {
        JTextField serverField = new JTextField("ws://127.0.0.1:8088");
        JTextField roomField = new JTextField("1001");
        JTextField nameField = new JTextField("玩家_" + (int)(Math.random() * 900 + 100));

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("对战服务器 WebSocket 地址:"));
        panel.add(serverField);
        panel.add(new JLabel("房间编号 (相同房间号自动对战):"));
        panel.add(roomField);
        panel.add(new JLabel("我的昵称:"));
        panel.add(nameField);

        int result = JOptionPane.showConfirmDialog(this, panel, "加入联机对战", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String server = serverField.getText().trim();
            String room = roomField.getText().trim();
            String name = nameField.getText().trim();
            if (!server.isEmpty() && !room.isEmpty()) {
                new GameFrame(new OnlineController(server, room, name)).display();
            }
        }
    }
}
