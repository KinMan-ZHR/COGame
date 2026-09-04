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
 * AI 流派风格选择对话框：高对比度深色科技卡片界面，极简文案，毫秒级即时响应
 */
public class AiPlaystyleDialog extends JDialog {

    @FunctionalInterface
    public interface SelectionCallback {
        void onConfirmed(AiPlaystyle playstyle, person.kinman.cogame.core.model.TurnOrderPreference preference);
    }

    private AiPlaystyle selectedStyle = AiPlaystyle.ANTIGRAVITY;
    private person.kinman.cogame.core.model.TurnOrderPreference selectedPreference = person.kinman.cogame.core.model.TurnOrderPreference.FIRST;
    private final SelectionCallback callback;
    private final List<PlaystyleCard> cardList = new ArrayList<>();

    public AiPlaystyleDialog(JFrame parent, Consumer<AiPlaystyle> onConfirm) {
        this(parent, (style, pref) -> {
            if (onConfirm != null) {
                onConfirm.accept(style);
            }
        });
    }

    public AiPlaystyleDialog(JFrame parent, SelectionCallback callback) {
        super(parent, "选择 AI 对手与分先模式", true);
        this.callback = callback;

        this.setSize(530, 490);
        this.setLocationRelativeTo(parent);
        this.setResizable(false);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(11, 17, 32));
        contentPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. 标题区 (简练清爽，去除冗长废话)
        JLabel titleLabel = new JLabel("⚡ 选择挑战的 AI 流派与分先模式");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(248, 250, 252));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("支持自由选择先手/后手 (验证先手优势) · 双击卡片直接开战");
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        contentPanel.add(subLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // 2. 卡片容器：一次性初始化，点击时仅就地重绘，0毫秒延迟无卡顿
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

        // 3. 分先偏好选择条 (高对比度自绘制按钮)
        JPanel turnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        turnPanel.setOpaque(false);

        JLabel turnLabel = new JLabel("分先执子:");
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        turnLabel.setForeground(new Color(226, 232, 240));
        turnPanel.add(turnLabel);

        List<TurnPrefOptionButton> prefButtons = new ArrayList<>();
        Runnable refreshPrefButtons = () -> {
            for (TurnPrefOptionButton b : prefButtons) {
                b.setSelected(b.pref == selectedPreference);
            }
        };

        TurnPrefOptionButton btnFirst = new TurnPrefOptionButton(
                person.kinman.cogame.core.model.TurnOrderPreference.FIRST,
                "🔵 我执先手 (P1)",
                true,
                () -> {
                    selectedPreference = person.kinman.cogame.core.model.TurnOrderPreference.FIRST;
                    refreshPrefButtons.run();
                }
        );
        TurnPrefOptionButton btnSecond = new TurnPrefOptionButton(
                person.kinman.cogame.core.model.TurnOrderPreference.SECOND,
                "🔴 AI 执先 (P2)",
                false,
                () -> {
                    selectedPreference = person.kinman.cogame.core.model.TurnOrderPreference.SECOND;
                    refreshPrefButtons.run();
                }
        );
        TurnPrefOptionButton btnRandom = new TurnPrefOptionButton(
                person.kinman.cogame.core.model.TurnOrderPreference.RANDOM,
                "🎲 随机分先",
                false,
                () -> {
                    selectedPreference = person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
                    refreshPrefButtons.run();
                }
        );

        prefButtons.add(btnFirst);
        prefButtons.add(btnSecond);
        prefButtons.add(btnRandom);

        turnPanel.add(btnFirst);
        turnPanel.add(btnSecond);
        turnPanel.add(btnRandom);

        contentPanel.add(turnPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // 4. 底部开战按钮 (采用自绘暗黑按钮，杜绝白底反噬)
        DarkThemeHelper.DarkButton startBtn = new DarkThemeHelper.DarkButton(
                "⚔️ 选定此对手 · 立即开战",
                new Color(2, 132, 199),
                new Color(14, 165, 233),
                new Color(3, 105, 161),
                new Color(56, 189, 248),
                Color.WHITE
        );
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        startBtn.setPreferredSize(new Dimension(440, 42));
        startBtn.setMaximumSize(new Dimension(440, 42));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.addActionListener(e -> confirmAndStart());

        contentPanel.add(startBtn);
        this.setContentPane(contentPanel);
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
            this.callback.onConfirmed(selectedStyle, selectedPreference);
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

            this.setPreferredSize(new Dimension(464, 66));
            this.setMaximumSize(new Dimension(464, 66));
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

    /**
     * 自绘制分先执子选项按钮
     */
    private static class TurnPrefOptionButton extends JButton {
        final person.kinman.cogame.core.model.TurnOrderPreference pref;
        private boolean selected;

        public TurnPrefOptionButton(person.kinman.cogame.core.model.TurnOrderPreference pref,
                                   String label,
                                   boolean selected,
                                   Runnable onClick) {
            super(label);
            this.pref = pref;
            this.selected = selected;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setPreferredSize(new Dimension(130, 32));

            addActionListener(e -> onClick.run());
        }

        public void setSelected(boolean sel) {
            this.selected = sel;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (selected) {
                if (pref == person.kinman.cogame.core.model.TurnOrderPreference.FIRST) {
                    g2.setColor(new Color(14, 116, 144));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
                    g2.setColor(new Color(56, 189, 248));
                } else if (pref == person.kinman.cogame.core.model.TurnOrderPreference.SECOND) {
                    g2.setColor(new Color(180, 83, 9));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
                    g2.setColor(new Color(251, 191, 36));
                } else {
                    g2.setColor(new Color(109, 40, 217));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
                    g2.setColor(new Color(192, 132, 252));
                }
                g2.setStroke(new BasicStroke(1.8f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(new Color(21, 32, 54));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
                g2.setColor(new Color(51, 65, 85));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
                g2.setColor(new Color(148, 163, 184));
            }

            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }
}
