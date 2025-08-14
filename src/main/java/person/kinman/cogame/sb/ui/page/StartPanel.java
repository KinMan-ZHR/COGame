package person.kinman.cogame.sb.ui.page;

import person.kinman.cogame.sb.contract.PanelId;
import person.kinman.cogame.sb.event.OnlineGameEvent;
import person.kinman.cogame.sb.event.LocalGameEvent;
import person.kinman.cogame.sb.eventBus.GlobalEventBus;
import person.kinman.cogame.sb.ui.BasePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 启动面板 - 欢迎页面，包含模式选择按钮
 */
public class StartPanel extends BasePanel {

    private JButton localBtn;
    private JButton onlineBtn;

    public StartPanel() {
    }


    @Override
    protected void initListeners() {
        localBtn.addActionListener(e -> {
            // 触发开始游戏事件
            GlobalEventBus.getUiBus().post(new LocalGameEvent());
        });

        onlineBtn.addActionListener(e -> {
            // 触发退出游戏事件
            GlobalEventBus.getUiBus().post(new OnlineGameEvent());
        });
    }

    protected void initUI() {
        setBackground(new Color(30, 30, 50));
        setLayout(new BorderLayout());

        // 标题区域
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(30, 30, 50));
        JLabel titleLabel = new JLabel("welcome");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 48));
        titleLabel.setForeground(new Color(180, 160, 255));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // 按钮区域
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(40, 40, 60));
        buttonPanel.setLayout(new GridLayout(1, 2, 60, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        // 本地对战按钮
        localBtn = createButton("本地对战", new Color(70, 130, 180));

        // 联网对战按钮
        onlineBtn = createButton("联网对战", new Color(100, 149, 237));

        buttonPanel.add(localBtn);
        buttonPanel.add(onlineBtn);
        add(buttonPanel, BorderLayout.CENTER);

        // 底部信息
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(new Color(30, 30, 50));
        JLabel footerLabel = new JLabel("版本 1.0.0 | 按ESC退出");
        footerLabel.setForeground(Color.LIGHT_GRAY);
        footerPanel.add(footerLabel);
        add(footerPanel, BorderLayout.SOUTH);
    }

    /**
     * 创建统一风格的按钮
     */
    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        button.setPreferredSize(new Dimension(180, 60));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        
        // 按钮悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBorder(BorderFactory.createLoweredBevelBorder());
                button.setBackground(color.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBorder(BorderFactory.createRaisedBevelBorder());
                button.setBackground(color);
            }
        });
        
        return button;
    }
}
