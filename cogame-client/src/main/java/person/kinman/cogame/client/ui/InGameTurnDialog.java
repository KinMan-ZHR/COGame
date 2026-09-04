package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.controller.GameController;
import person.kinman.cogame.core.model.TurnOrderPreference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * 局内分先与新一局就绪对话框：
 * 原生支持在对局内、终局结算时自由分先（玩家先手、AI/对手先手、随机掷骰、互换席位）
 */
public class InGameTurnDialog extends JDialog {
    private final GameController controller;
    private final boolean isGameOver;

    public InGameTurnDialog(Frame parent, GameController controller, boolean isGameOver) {
        super(parent, isGameOver ? "COGame - 终局结算与新局分先" : "COGame - 局内分先与开启新局", true);
        this.controller = controller;
        this.isGameOver = isGameOver;

        this.setSize(520, 430);
        this.setLocationRelativeTo(parent);
        this.setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(DarkThemeHelper.COLOR_BG_DARKEST);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248), 1),
                BorderFactory.createEmptyBorder(18, 22, 16, 22)
        ));

        // 1. 顶部 Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        String titleText = isGameOver ? "🏆 战局已定！新一局分先执子" : "⚔️ 局内分先 · 开启新一局";
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(isGameOver ? new Color(251, 191, 36) : new Color(56, 189, 248));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String descText = "<html><center style='color:#94a3b8; font-size:12px;'>"
                + controller.getModeName() + "<br/>"
                + "请选择下一局谁先走第一步 (联机双方同选先手将由系统公平掷骰裁定)："
                + "</center></html>";
        JLabel descLabel = new JLabel(descText);
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        headerPanel.add(descLabel);

        root.add(headerPanel, BorderLayout.NORTH);

        // 2. 中间卡片选项 (4 张大尺寸战术卡)
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setOpaque(false);

        cardsPanel.add(new OptionCard(
                "🔵 我执先手 (P1)",
                "先发制人，率先行动占领中枢与边角",
                new Color(6, 182, 212),
                () -> selectPreference(TurnOrderPreference.FIRST)
        ));
        cardsPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        cardsPanel.add(new OptionCard(
                "🔴 对手 / AI 执先 (P2)",
                "防守反击，由对方率先出招，我执后手测试破局策略",
                new Color(245, 158, 11),
                () -> selectPreference(TurnOrderPreference.SECOND)
        ));
        cardsPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        cardsPanel.add(new OptionCard(
                "🎲 双方公平掷骰 (随机分先)",
                "50/50 绝对公平，双方均选先手由服务端公平掷骰裁定",
                new Color(16, 185, 129),
                () -> selectPreference(TurnOrderPreference.RANDOM)
        ));
        cardsPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        cardsPanel.add(new OptionCard(
                "🔄 交换上局攻守席位",
                "红蓝攻守互换，换边再战下一局！",
                new Color(168, 85, 247),
                () -> {
                    dispose();
                    controller.swapTurnOrder();
                }
        ));

        root.add(cardsPanel, BorderLayout.CENTER);

        // 3. 底部取消栏
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        DarkThemeHelper.DarkButton btnCancel = new DarkThemeHelper.DarkButton(
                isGameOver ? "查看终局盘面" : "取消",
                new Color(30, 41, 59), new Color(51, 65, 85), DarkThemeHelper.COLOR_BORDER
        );
        btnCancel.setPreferredSize(new Dimension(120, 32));
        btnCancel.addActionListener(e -> dispose());
        bottomBar.add(btnCancel);

        root.add(bottomBar, BorderLayout.SOUTH);

        this.setContentPane(root);
    }

    private void selectPreference(TurnOrderPreference preference) {
        dispose();
        controller.resetGameWithPreference(preference);
    }

    /**
     * 极简现代战术选项卡
     */
    private static class OptionCard extends JPanel {
        private final String title;
        private final String desc;
        private final Color accent;
        private final Runnable onClick;
        private boolean hovered = false;

        public OptionCard(String title, String desc, Color accent, Runnable onClick) {
            this.title = title;
            this.desc = desc;
            this.accent = accent;
            this.onClick = onClick;

            this.setPreferredSize(new Dimension(460, 56));
            this.setMaximumSize(new Dimension(460, 56));
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
            this.setOpaque(false);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        OptionCard.this.onClick.run();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            Color bg = hovered ? new Color(21, 34, 58) : new Color(15, 23, 42);
            Color border = hovered ? accent : new Color(40, 52, 75);

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));

            g2.setColor(border);
            g2.setStroke(new BasicStroke(hovered ? 1.8f : 1.0f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));

            // 左侧高亮色条
            g2.setColor(accent);
            g2.fill(new RoundRectangle2D.Float(0, 0, 4, h, 4, 4));

            // 标题
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.setColor(hovered ? Color.WHITE : new Color(241, 245, 249));
            g2.drawString(title, 16, 23);

            // 副标题说明
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(hovered ? new Color(203, 213, 225) : new Color(148, 163, 184));
            g2.drawString(desc, 16, 43);

            // 右侧箭头
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.setColor(hovered ? accent : new Color(71, 85, 105));
            g2.drawString("➔", w - 24, 33);

            g2.dispose();
        }
    }
}
