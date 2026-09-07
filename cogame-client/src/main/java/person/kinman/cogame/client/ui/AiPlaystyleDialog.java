package person.kinman.cogame.client.ui;

import person.kinman.cogame.ai.AiPlaystyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * AI 流派风格选择对话框：高对比度深色科技卡片界面，集成 3~10 档思考深度滑动调节与极简双国手选择
 */
public class AiPlaystyleDialog extends JDialog {

    @FunctionalInterface
    public interface SelectionCallback {
        void onConfirmed(AiPlaystyle playstyle, int searchDepth, person.kinman.cogame.core.model.TurnOrderPreference preference);
    }

    private AiPlaystyle selectedStyle = AiPlaystyle.CE_TIAN;
    private int selectedDepth = 5;
    private final SelectionCallback callback;
    private final List<PlaystyleCard> cardList = new ArrayList<>();
    private JLabel depthBadge;

    public AiPlaystyleDialog(JFrame parent, Consumer<AiPlaystyle> onConfirm) {
        this(parent, (style, depth, pref) -> {
            if (onConfirm != null) {
                onConfirm.accept(style);
            }
        });
    }

    public AiPlaystyleDialog(JFrame parent, SelectionCallback callback) {
        super(parent, "选择挑战的 AI 棋风流派与思考深度", true);
        this.callback = callback;

        this.setSize(540, 480);
        this.setLocationRelativeTo(parent);
        this.setResizable(false);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(11, 17, 32));
        contentPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. 标题区 (简练清爽，聚焦流派选择)
        JLabel titleLabel = new JLabel("⚡ 选择挑战的 AI 战略国手");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(248, 250, 252));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("双击流派卡片或点击下方按钮直接进局开战 · 局内顶栏支持随时分先与换先");
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        contentPanel.add(subLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // 2. 双流派卡片容器 (策天 vs 绝影)
        JPanel cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setOpaque(false);

        for (AiPlaystyle style : AiPlaystyle.values()) {
            boolean isSelected = (style == selectedStyle);
            PlaystyleCard card = new PlaystyleCard(
                    style,
                    isSelected,
                    () -> selectStyle(style),
                    () -> {
                        selectStyle(style);
                        confirmAndStart();
                    }
            );
            cardList.add(card);
            cardsContainer.add(card);
            cardsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        contentPanel.add(cardsContainer);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // 3. 思考深度滑动条控制区 (最低 3，默认 5，最大 10)
        JPanel depthPanel = new JPanel();
        depthPanel.setLayout(new BoxLayout(depthPanel, BoxLayout.Y_AXIS));
        depthPanel.setOpaque(false);
        depthPanel.setBackground(new Color(15, 23, 42));
        depthPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30, 41, 59), 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        // 深度标头行
        JPanel depthHeader = new JPanel(new BorderLayout());
        depthHeader.setOpaque(false);

        JLabel depthTitle = new JLabel("🧠 策略引擎思考深度 (前瞻推演层数):");
        depthTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        depthTitle.setForeground(new Color(226, 232, 240));

        depthBadge = new JLabel(getDepthDescription(5));
        depthBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
        depthBadge.setForeground(new Color(56, 189, 248));

        depthHeader.add(depthTitle, BorderLayout.WEST);
        depthHeader.add(depthBadge, BorderLayout.EAST);
        depthPanel.add(depthHeader);
        depthPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        // JSlider
        JSlider slider = new JSlider(3, 10, 5);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        slider.setOpaque(false);
        slider.setForeground(new Color(148, 163, 184));
        slider.setFont(new Font("SansSerif", Font.BOLD, 11));

        slider.addChangeListener(e -> {
            selectedDepth = slider.getValue();
            depthBadge.setText(getDepthDescription(selectedDepth));
            Color badgeColor = switch (selectedDepth) {
                case 3, 4 -> new Color(52, 211, 153);
                case 5, 6 -> new Color(56, 189, 248);
                case 7, 8 -> new Color(245, 158, 11);
                default -> new Color(239, 68, 68);
            };
            depthBadge.setForeground(badgeColor);
        });

        depthPanel.add(slider);
        contentPanel.add(depthPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // 4. 底部开战按钮
        DarkThemeHelper.DarkButton startBtn = new DarkThemeHelper.DarkButton(
                "⚔️ 选定此流派 · 立即进入对局",
                new Color(2, 132, 199),
                new Color(14, 165, 233),
                new Color(3, 105, 161),
                new Color(56, 189, 248),
                Color.WHITE
        );
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        startBtn.setPreferredSize(new Dimension(460, 42));
        startBtn.setMaximumSize(new Dimension(460, 42));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.addActionListener(e -> confirmAndStart());

        contentPanel.add(startBtn);
        this.setContentPane(contentPanel);
    }

    private String getDepthDescription(int depth) {
        return switch (depth) {
            case 3 -> "3 层 · 极速响应";
            case 4 -> "4 层 · 敏捷攻防";
            case 5 -> "5 层 (推荐默认 · 均衡深算)";
            case 6 -> "6 层 · 深度推演";
            case 7 -> "7 层 · 高阶宏图";
            case 8 -> "8 层 · 大师推演";
            case 9 -> "9 层 · 巅峰攻杀";
            case 10 -> "10 层 (国手神算 · 极限深搜)";
            default -> depth + " 层推演";
        };
    }

    private void selectStyle(AiPlaystyle newStyle) {
        this.selectedStyle = newStyle;
        for (PlaystyleCard card : cardList) {
            card.setSelected(card.getStyle() == newStyle);
        }
    }

    private void confirmAndStart() {
        dispose();
        if (this.callback != null) {
            this.callback.onConfirmed(selectedStyle, selectedDepth, person.kinman.cogame.core.model.TurnOrderPreference.FIRST);
        }
    }

    /**
     * 自绘高性能流派卡片：单组件无嵌套，防抖动，零重布局延迟
     */
    private static class PlaystyleCard extends JPanel {
        private final AiPlaystyle style;
        private final Color accentColor;
        private final Runnable onClick;
        private final Runnable onDoubleClick;
        private boolean selected;
        private boolean hovered;

        public PlaystyleCard(AiPlaystyle style, boolean selected, Runnable onClick, Runnable onDoubleClick) {
            this.style = style;
            this.selected = selected;
            this.onClick = onClick;
            this.onDoubleClick = onDoubleClick;
            this.accentColor = Color.decode(style.getColorHex());

            this.setPreferredSize(new Dimension(488, 68));
            this.setMaximumSize(new Dimension(488, 68));
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
            this.setOpaque(false);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() >= 2) {
                        PlaystyleCard.this.onDoubleClick.run();
                    } else {
                        PlaystyleCard.this.onClick.run();
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

        public AiPlaystyle getStyle() {
            return style;
        }

        public void setSelected(boolean selected) {
            if (this.selected != selected) {
                this.selected = selected;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. 卡片底色
            Color bg;
            if (selected) {
                bg = new Color(21, 34, 58);
            } else if (hovered) {
                bg = new Color(24, 36, 56);
            } else {
                bg = new Color(15, 23, 42);
            }
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));

            // 2. 边框高亮
            if (selected) {
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(2.0f));
            } else if (hovered) {
                g2.setColor(new Color(94, 115, 145));
                g2.setStroke(new BasicStroke(1.2f));
            } else {
                g2.setColor(new Color(40, 52, 75));
                g2.setStroke(new BasicStroke(1.0f));
            }
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));

            // 3. 左侧主标题 (流派角色名)
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2.setColor(selected ? accentColor : new Color(241, 245, 249));
            g2.drawString(style.getDisplayName(), 18, 28);

            // 4. 左侧副标题 (精简特色)
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(selected ? new Color(203, 213, 225) : new Color(148, 163, 184));
            g2.drawString(style.getTagline(), 18, 50);

            // 5. 右侧单选状态指示圈
            int radioX = w - 38;
            int radioY = (h - 18) / 2;
            if (selected) {
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(radioX, radioY, 18, 18);
                g2.fillOval(radioX + 4, radioY + 4, 10, 10);
            } else {
                g2.setColor(hovered ? new Color(148, 163, 184) : new Color(71, 85, 105));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(radioX, radioY, 18, 18);
            }

            g2.dispose();
        }
    }
}
