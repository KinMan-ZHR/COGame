package person.kinman.cogame.client.ui;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 游戏主窗口，负责管理所有场景面板的切换
 */
public class GameFrame extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;


    public GameFrame() {
        setTitle("COG");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // ESC键退出功能
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    int choice = JOptionPane.showConfirmDialog(
                            GameFrame.this,
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
    }
}
