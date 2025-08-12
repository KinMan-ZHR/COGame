package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.ui.page.BattlePanel;
import person.kinman.cogame.client.ui.page.LocalGamePanel;
import person.kinman.cogame.client.ui.page.OnlineGamePanel;
import person.kinman.cogame.client.ui.page.StartPanel;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 游戏主窗口，负责管理所有场景面板的切换
 */
public class GameLauncher extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private CardLayout cardLayout;
    private JPanel containerPanel;

    // 场景标识常量
    public static final String START_PANEL = "startPanel";
    public static final String LOCAL_GAME_PANEL = "localGamePanel";
    public static final String ONLINE_GAME_PANEL = "onlineGamePanel";
    public static final String BATTLE_PANEL = "battlePanel";

    public GameLauncher() {
        setTitle("游戏战场");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // 初始化布局和容器
        initLayout();

        // 初始化并添加所有场景面板
        addPanels();

        // ESC键退出功能
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    int choice = JOptionPane.showConfirmDialog(
                            GameLauncher.this,
                            "确定要退出游戏吗？",
                            "退出确认",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                }
            }
        });

        // 默认显示启动面板
        showPanel(START_PANEL);
        setVisible(true);
    }

    private void initLayout() {
        cardLayout = new CardLayout();
        containerPanel = new JPanel(cardLayout);
        containerPanel.setBackground(new Color(30, 30, 50));
        add(containerPanel);
    }

    private void addPanels() {
        // 创建各场景面板实例，传入主窗口引用用于切换场景
        containerPanel.add(new StartPanel(this), START_PANEL);
        containerPanel.add(new LocalGamePanel(this), LOCAL_GAME_PANEL);
        containerPanel.add(new OnlineGamePanel(this), ONLINE_GAME_PANEL);
        containerPanel.add(new BattlePanel(), BATTLE_PANEL);
    }

    /**
     * 切换显示指定场景
     */
    public void showPanel(String panelName) {
        cardLayout.show(containerPanel, panelName);
        this.requestFocusInWindow(); // 确保键盘事件生效
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameLauncher::new);
    }
}
