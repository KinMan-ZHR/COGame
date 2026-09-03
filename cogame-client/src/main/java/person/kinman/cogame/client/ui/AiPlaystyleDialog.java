package person.kinman.cogame.client.ui;

import person.kinman.cogame.ai.AiPlaystyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * AI 流派风格选择对话框：高对比度深色科技卡片界面，支持选择不同性格算法的 AI 对手
 */
public class AiPlaystyleDialog extends JDialog {

    private AiPlaystyle selectedStyle = AiPlaystyle.ANTIGRAVITY;
    private final Consumer<AiPlaystyle> onConfirm;
    private final JPanel cardsContainer;

    public AiPlaystyleDialog(JFrame parent, Consumer<AiPlaystyle> onConfirm) {
        super(parent, "选择挑战的 AI 流派风格", true);
        this.onConfirm = onConfirm;

        this.setSize(580, 520);
        this.setLocationRelativeTo(parent);
        this.setResizable(false);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(11, 17, 32));
        contentPanel.setBorder(new EmptyBorder(22, 28, 22, 28));

        // 1. 标题区
        JLabel titleLabel = new JLabel("⚡ 选择您的 AI 对手流派");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(new Color(248, 250, 252));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("无需生硬的难度划分，不同 AI 拥有截然不同的图论算法哲学与实战棋风");
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        contentPanel.add(subLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 18)));

        // 2. 三大流派卡片容器
        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setOpaque(false);

        refreshCards();
        contentPanel.add(cardsContainer);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // 3. 底部开战按钮
        JButton startBtn = new JButton("⚔️ 选定此流派 · 立即进入对局");
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        startBtn.setForeground(Color.WHITE);
        startBtn.setBackground(new Color(14, 165, 233));
        startBtn.setFocusPainted(false);
        startBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        startBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.setMaximumSize(new Dimension(420, 48));

        startBtn.addActionListener(e -> {
            dispose();
            if (this.onConfirm != null) {
                this.onConfirm.accept(selectedStyle);
            }
        });

        contentPanel.add(startBtn);
        this.setContentPane(contentPanel);
    }

    private void refreshCards() {
        cardsContainer.removeAll();
        for (AiPlaystyle style : AiPlaystyle.values()) {
            boolean isSelected = (style == selectedStyle);
            JPanel card = createPlaystyleCard(style, isSelected);
            cardsContainer.add(card);
            cardsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private JPanel createPlaystyleCard(AiPlaystyle style, boolean isSelected) {
        Color accentColor = Color.decode(style.getColorHex());
        JPanel card = new JPanel(new BorderLayout(10, 6));
        card.setOpaque(true);
        card.setBackground(isSelected ? new Color(21, 32, 54) : new Color(15, 23, 42));
        card.setMaximumSize(new Dimension(520, 92));
        card.setPreferredSize(new Dimension(520, 92));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color borderColor = isSelected ? accentColor : new Color(51, 65, 85);
        int borderWidth = isSelected ? 2 : 1;
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, borderWidth, true),
                new EmptyBorder(8, 14, 8, 14)
        ));

        // 顶部信息：流派名 + 勾选状态
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(style.getDisplayName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        nameLabel.setForeground(isSelected ? accentColor : new Color(241, 245, 249));

        JLabel statusLabel = new JLabel(isSelected ? "● 已选定 (Selected)" : "○ 点击选择");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setForeground(isSelected ? accentColor : new Color(100, 116, 139));

        topPanel.add(nameLabel, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.EAST);

        // 中部：一句话标签
        JLabel tagLabel = new JLabel(style.getTagline());
        tagLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tagLabel.setForeground(new Color(203, 213, 225));

        // 底部：详细棋风描述
        JLabel descLabel = new JLabel(style.getDetailDescription());
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        descLabel.setForeground(new Color(148, 163, 184));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(topPanel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        textPanel.add(tagLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        textPanel.add(descLabel);

        card.add(textPanel, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedStyle = style;
                refreshCards();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (selectedStyle != style) {
                    card.setBackground(new Color(30, 41, 59));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (selectedStyle != style) {
                    card.setBackground(new Color(15, 23, 42));
                }
            }
        });

        return card;
    }
}
