package person.kinman.cogame.client.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * 客户端全局深色科技风主题辅助工具：
 * 杜绝 Windows / Linux / macOS 各操作系统原生亮白色浅色组件发白、亮片或遮蔽问题
 */
public class DarkThemeHelper {
    public static final Color COLOR_BG_DARKEST = new Color(11, 17, 32);   // #0b1120 深邃黑蓝
    public static final Color COLOR_BG_PANEL = new Color(15, 23, 42);     // #0f172a 深蓝背景
    public static final Color COLOR_BG_INPUT = new Color(30, 41, 59);     // #1e293b 输入框/卡片
    public static final Color COLOR_BG_ROW_ALT = new Color(21, 32, 54);   // #152036 斑马条纹交替色
    public static final Color COLOR_BORDER = new Color(51, 65, 85);       // #334155 分隔线
    public static final Color COLOR_BORDER_FOCUS = new Color(56, 189, 248); // #38bdf8 电光青
    public static final Color COLOR_TEXT_PRIMARY = new Color(248, 250, 252); // #f8fafc 纯白高对比
    public static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184); // #94a3b8 亮灰色说明

    /**
     * 自绘制高对比度深色按钮，彻底屏蔽操作系统原生 ButtonUI 导致的发白、亮片与白底反噬
     */
    public static class DarkButton extends JButton {
        private final Color normalBg;
        private final Color hoverBg;
        private final Color pressedBg;
        private final Color borderColor;
        private final Color textColor;

        public DarkButton(String text, Color normalBg, Color hoverBg, Color borderColor) {
            this(text, normalBg, hoverBg, normalBg.darker(), borderColor, Color.WHITE);
        }

        public DarkButton(String text, Color normalBg, Color hoverBg, Color pressedBg, Color borderColor, Color textColor) {
            super(text);
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            this.pressedBg = pressedBg;
            this.borderColor = borderColor;
            this.textColor = textColor;

            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(new Font("SansSerif", Font.BOLD, 13));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();

            Color bg = normalBg;
            if (getModel().isPressed()) {
                bg = pressedBg;
            } else if (getModel().isRollover()) {
                bg = hoverBg;
            }

            // 1. 绘制圆角背景
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));

            // 2. 绘制醒目外轮廓边框
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8));

            // 3. 居中绘制高对比度文字
            g2.setColor(textColor);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textX = (w - fm.stringWidth(getText())) / 2;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }
    }

    public static void styleDarkTextField(JTextField field) {
        field.setBackground(COLOR_BG_INPUT);
        field.setForeground(COLOR_TEXT_PRIMARY);
        field.setCaretColor(COLOR_BORDER_FOCUS);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    public static void styleDarkPasswordField(JPasswordField field) {
        field.setBackground(COLOR_BG_INPUT);
        field.setForeground(COLOR_TEXT_PRIMARY);
        field.setCaretColor(COLOR_BORDER_FOCUS);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    public static void styleDarkComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setBackground(COLOR_BG_INPUT);
        combo.setForeground(COLOR_TEXT_PRIMARY);
        combo.setFocusable(false);
        combo.setOpaque(true);
        combo.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(COLOR_BG_ROW_ALT);
                        g2.fillRect(0, 0, getWidth(), getHeight());

                        g2.setColor(COLOR_BORDER_FOCUS);
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        int[] xPoints = {cx - 4, cx + 4, cx};
                        int[] yPoints = {cy - 2, cy - 2, cy + 3};
                        g2.fillPolygon(xPoints, yPoints, 3);
                        g2.dispose();
                    }
                };
                btn.setContentAreaFilled(false);
                btn.setOpaque(false);
                btn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                btn.setFocusPainted(false);
                return btn;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(COLOR_BG_INPUT);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setOpaque(true);
                l.setFont(new Font("SansSerif", Font.PLAIN, 12));
                if (isSelected) {
                    l.setBackground(new Color(2, 132, 199));
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(COLOR_BG_INPUT);
                    l.setForeground(COLOR_TEXT_PRIMARY);
                }
                l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return l;
            }
        });
    }

    public static void styleDarkTable(JTable table) {
        table.setRowHeight(38);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setBackground(COLOR_BG_INPUT);
        table.setForeground(COLOR_TEXT_PRIMARY);
        table.setSelectionBackground(new Color(2, 132, 199));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(COLOR_BORDER);
        table.setShowGrid(true);

        // 表头自绘
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = new JLabel(value != null ? value.toString() : "");
                l.setOpaque(true);
                l.setBackground(COLOR_BG_ROW_ALT);
                l.setForeground(COLOR_BORDER_FOCUS);
                l.setFont(new Font("SansSerif", Font.BOLD, 13));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 1, COLOR_BORDER),
                        BorderFactory.createEmptyBorder(8, 4, 8, 4)
                ));
                return l;
            }
        });

        // 单元格斑马交替深色渲染
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                if (isSelected) {
                    l.setBackground(new Color(2, 132, 199));
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground((row % 2 == 0) ? COLOR_BG_ROW_ALT : COLOR_BG_INPUT);
                    l.setForeground(COLOR_TEXT_PRIMARY);
                }
                l.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                return l;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }
}
