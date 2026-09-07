package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.audio.AudioPlayer;
import person.kinman.cogame.client.controller.GameController;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 游戏主窗口：支持自适应窗口缩放、F11全屏切换与局内分先战术顶栏
 */
public class GameFrame extends JFrame {
    private final GameController controller;
    private final GameCanvas canvas;
    private boolean isFullScreen = false;

    private JLabel modeLabel;
    private JLabel roleBadge;
    private JLabel statusLabel;
    private boolean wasGameOver = false;

    public GameFrame(GameController controller) {
        this.controller = controller;
        this.canvas = new GameCanvas(controller);

        this.setTitle("COGame - 《端脑》隔断棋盘博弈 (" + controller.getModeName() + ")");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(true);
        this.setMinimumSize(new Dimension(880, 640));
        this.setPreferredSize(new Dimension(1120, 800));

        this.setLayout(new BorderLayout());
        this.add(createTopBar(), BorderLayout.NORTH);
        this.add(canvas, BorderLayout.CENTER);
        this.pack();
        this.setLocationRelativeTo(null);

        setupStateListeners();
        setupKeyListeners();
        updateTopBar(controller.getGameState());

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                AudioPlayer.stopMusic();
                controller.close();
            }
        });
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(new Color(15, 23, 42)); // 深邃墨蓝黑曜石
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 41, 59)),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        // 1. 左侧：模式名称与身份先手徽章
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        modeLabel = new JLabel("⚔️ " + controller.getModeName());
        modeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        modeLabel.setForeground(new Color(226, 232, 240));

        roleBadge = new JLabel();
        roleBadge.setFont(new Font("SansSerif", Font.BOLD, 12));

        leftPanel.add(modeLabel);
        leftPanel.add(roleBadge);
        bar.add(leftPanel, BorderLayout.WEST);

        // 2. 中间：实时状态与操作指令
        statusLabel = new JLabel("💡 鼠标左键点击移动 · 右键定向锁边 | 或使用 WASD + L 键", JLabel.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(148, 163, 184));
        bar.add(statusLabel, BorderLayout.CENTER);

        // 3. 右侧：战术功能按钮 (均 setFocusable(false) 避免劫持键盘焦点)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        DarkThemeHelper.DarkButton btnNewGame = new DarkThemeHelper.DarkButton(
                "⚔️ 新一局 (分先)",
                new Color(2, 132, 199), new Color(14, 165, 233), new Color(56, 189, 248)
        );
        btnNewGame.setFocusable(false);
        btnNewGame.setPreferredSize(new Dimension(125, 30));
        btnNewGame.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnNewGame.setToolTipText("开启新一局并重新选择先后手");
        btnNewGame.addActionListener(e -> openTurnOrderDialog(false));

        DarkThemeHelper.DarkButton btnSwap = new DarkThemeHelper.DarkButton(
                "🔄 换先对决",
                new Color(109, 40, 217), new Color(124, 58, 237), new Color(192, 132, 252)
        );
        btnSwap.setFocusable(false);
        btnSwap.setPreferredSize(new Dimension(95, 30));
        btnSwap.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSwap.setToolTipText("一键互换先后手席位开启新局");
        btnSwap.addActionListener(e -> controller.swapTurnOrder());

        DarkThemeHelper.DarkButton btnPath = new DarkThemeHelper.DarkButton(
                "💡 寻路 (P)",
                new Color(30, 41, 59), new Color(51, 65, 85), DarkThemeHelper.COLOR_BORDER
        );
        btnPath.setFocusable(false);
        btnPath.setPreferredSize(new Dimension(80, 30));
        btnPath.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnPath.addActionListener(e -> canvas.toggleShowPath());

        DarkThemeHelper.DarkButton btnFull = new DarkThemeHelper.DarkButton(
                "⛶ 全屏",
                new Color(30, 41, 59), new Color(51, 65, 85), DarkThemeHelper.COLOR_BORDER
        );
        btnFull.setFocusable(false);
        btnFull.setPreferredSize(new Dimension(65, 30));
        btnFull.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnFull.addActionListener(e -> toggleFullScreen());

        rightPanel.add(btnNewGame);
        rightPanel.add(btnSwap);
        rightPanel.add(btnPath);
        rightPanel.add(btnFull);
        bar.add(rightPanel, BorderLayout.EAST);

        return bar;
    }

    private void setupStateListeners() {
        controller.setOnStateChanged(state -> {
            SwingUtilities.invokeLater(() -> {
                updateTopBar(state);
                if (state.isOver()) {
                    if (!wasGameOver) {
                        wasGameOver = true;
                        // 延迟 600ms 弹出新一局分先框，让玩家先看清最终比分盘面
                        Timer timer = new Timer(650, evt -> {
                            if (isVisible() && controller.getGameState().isOver()) {
                                openTurnOrderDialog(true);
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
                    }
                } else {
                    wasGameOver = false;
                }
            });
        });

        controller.setOnNotification(msg -> {
            SwingUtilities.invokeLater(() -> {
                if (statusLabel != null && msg != null && !msg.isEmpty()) {
                    statusLabel.setText(msg);
                    statusLabel.setForeground(new Color(56, 189, 248));
                    Timer t = new Timer(6000, evt -> {
                        if (statusLabel.getText().equals(msg)) {
                            statusLabel.setText("💡 鼠标左键点击移动 · 右键定向锁边 | 或使用 WASD + L 键");
                            statusLabel.setForeground(new Color(148, 163, 184));
                        }
                    });
                    t.setRepeats(false);
                    t.start();
                }
            });
        });
    }

    private void updateTopBar(GameState state) {
        if (modeLabel != null) {
            modeLabel.setText("⚔️ " + controller.getModeName());
        }
        setTitle("COGame - 《端脑》隔断棋盘博弈 (" + controller.getModeName() + ")");

        if (roleBadge != null) {
            int myId = controller.getMyPlayerId();
            if (myId == 1) {
                roleBadge.setText("🔵 您执先手 (P1)");
                roleBadge.setForeground(new Color(6, 182, 212));
                roleBadge.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(6, 182, 212), 1),
                        BorderFactory.createEmptyBorder(2, 8, 2, 8)
                ));
            } else if (myId == 2) {
                roleBadge.setText("🔴 您执后手 (P2)");
                roleBadge.setForeground(new Color(245, 158, 11));
                roleBadge.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(245, 158, 11), 1),
                        BorderFactory.createEmptyBorder(2, 8, 2, 8)
                ));
            } else {
                roleBadge.setText("👥 本地双人");
                roleBadge.setForeground(new Color(148, 163, 184));
                roleBadge.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                        BorderFactory.createEmptyBorder(2, 8, 2, 8)
                ));
            }
        }
    }

    public void openTurnOrderDialog(boolean isGameOver) {
        InGameTurnDialog dialog = new InGameTurnDialog(this, controller, isGameOver);
        dialog.setVisible(true);
        requestFocusInWindow();
    }

    private void setupKeyListeners() {
        KeyAdapter keyAdapter = new KeyAdapter() {
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
                            openTurnOrderDialog(false);
                    case KeyEvent.VK_F11 ->
                            toggleFullScreen();
                }
            }
        };
        this.addKeyListener(keyAdapter);
        this.canvas.addKeyListener(keyAdapter);
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
            setSize(1120, 800);
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
