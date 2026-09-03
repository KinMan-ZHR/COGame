package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.audio.AudioPlayer;
import person.kinman.cogame.client.controller.GameController;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEvaluator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

/**
 * 游戏核心画板：保留原汁原味的原生 Swing 视觉与布局
 */
public class GameCanvas extends JPanel {
    public static final int ScreenWIDTH = 840;
    public static final int ScreenHEIGHT = 640;
    public static final int GameScreenWIDTH = 580;

    public static final int GRID_START_X = 60;
    public static final int GRID_START_Y = 60;
    public static final int CELL_SIZE = 80;

    private final GameController controller;
    private Image player1Img;
    private Image player2Img;
    private boolean showPath = false;
    private String statusNotification = "";

    public GameCanvas(GameController controller) {
        this.controller = controller;
        this.setPreferredSize(new Dimension(ScreenWIDTH, ScreenHEIGHT));
        this.setBackground(Color.DARK_GRAY);
        loadImages();

        controller.setOnStateChanged(state -> {
            updateMusic(state);
            repaint();
        });

        controller.setOnNotification(msg -> {
            this.statusNotification = msg;
            repaint();
        });

        updateMusic(controller.getGameState());
    }

    private void loadImages() {
        player1Img = loadImage("player1.jpg");
        player2Img = loadImage("player2.jpg");
    }

