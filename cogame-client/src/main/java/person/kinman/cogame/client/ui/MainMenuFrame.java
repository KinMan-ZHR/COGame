package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.controller.AiController;
import person.kinman.cogame.client.controller.LocalController;
import person.kinman.cogame.client.controller.OnlineController;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * 启动主菜单：高对比度深邃科技风界面，支持 6x6~13x13 棋盘规格选择与自绘高可见度模式按钮
 */
public class MainMenuFrame extends JFrame {
    private final JComboBox<String> boardSizeComboBox;

    public MainMenuFrame() {
        this.setTitle("COGame - 《端脑》隔断棋盘博弈 (Ver 2.1)");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(540, 560);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(11, 17, 32)); // 深邃墨蓝黑底色
        mainPanel.setBorder(BorderFactory.createEmptyBorder(28, 45, 28, 45));

        // 1. 标题与副标题
        JLabel titleLabel = new JLabel("端 脑 · 封 锁 博 弈");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(new Color(248, 250, 252));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Die Now Grid Disconnection Strategy Game");
        subLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
        subLabel.setForeground(new Color(148, 163, 184));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(subLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 22)));

        // 2. 棋盘规格选择面板 (6x6 ~ 13x13)
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        sizePanel.setBackground(new Color(21, 32, 54));
        sizePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        sizePanel.setMaximumSize(new Dimension(450, 50));

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
        boardSizeComboBox.setBackground(new Color(15, 23, 42));
        boardSizeComboBox.setForeground(Color.WHITE);
        boardSizeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (isSelected) {
                    label.setBackground(new Color(2, 132, 199));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(15, 23, 42));
                    label.setForeground(new Color(241, 245, 249));
                }
                return label;
            }
        });
        boardSizeComboBox.setSelectedIndex(0);

        sizePanel.add(sizeLabel);
        sizePanel.add(boardSizeComboBox);
        mainPanel.add(sizePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 22)));

        // 3. 模式选择按钮 (全自绘高对比度，彻底杜绝系统默认浅色按钮发白)
        ModernMenuButton btnLocal = new ModernMenuButton(
                "① 单机双人对战 (Local 2P)",
                "同屏双人 · 同一键盘交替轮流封锁",
                new Color(2, 132, 199),
                new Color(14, 165, 233),
                new Color(3, 105, 161),
                new Color(56, 189, 248)
        );
        btnLocal.addActionListener(e -> {
            int size = getSelectedBoardSize();
            new GameFrame(new LocalController(size)).display();
        });

        ModernMenuButton btnAi = new ModernMenuButton(
                "② 人机挑战模式 (vs 端脑AI)",
                "启发式算法 · 动态最短路径阻断与领地争夺",
                new Color(5, 150, 105),
                new Color(16, 185, 129),
                new Color(4, 120, 87),
                new Color(52, 211, 153)
        );
        btnAi.addActionListener(e -> {
            int size = getSelectedBoardSize();
            new GameFrame(new AiController(size)).display();
        });

        ModernMenuButton btnOnline = new ModernMenuButton(
                "③ 网络联机对战 (Online PvP)",
                "WebSocket 跨网互联 · 房间号秒配对战",
                new Color(124, 58, 237),
                new Color(147, 51, 234),
                new Color(109, 40, 217),
                new Color(192, 132, 252)
        );
        btnOnline.addActionListener(e -> {
            showOnlineDialog(getSelectedBoardSize());
        });

        mainPanel.add(btnLocal);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 14)));
        mainPanel.add(btnAi);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 14)));
        mainPanel.add(btnOnline);

        // 4. 底部全屏与快捷键提示
        mainPanel.add(Box.createRigidArea(new Dimension(0, 22)));
        JLabel tipLabel = new JLabel("★ 对局中支持按 F11 一键切换自适应全屏 | P 键开关连通路径高亮");
        tipLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tipLabel.setForeground(new Color(100, 116, 139));
        tipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(tipLabel);

        this.add(mainPanel);
    }

    private int getSelectedBoardSize() {
        return boardSizeComboBox.getSelectedIndex() + 6;
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

    /**
     * 自绘制高对比度现代化模式选择按钮（杜绝各操作系统原生白底遮蔽）
     */
    private static class ModernMenuButton extends JButton {
        private final String subtitle;
        private final Color normalBg;
        private final Color hoverBg;
        private final Color pressedBg;
        private final Color borderColor;

        public ModernMenuButton(String title, String subtitle, Color normalBg, Color hoverBg, Color pressedBg, Color borderColor) {
            super(title);
            this.subtitle = subtitle;
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            this.pressedBg = pressedBg;
            this.borderColor = borderColor;

            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setMaximumSize(new Dimension(450, 56));
            setPreferredSize(new Dimension(450, 56));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();

            Color currentBg = normalBg;
            if (getModel().isPressed()) {
                currentBg = pressedBg;
            } else if (getModel().isRollover()) {
                currentBg = hoverBg;
            }

            // 1. 绘制渐变科技底色
            GradientPaint gp = new GradientPaint(0, 0, currentBg, 0, h, currentBg.darker());
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));

            // 2. 绘制醒目外轮廓荧光边框
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));

            // 3. 绘制标题（纯白超高对比度粗体）
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            int titleX = 22;
            int titleY = 24;
            g2.drawString(getText(), titleX, titleY);

            // 4. 绘制说明副标题（浅蓝高可读字体）
            g2.setColor(new Color(224, 242, 254));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString(subtitle, titleX, titleY + 19);

            // 5. 右侧醒目箭头
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2.drawString("➔", w - 30, h / 2 + 5);

            g2.dispose();
        }
    }
}
