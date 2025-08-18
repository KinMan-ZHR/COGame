package person.kinman.cogame.client.ui.button;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;

/**
 * JButton工厂类，用于创建各种样式和功能的JButton
 */
public class JButtonFactory {

    // 默认按钮属性
    private static final Font DEFAULT_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Color DEFAULT_FOREGROUND = Color.BLACK;
    private static final Color DEFAULT_BACKGROUND = new Color(240, 240, 240);
    private static final int DEFAULT_PREFERRED_WIDTH = 100;
    private static final int DEFAULT_PREFERRED_HEIGHT = 28;

    /**
     * 创建自定义返回按钮（矢量图标）
     */
    public static JButton createBackButton() {
        // 自定义按钮，使用矢量图形绘制返回图标
        JButton backBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();

                // 启用抗锯齿，使图形更平滑
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制背景（鼠标悬停时显示）
                if (getModel().isRollover()) {
                    g2.setColor(new Color(60, 40, 80, 150));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }

                // 绘制返回箭头（矢量图形）
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                GeneralPath arrow = new GeneralPath();
                arrow.moveTo(getWidth() * 0.7f, getHeight() * 0.25f);
                arrow.lineTo(getWidth() * 0.3f, getHeight() * 0.5f);
                arrow.lineTo(getWidth() * 0.7f, getHeight() * 0.75f);
                arrow.lineTo(getWidth() * 0.6f, getHeight() * 0.5f);
                arrow.lineTo(getWidth() * 0.7f, getHeight() * 0.25f);
                arrow.closePath();

                // 填充箭头
                g2.fill(arrow);

                // 绘制箭头左侧的竖线
                g2.drawLine(
                        (int) (getWidth() * 0.3f),
                        (int) (getHeight() * 0.25f),
                        (int) (getWidth() * 0.3f),
                        (int) (getHeight() * 0.75f)
                );

                g2.dispose();
            }
        };

        // 设置按钮属性
        backBtn.setPreferredSize(new Dimension(40, 40));
        backBtn.setContentAreaFilled(false); // 透明背景
        backBtn.setBorderPainted(false);     // 无边框
        backBtn.setFocusPainted(false);      // 无焦点框
        backBtn.setToolTipText("返回主菜单"); // 鼠标悬停提示
        backBtn.setRolloverEnabled(true);    // 启用悬停效果

        return backBtn;
    }

    /**
     * 创建默认样式的按钮
     * @param text 按钮文本
     * @return 配置好的JButton
     */
    public static JButton createDefaultButton(String text) {
        JButton button = new JButton(text);
        setupDefaultProperties(button);
        return button;
    }

    /**
     * 创建带有点击事件的按钮
     * @param text 按钮文本
     * @param listener 点击事件监听器
     * @return 配置好的JButton
     */
    public static JButton createButtonWithAction(String text, ActionListener listener) {
        JButton button = createDefaultButton(text);
        button.addActionListener(listener);
        return button;
    }

    /**
     * 创建带有图标的按钮
     * @param icon 按钮图标
     * @return 配置好的JButton
     */
    public static JButton createIconButton(Icon icon) {
        JButton button = new JButton(icon);
        setupDefaultProperties(button);
        return button;
    }

    /**
     * 创建带有文本和图标的按钮
     * @param text 按钮文本
     * @param icon 按钮图标
     * @param horizontalTextPosition 文本相对于图标的水平位置
     * @return 配置好的JButton
     */
    public static JButton createTextAndIconButton(String text, Icon icon, int horizontalTextPosition) {
        JButton button = new JButton(text, icon);
        button.setHorizontalTextPosition(horizontalTextPosition);
        setupDefaultProperties(button);
        return button;
    }

    /**
     * 创建具有自定义颜色的按钮
     * @param text 按钮文本
     * @param foreground 前景色(文本颜色)
     * @param background 背景色
     * @return 配置好的JButton
     */
    public static JButton createColoredButton(String text, Color foreground, Color background) {
        JButton button = createDefaultButton(text);
        button.setForeground(foreground);
        button.setBackground(background);
        // 确保按钮能够显示背景色
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    /**
     * 创建具有自定义字体的按钮
     * @param text 按钮文本
     * @param font 字体
     * @return 配置好的JButton
     */
    public static JButton createButtonWithFont(String text, Font font) {
        JButton button = createDefaultButton(text);
        button.setFont(font);
        return button;
    }

    /**
     * 创建禁用状态的按钮
     * @param text 按钮文本
     * @return 配置好的JButton
     */
    public static JButton createDisabledButton(String text) {
        JButton button = createDefaultButton(text);
        button.setEnabled(false);
        return button;
    }

    /**
     * 创建具有自定义边框的按钮
     * @param text 按钮文本
     * @param border 边框
     * @return 配置好的JButton
     */
    public static JButton createButtonWithBorder(String text, Border border) {
        JButton button = createDefaultButton(text);
        button.setBorder(border);
        return button;
    }

    /**
     * 创建具有快捷键的按钮
     * @param text 按钮文本
     * @param mnemonic 快捷键(使用KeyEvent.VK_*常量)
     * @return 配置好的JButton
     */
    public static JButton createButtonWithMnemonic(String text, int mnemonic) {
        JButton button = createDefaultButton(text);
        button.setMnemonic(mnemonic);
        return button;
    }

    /**
     * 设置按钮的默认属性
     * @param button 要配置的按钮
     */
    private static void setupDefaultProperties(JButton button) {
        button.setFont(DEFAULT_FONT);
        button.setForeground(DEFAULT_FOREGROUND);
        button.setBackground(DEFAULT_BACKGROUND);
        button.setPreferredSize(new Dimension(DEFAULT_PREFERRED_WIDTH, DEFAULT_PREFERRED_HEIGHT));
        button.setFocusPainted(false); // 去除焦点边框
    }
}
    