    private Image loadImage(String name) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(name)) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch (Exception ignored) {}
        // 占位图片
        BufferedImage fallback = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = fallback.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        return fallback;
    }

    private void updateMusic(GameState state) {
        if (state.isOver()) {
            AudioPlayer.playMusic("Brand X Music - Get Bent.wav");
        } else {
            if (state.getCurrentTurn() == 1) {
                AudioPlayer.playMusic("Varien-Future Funk.wav");
            } else {
                AudioPlayer.playMusic("imagine dragonslil wayne - believer.wav");
            }
        }
    }

    public void toggleShowPath() {
        this.showPath = !this.showPath;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GameState state = controller.getGameState();
        Board board = state.getBoard();

        // 1. 棋盘区域底色
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(0, 0, GameScreenWIDTH, ScreenHEIGHT);

        // 2. 右侧信息栏底色
        g2.setColor(new Color(245, 235, 240));
        g2.fillRect(GameScreenWIDTH, 0, ScreenWIDTH - GameScreenWIDTH, ScreenHEIGHT);

        // 3. 计算路径（若开启）
        List<int[]> path = null;
        if (showPath) {
            path = GameEvaluator.findPath(board,
                    state.getP1().getR(), state.getP1().getC(),
                    state.getP2().getR(), state.getP2().getC());
        }

        // 4. 绘制每个格子底色与编号
        int rows = board.getRows();
        int cols = board.getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = GRID_START_X + c * CELL_SIZE;
                int y = GRID_START_Y + r * CELL_SIZE;

                // 判断是否在最短路径上
                boolean inPath = false;
                if (path != null) {
                    for (int[] p : path) {
                        if (p[0] == r && p[1] == c) {
                            inPath = true;
                            break;
                        }
                    }
                }

                g2.setColor(inPath ? new Color(30, 100, 200) : new Color(15, 15, 15));
                g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                // 格子编号
                g2.setColor(new Color(80, 80, 80));
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                int cellIndex = r * cols + 1 + c;
                g2.drawString(String.valueOf(cellIndex), x + 8, y + 24);
            }
        }

        // 5. 绘制所有边（绿色=畅通，红色粗线=已封锁）
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = GRID_START_X + c * CELL_SIZE;
                int y = GRID_START_Y + r * CELL_SIZE;

                // 上边
                drawBorderEdge(g2, x, y, x + CELL_SIZE, y, board.isConnected(r, c, Direction.UP), r == 0);
                // 下边
                drawBorderEdge(g2, x, y + CELL_SIZE, x + CELL_SIZE, y + CELL_SIZE, board.isConnected(r, c, Direction.DOWN), r == rows - 1);
                // 左边
                drawBorderEdge(g2, x, y, x, y + CELL_SIZE, board.isConnected(r, c, Direction.LEFT), c == 0);
                // 右边
                drawBorderEdge(g2, x + CELL_SIZE, y, x + CELL_SIZE, y + CELL_SIZE, board.isConnected(r, c, Direction.RIGHT), c == cols - 1);
            }
        }

        // 6. 绘制玩家
        drawPlayer(g2, state.getP1(), player1Img, Color.CYAN);
        drawPlayer(g2, state.getP2(), player2Img, Color.ORANGE);

        // 7. 绘制右侧状态栏
        drawSidebar(g2, state);
    }

    private void drawBorderEdge(Graphics2D g2, int x1, int y1, int x2, int y2, boolean open, boolean isBoundary) {
        if (isBoundary) {
            g2.setColor(new Color(180, 50, 50));
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(x1, y1, x2, y2);
        } else {
            if (open) {
                g2.setColor(new Color(46, 204, 113));
                g2.setStroke(new BasicStroke(2));
            } else {
                g2.setColor(new Color(231, 76, 60));
                g2.setStroke(new BasicStroke(4));
            }
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawPlayer(Graphics2D g2, PlayerState player, Image img, Color indicatorColor) {
        int px = GRID_START_X + player.getC() * CELL_SIZE;
        int py = GRID_START_Y + player.getR() * CELL_SIZE;

        // 居中立绘
        int imgSize = CELL_SIZE - 12;
        if (img != null) {
            g2.drawImage(img, px + 6, py + 6, imgSize, imgSize, null);
        }

        // 朝向指示点
        g2.setColor(indicatorColor);
        int dotSize = 10;
        int dotX = px + CELL_SIZE / 2 - dotSize / 2;
        int dotY = py + CELL_SIZE / 2 - dotSize / 2;

        switch (player.getDirection()) {
            case UP -> dotY = py + 4;
            case DOWN -> dotY = py + CELL_SIZE - dotSize - 4;
            case LEFT -> dotX = px + 4;
            case RIGHT -> dotX = px + CELL_SIZE - dotSize - 4;
        }
        g2.fillOval(dotX, dotY, dotSize, dotSize);
        g2.setColor(Color.WHITE);
        g2.drawOval(dotX, dotY, dotSize, dotSize);
    }

    private void drawSidebar(Graphics2D g2, GameState state) {
        int startX = GameScreenWIDTH + 15;
        int y = 30;

        // 模式标签
        g2.setColor(new Color(52, 73, 94));
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.drawString("【" + controller.getModeName() + "】", startX, y);
        y += 25;

        // 当前操作者头像与信息
        PlayerState actor = state.getCurrentPlayer();
        Image actorImg = (actor.getId() == 1) ? player1Img : player2Img;

        if (actorImg != null) {
            g2.drawImage(actorImg, startX, y, 90, 110, null);
        }

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString(actor.getName(), startX + 105, y + 30);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.drawString("回合: " + (actor.getId() == 1 ? "先手 (P1)" : "后手 (P2)"), startX + 105, y + 60);
        g2.drawString("朝向: " + actor.getDirection().getName(), startX + 105, y + 85);
        y += 125;

        // 操作指南
        g2.setColor(new Color(80, 80, 80));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString("—— 操作指南 ——", startX, y);
        y += 20;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString("• WASD / 方向键 : 移动并改变朝向", startX, y); y += 18;
        g2.drawString("• 空格 (SPACE)  : 沿当前朝向前进", startX, y); y += 18;
        g2.drawString("• R 键           : 顺时针旋转90°", startX, y); y += 18;
        g2.drawString("• L 键           : 封锁当前朝向边 (切回合)", startX, y); y += 18;
        g2.drawString("• P 键           : 开启/关闭连通路径", startX, y); y += 18;
        g2.drawString("• + 键           : 重置当前棋局", startX, y); y += 28;

        // 终局结果
        if (state.isOver()) {
            g2.setColor(new Color(192, 57, 43));
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("★ 游戏结束！", startX, y); y += 22;

            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            String winnerTitle = (state.getWinner() == 3) ? "平局" : (state.getPlayer(state.getWinner()).getName() + " 胜利！");
            g2.drawString(winnerTitle, startX, y); y += 22;

            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2.drawString("• " + state.getP1().getName() + ": " + state.getP1Territory() + " 格 / " + state.getP1UnblockedEdges() + " 边", startX, y); y += 20;
            g2.drawString("• " + state.getP2().getName() + ": " + state.getP2Territory() + " 格 / " + state.getP2UnblockedEdges() + " 边", startX, y); y += 25;
        }

        // 状态通知
        if (!statusNotification.isEmpty()) {
            g2.setColor(new Color(41, 128, 185));
            g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g2.drawString("ℹ " + statusNotification, startX, ScreenHEIGHT - 20);
        }
    }
}
