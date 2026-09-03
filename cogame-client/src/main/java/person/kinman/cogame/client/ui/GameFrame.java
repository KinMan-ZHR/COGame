package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.audio.AudioPlayer;
import person.kinman.cogame.client.controller.GameController;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Direction;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 游戏主窗口
 */
public class GameFrame extends JFrame {
    private final GameController controller;
    private final GameCanvas canvas;

    public GameFrame(GameController controller) {
        this.controller = controller;
        this.canvas = new GameCanvas(controller);

        this.setTitle("COGame - 《端脑》隔断棋盘博弈 (" + controller.getModeName() + ")");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.add(canvas);
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

    public void display() {
        this.setVisible(true);
        this.requestFocusInWindow();
    }
}
