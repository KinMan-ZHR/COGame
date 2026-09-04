package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.audio.AudioPlayer;
import person.kinman.cogame.client.controller.GameController;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Direction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 游戏主窗口：支持自适应窗口缩放与 F11 全屏切换
 */
public class GameFrame extends JFrame {
    private final GameController controller;
    private final GameCanvas canvas;
    private boolean isFullScreen = false;

    public GameFrame(GameController controller) {
        this.controller = controller;
        this.canvas = new GameCanvas(controller);

        this.setTitle("COGame - 《端脑》隔断棋盘博弈 (" + controller.getModeName() + ")");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(true);
        this.setMinimumSize(new Dimension(880, 620));
        this.setPreferredSize(new Dimension(1100, 780));

        this.add(canvas, BorderLayout.CENTER);
        this.pack();
        this.setLocationRelativeTo(null);

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W, KeyEvent.VK_UP ->
                            controller.handleUserAction(GameAction.changeDirMove(Direction.UP));
                    case KeyEvent.VK_S, KeyEvent.VK_DOWN ->
                            controller.handleUserAction(GameAction.changeDirMove(Direction.DOWN));
                    case KeyEvent.VK_A, KeyEvent.VK_LEFT ->
                            controller.handleUserAction(GameAction.changeDirMove(Direction.LEFT));
                    case KeyEvent.VK_D, KeyEvent.VK_RIGHT ->
                            controller.handleUserAction(GameAction.changeDirMove(Direction.RIGHT));
                    case KeyEvent.VK_SPACE ->
                            controller.handleUserAction(GameAction.move());
                    case KeyEvent.VK_R ->
                            controller.handleUserAction(GameAction.rotate());
                    case KeyEvent.VK_L ->
                            controller.handleUserAction(GameAction.lock());
                    case KeyEvent.VK_P ->
                            canvas.toggleShowPath();
                    case KeyEvent.VK_ADD, KeyEvent.VK_EQUALS ->
                            controller.resetGame();
                    case KeyEvent.VK_F11 ->
                            toggleFullScreen();
                }
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                AudioPlayer.stopMusic();
                controller.close();
            }
        });
    }

    /**
     * 切换自适应全屏模式
     */
    public void toggleFullScreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        dispose();
        if (!isFullScreen) {
            setUndecorated(true);
            if (gd.isFullScreenSupported()) {
                gd.setFullScreenWindow(this);
            } else {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            isFullScreen = true;
        } else {
            if (gd.isFullScreenSupported()) {
                gd.setFullScreenWindow(null);
            }
            setUndecorated(false);
            setExtendedState(JFrame.NORMAL);
            setSize(1100, 780);
            setLocationRelativeTo(null);
            isFullScreen = false;
        }
        setVisible(true);
        requestFocusInWindow();
    }

    public void display() {
        this.setVisible(true);
        this.requestFocusInWindow();
        this.controller.start();
    }
}
