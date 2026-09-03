package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.controller.AiController;
import person.kinman.cogame.client.controller.LocalController;
import person.kinman.cogame.client.controller.OnlineController;

import javax.swing.*;
import java.awt.*;

/**
 * 启动主菜单：支持 6x6~13x13 棋盘规格选择与三大对战模式切换
 */
public class MainMenuFrame extends JFrame {
    private final JComboBox<String> boardSizeComboBox;

    public MainMenuFrame() {
        this.setTitle("COGame - 《端脑》隔断棋盘博弈 (Ver 2.0)");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(520, 520);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(15, 23, 42)); // 深邃夜空灰蓝
        mainPanel.setBorder(BorderFactory.createEmptyBorder(28, 45, 28, 45));

        // 1. 标题与副标题
        JLabel titleLabel = new JLabel("端 脑 · 封 锁 博 弈");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(new Color(248, 250, 252));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Die Now Grid Disconnection Strategy Game");
        subLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
        subLabel.setForeground(new Color(148, 163, 184));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(subLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // 2. 棋盘规格选择面板 (6x6 ~ 13x13)
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        sizePanel.setBackground(new Color(30, 41, 59));
        sizePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        sizePanel.setMaximumSize(new Dimension(420, 48));

        JLabel sizeLabel = new JLabel("棋盘规格 (Board Size):");
        sizeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        sizeLabel.setForeground(new Color(226, 232, 240));

        String[] sizeOptions = {
                "6 × 6 (经典原版 - 36格)",
                "7 × 7 (战术进阶 - 49格)",
                "8 × 8 (战略纵深 - 64格)",
                "9 × 9 (九宫迷阵 - 81格)",
                "10 × 10 (双位矩阵 - 100格)",
                "11 × 11 (广袤对决 - 121格)",
                "12 × 12 (宏大博弈 - 144格)",
                "13 × 13 (终极拓扑迷宫 - 169格)"
        };
        boardSizeComboBox = new JComboBox<>(sizeOptions);
        boardSizeComboBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        boardSizeComboBox.setSelectedIndex(0);

        sizePanel.add(sizeLabel);
        sizePanel.add(boardSizeComboBox);
        mainPanel.add(sizePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // 3. 模式选择按钮
        JButton btnLocal = createStyledButton("① 单机双人对战 (Local 2P)", new Color(2, 132, 199), new Color(3, 105, 161));
        btnLocal.addActionListener(e -> {
            int size = getSelectedBoardSize();
            new GameFrame(new LocalController(size)).display();
        });

        JButton btnAi = createStyledButton("② 人机挑战模式 (vs 端脑AI)", new Color(16, 185, 129), new Color(5, 150, 105));
        btnAi.addActionListener(e -> {
            int size = getSelectedBoardSize();
            new GameFrame(new AiController(size)).display();
        });

        JButton btnOnline = createStyledButton("③ 网络联机对战 (Online PvP)", new Color(147, 51, 234), new Color(126, 34, 206));
        btnOnline.addActionListener(e -> {
            showOnlineDialog(getSelectedBoardSize());
        });

        mainPanel.add(btnLocal);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 14)));
        mainPanel.add(btnAi);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 14)));
        mainPanel.add(btnOnline);

        // 4. 底部全屏与快捷键提示
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        JLabel tipLabel = new JLabel("提示：游戏中支持按 F11 一键切换自适应全屏");
        tipLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tipLabel.setForeground(new Color(100, 116, 139));
        tipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(tipLabel);

        this.add(mainPanel);
    }

    private int getSelectedBoardSize() {
        return boardSizeComboBox.getSelectedIndex() + 6;
    }

    private JButton createStyledButton(String text, Color bg, Color hoverBg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(420, 48));
        btn.setPreferredSize(new Dimension(420, 48));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showOnlineDialog(int defaultSize) {
        JTextField serverField = new JTextField("ws://127.0.0.1:8088");
        JTextField roomField = new JTextField("1001");
        JTextField nameField = new JTextField("玩家_" + (int) (Math.random() * 900 + 100));
        JComboBox<String> sizeBox = new JComboBox<>(new String[]{
                "6x6", "7x7", "8x8", "9x9", "10x10", "11x11", "12x12", "13x13"
        });
        sizeBox.setSelectedIndex(defaultSize - 6);

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("对战服务器 WebSocket 地址:"));
        panel.add(serverField);
        panel.add(new JLabel("房间编号 (相同房间号自动对战):"));
        panel.add(roomField);
        panel.add(new JLabel("我的昵称:"));
        panel.add(nameField);
        panel.add(new JLabel("棋盘规格 (房主设定):"));
        panel.add(sizeBox);

        int result = JOptionPane.showConfirmDialog(this, panel, "加入联机对战", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String server = serverField.getText().trim();
            String room = roomField.getText().trim();
            String name = nameField.getText().trim();
            int size = sizeBox.getSelectedIndex() + 6;
            if (!server.isEmpty() && !room.isEmpty()) {
                new GameFrame(new OnlineController(server, room, name, size)).display();
            }
        }
    }
}
