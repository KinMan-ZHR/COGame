package person.kinman.cogame.client.ui.page;

import person.kinman.cogame.client.ui.GameLauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 本地对战面板 - 包含本地游戏场景和逻辑
 */
public class LocalGamePanel extends JPanel {
    private GameLauncher mainFrame;
    private Timer gameTimer; // 游戏循环计时器
    private int gameTime = 0; // 游戏运行时间（秒）

    public LocalGamePanel(GameLauncher mainFrame) {
        this.mainFrame = mainFrame;
        initUI();
        initGameLogic();
    }

    private void initUI() {
        setBackground(new Color(20, 40, 60));
        setLayout(new BorderLayout());

        // 标题
        JLabel titleLabel = new JLabel("本地对战");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // 游戏区域（中央）
        // 实际开发中可替换为自定义游戏画布
        add(createGameArea(), BorderLayout.CENTER);

        // 返回按钮
        JButton backBtn = createButton("返回主菜单", new Color(178, 34, 34));
        backBtn.addActionListener(e -> {
            stopGame(); // 停止游戏循环
            mainFrame.showPanel(GameLauncher.START_PANEL);
        });
        
        JPanel backPanel = new JPanel();
        backPanel.setBackground(new Color(20, 40, 60));
        backPanel.add(backBtn);
        add(backPanel, BorderLayout.SOUTH);
    }

    /**
     * 创建游戏区域面板
     */
    private JPanel createGameArea() {
        JPanel gameArea = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 绘制游戏元素（示例：显示游戏时间）
                g.setColor(Color.WHITE);
                g.setFont(new Font("微软雅黑", Font.PLAIN, 24));
                g.drawString("游戏进行中...", 320, 250);
                g.drawString("时间: " + gameTime + "秒", 340, 300);
            }
        };
        gameArea.setBackground(new Color(20, 40, 60));
        return gameArea;
    }

    /**
     * 初始化游戏逻辑
     */
    private void initGameLogic() {
        // 游戏循环：每1000ms更新一次
        gameTimer = new Timer(1000, (ActionEvent e) -> {
            gameTime++;
            repaint(); // 重绘游戏区域
        });
    }

    /**
     * 开始游戏
     */
    public void startGame() {
        gameTime = 0;
        gameTimer.start();
    }

    /**
     * 停止游戏
     */
    public void stopGame() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
    }

    /**
     * 创建按钮
     */
    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        button.setPreferredSize(new Dimension(160, 50));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        // 显示时自动开始游戏，隐藏时停止
        if (visible) {
            startGame();
        } else {
            stopGame();
        }
    }
}
