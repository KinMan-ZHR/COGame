package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.audio.AudioPlayer;
import person.kinman.cogame.client.controller.GameController;
import person.kinman.cogame.client.controller.OnlineController;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEvaluator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * 游戏核心画板：支持 6x6~13x13 动态规格、自适应窗口/全屏缩放与高对比度现代暗色视觉
 * v2.6 全新支持鼠标交互：左键点击目标格移动、右键象限判定朝向锁边与实时悬停预览指引
 */
public class GameCanvas extends JPanel {
    private final GameController controller;
    private Image player1Img;
    private Image player2Img;
    private boolean showPath = false;
    private String statusNotification = "";

    // 鼠标交互与悬停预览状态
    private int hoverR = -1;
    private int hoverC = -1;
    private Direction hoverDir = null;
    private int lastStartX = 0;
    private int lastStartY = 0;
    private int lastCellSize = 0;
    private int lastRows = 0;
    private int lastCols = 0;

    public GameCanvas(GameController controller) {
        this.controller = controller;
        this.setBackground(new Color(11, 17, 32)); // 深邃墨蓝底色
        this.setFocusable(true);
        loadImages();
        setupMouseListeners();

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
        BufferedImage fallback = new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = fallback.createGraphics();
        g.setColor(new Color(30, 41, 59));
        g.fillRect(0, 0, 120, 120);
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
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int totalWidth = getWidth();
        int totalHeight = getHeight();

        GameState state = controller.getGameState();
        Board board = state.getBoard();
        int rows = board.getRows();
        int cols = board.getCols();

        // 1. 布局划分：左侧为自适应棋盘区域，右侧为固定/响应式信息栏
        int sidebarWidth = Math.max(300, Math.min(360, (int) (totalWidth * 0.30)));
        int boardAreaWidth = totalWidth - sidebarWidth;
        int boardAreaHeight = totalHeight;

        // 2. 自适应计算每个单元格的像素大小
        int padding = 36;
        int maxGridWidth = boardAreaWidth - padding * 2;
        int maxGridHeight = boardAreaHeight - padding * 2;
        int cellSize = Math.min(maxGridWidth / cols, maxGridHeight / rows);
        cellSize = Math.max(32, cellSize); // 最小保证32px以保证可读

        int gridPixelWidth = cols * cellSize;
        int gridPixelHeight = rows * cellSize;
        int startX = (boardAreaWidth - gridPixelWidth) / 2;
        int startY = (boardAreaHeight - gridPixelHeight) / 2;

        this.lastStartX = startX;
        this.lastStartY = startY;
        this.lastCellSize = cellSize;
        this.lastRows = rows;
        this.lastCols = cols;

        // 3. 绘制棋盘大底板
        g2.setColor(new Color(15, 23, 42));
        g2.fill(new RoundRectangle2D.Float(startX - 12, startY - 12, gridPixelWidth + 24, gridPixelHeight + 24, 16, 16));
        g2.setColor(new Color(30, 41, 59));
        g2.setStroke(new BasicStroke(2));
        g2.draw(new RoundRectangle2D.Float(startX - 12, startY - 12, gridPixelWidth + 24, gridPixelHeight + 24, 16, 16));

        // 4. 计算路径高亮（若启用P键）
        List<int[]> path = null;
        if (showPath) {
            path = GameEvaluator.findPath(board,
                    state.getP1().getR(), state.getP1().getC(),
                    state.getP2().getR(), state.getP2().getC());
        }

        // 计算当前回合玩家在当前能量步数限制内的可达格子 (避开对手身位)
        Set<Long> reachableWithin3 = null;
        if (!state.isOver()) {
            PlayerState currP = state.getCurrentPlayer();
            PlayerState oppP = state.getOpponentPlayer();
            reachableWithin3 = GameEvaluator.getReachableWithinSteps(
                    board, state.getTurnStartR(), state.getTurnStartC(),
                    oppP.getR(), oppP.getC(), currP.getEnergy());
        }

        // 5. 绘制所有格子单元
        int fontSize = Math.max(10, (int) (cellSize * 0.28));
        Font cellFont = new Font("Consolas", Font.BOLD, fontSize);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cx = startX + c * cellSize;
                int cy = startY + r * cellSize;

                boolean inPath = false;
                if (path != null) {
                    for (int[] p : path) {
                        if (p[0] == r && p[1] == c) {
                            inPath = true;
                            break;
                        }
                    }
                }

                boolean isReachable = (reachableWithin3 != null && reachableWithin3.contains(GameEvaluator.encode(r, c)));

                // 格子填充色（现代暗黑科技风）
                if (inPath) {
                    g2.setColor(new Color(14, 116, 144)); // 连通路径高亮青蓝
                } else if (r == state.getP1().getR() && c == state.getP1().getC()) {
                    g2.setColor(new Color(8, 51, 68)); // P1 所在格微光
                } else if (r == state.getP2().getR() && c == state.getP2().getC()) {
                    g2.setColor(new Color(69, 26, 3)); // P2 所在格微光
                } else if (isReachable) {
                    g2.setColor(new Color(22, 42, 68)); // 当前回合可达范围柔和暗青
                } else {
                    g2.setColor(new Color(19, 28, 46)); // 默认深邃黑曜石格
                }
                g2.fillRect(cx, cy, cellSize, cellSize);

                // 格子内部微弱网格分隔线
                g2.setColor(new Color(36, 48, 71, 100));
                g2.setStroke(new BasicStroke(1));
                g2.drawRect(cx, cy, cellSize, cellSize);

                // 可达范围中心能量微光点
                if (isReachable && !(r == state.getP1().getR() && c == state.getP1().getC()) && !(r == state.getP2().getR() && c == state.getP2().getC())) {
                    g2.setColor(new Color(56, 189, 248, 90));
                    g2.fillOval(cx + cellSize / 2 - 2, cy + cellSize / 2 - 2, 5, 5);
                }

                // 绘制低饱和度精致格子编号 (微弱暗灰字，彻底降噪)
                g2.setColor(new Color(100, 116, 139, 85));
                g2.setFont(new Font("Consolas", Font.PLAIN, Math.max(9, (int) (cellSize * 0.22))));
                int cellIndex = r * cols + 1 + c;
                g2.drawString(String.valueOf(cellIndex), cx + 5, cy + fontSize + 2);
            }
        }

        // 6. 绘制所有边（核心博弈元素：绿色极细通路 vs P1电光青锁边 vs P2炽金琥珀锁边）
        float lockedEdgeWidth = Math.max(4.5f, cellSize * 0.09f);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cx = startX + c * cellSize;
                int cy = startY + r * cellSize;

                // 上边
                drawBorderEdge(g2, cx, cy, cx + cellSize, cy, board.isConnected(r, c, Direction.UP), r == 0, board.getEdgeLocker(r, c, Direction.UP), lockedEdgeWidth);
                // 下边
                drawBorderEdge(g2, cx, cy + cellSize, cx + cellSize, cy + cellSize, board.isConnected(r, c, Direction.DOWN), r == rows - 1, board.getEdgeLocker(r, c, Direction.DOWN), lockedEdgeWidth);
                // 左边
                drawBorderEdge(g2, cx, cy, cx, cy + cellSize, board.isConnected(r, c, Direction.LEFT), c == 0, board.getEdgeLocker(r, c, Direction.LEFT), lockedEdgeWidth);
                // 右边
                drawBorderEdge(g2, cx + cellSize, cy, cx + cellSize, cy + cellSize, board.isConnected(r, c, Direction.RIGHT), c == cols - 1, board.getEdgeLocker(r, c, Direction.RIGHT), lockedEdgeWidth);
            }
        }

        // 7. 绘制玩家（带高对比光圈与高光三角朝向箭头）
        drawPlayer(g2, state.getP1(), player1Img, new Color(6, 182, 212), "P1", startX, startY, cellSize, state.getCurrentTurn() == 1 && !state.isOver());
        drawPlayer(g2, state.getP2(), player2Img, new Color(245, 158, 11), "P2", startX, startY, cellSize, state.getCurrentTurn() == 2 && !state.isOver());

        // 7.5 绘制鼠标悬停交互指引 (目标格高亮与朝向锁边预览箭头)
        drawMouseHoverIndicator(g2, state, startX, startY, cellSize, rows, cols, reachableWithin3);

        // 8. 绘制现代化高对比度侧边栏
        drawSidebar(g2, state, boardAreaWidth, 0, sidebarWidth, totalHeight);

        // 9. 联机等待对手加入时的沉浸式提示蒙层
        if (controller instanceof OnlineController oc && !oc.isGameStarted()) {
            g2.setColor(new Color(11, 17, 32, 225));
            g2.fillRect(0, 0, boardAreaWidth, totalHeight);

            int panelW = Math.min(480, boardAreaWidth - 40);
            int panelH = 150;
            int panelX = (boardAreaWidth - panelW) / 2;
            int panelY = (totalHeight - panelH) / 2;

            g2.setColor(new Color(30, 41, 59));
            g2.fill(new RoundRectangle2D.Float(panelX, panelY, panelW, panelH, 16, 16));
            g2.setColor(new Color(56, 189, 248));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new RoundRectangle2D.Float(panelX, panelY, panelW, panelH, 16, 16));

            g2.setFont(new Font("SansSerif", Font.BOLD, 20));
            String waitTitle = "⏳ 正在等待对手加入房间 (1/2)...";
            int tw = g2.getFontMetrics().stringWidth(waitTitle);
            g2.drawString(waitTitle, panelX + (panelW - tw) / 2, panelY + 45);

            g2.setColor(new Color(226, 232, 240));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String rInfo = "房间编号: " + (oc.getRoomId() != null ? oc.getRoomId() : "---") + "   |   已就绪: 1/2";
            int rw = g2.getFontMetrics().stringWidth(rInfo);
            g2.drawString(rInfo, panelX + (panelW - rw) / 2, panelY + 85);

            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String hint = "请将房间号告知对手，对手加入后将自动开局！";
            int hw = g2.getFontMetrics().stringWidth(hint);
            g2.drawString(hint, panelX + (panelW - hw) / 2, panelY + 120);
        }

        g2.dispose();
    }

    private void drawBorderEdge(Graphics2D g2, int x1, int y1, int x2, int y2, boolean open, boolean isBoundary, int locker, float lockedWidth) {
        if (isBoundary) {
            g2.setColor(new Color(71, 85, 105)); // 外棋盘边界：深枪灰色
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
        } else {
            if (open) {
                // 畅通通道：柔和暗灰蓝纤细刻线，视觉深度下沉，让锁闭的激光壁障成为战场焦点
                g2.setColor(new Color(36, 48, 71, 140));
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, y1, x2, y2);
            } else {
                // 已封锁边：高饱和度激光能量力场壁障
                Color glowColor;
                Color coreColor;
                if (locker == 1) {
                    // P1 (先手) 锁边：电光亮青激光壁障
                    glowColor = new Color(6, 182, 212, 130);
                    coreColor = new Color(103, 232, 249);
                } else if (locker == 2) {
                    // P2 (后手/AI) 锁边：暖金琥珀激光壁障
                    glowColor = new Color(245, 158, 11, 130);
                    coreColor = new Color(253, 224, 71);
                } else if (locker == 3) {
                    // 中立预置迷宫墙：钛合金冷灰壁障
                    glowColor = new Color(100, 116, 139, 110);
                    coreColor = new Color(203, 213, 225);
                } else {
                    // 默认警告红
                    glowColor = new Color(239, 68, 68, 120);
                    coreColor = new Color(252, 165, 165);
                }

                // 1. 发光辉光外层
                g2.setColor(glowColor);
                g2.setStroke(new BasicStroke(lockedWidth + 4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, y1, x2, y2);

                // 2. 核心鲜亮实体线
                g2.setColor(coreColor);
                g2.setStroke(new BasicStroke(lockedWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void drawPlayer(Graphics2D g2, PlayerState player, Image img, Color accentColor, String tag, int startX, int startY, int cellSize, boolean isTurn) {
        int px = startX + player.getC() * cellSize;
        int py = startY + player.getR() * cellSize;

        int margin = Math.max(4, cellSize / 10);
        int avatarSize = cellSize - margin * 2;

        // 行动方在格内有柔和呼吸光晕底座
        if (isTurn) {
            g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 75));
            g2.fillOval(px + margin - 5, py + margin - 5, avatarSize + 10, avatarSize + 10);
        } else {
            g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 35));
            g2.fillOval(px + margin - 2, py + margin - 2, avatarSize + 4, avatarSize + 4);
        }

        // 头像绘制
        if (img != null) {
            Shape oldClip = g2.getClip();
            g2.setClip(new java.awt.geom.Ellipse2D.Float(px + margin, py + margin, avatarSize, avatarSize));
            g2.drawImage(img, px + margin, py + margin, avatarSize, avatarSize, null);
            g2.setClip(oldClip);
        }

        // 外围高对比轮廓圆环
        g2.setColor(accentColor);
        g2.setStroke(new BasicStroke(isTurn ? 3.0f : 2.0f));
        g2.drawOval(px + margin, py + margin, avatarSize, avatarSize);

        // 醒目的朝向指针（三角形箭头，指向边框方向）
        int arrowSize = Math.max(8, cellSize / 6);
        drawDirectionArrow(g2, px + cellSize / 2, py + cellSize / 2, player.getDirection(), cellSize / 2 - 2, arrowSize, accentColor);
    }

    private void drawDirectionArrow(Graphics2D g2, int centerX, int centerY, Direction dir, int radius, int size, Color color) {
        Path2D.Double arrow = new Path2D.Double();
        int tipX = centerX, tipY = centerY;
        int b1X = centerX, b1Y = centerY, b2X = centerX, b2Y = centerY;

        switch (dir) {
            case UP -> {
                tipX = centerX; tipY = centerY - radius;
                b1X = centerX - size; b1Y = tipY + size * 2;
                b2X = centerX + size; b2Y = tipY + size * 2;
            }
            case DOWN -> {
                tipX = centerX; tipY = centerY + radius;
                b1X = centerX - size; b1Y = tipY - size * 2;
                b2X = centerX + size; b2Y = tipY - size * 2;
            }
            case LEFT -> {
                tipX = centerX - radius; tipY = centerY;
                b1X = tipX + size * 2; b1Y = centerY - size;
                b2X = tipX + size * 2; b2Y = centerY + size;
            }
            case RIGHT -> {
                tipX = centerX + radius; tipY = centerY;
                b1X = tipX - size * 2; b1Y = centerY - size;
                b2X = tipX - size * 2; b2Y = centerY + size;
            }
        }

        arrow.moveTo(tipX, tipY);
        arrow.lineTo(b1X, b1Y);
        arrow.lineTo(b2X, b2Y);
        arrow.closePath();

        g2.setColor(color);
        g2.fill(arrow);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(arrow);
    }

    private void drawSidebar(Graphics2D g2, GameState state, int x, int y, int width, int height) {
        // 侧边栏背景
        g2.setColor(new Color(15, 23, 42)); // 深邃黑曜石
        g2.fillRect(x, y, width, height);
        g2.setColor(new Color(30, 41, 59));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x, y, x, y + height);

        int pad = 16;
        int innerWidth = width - pad * 2;
        int curY = y + 20;

        // 面板 1：双雄对决一体化 HUD 卡片 (Versus Card)
        curY = drawVersusHud(g2, state, x + pad, curY, innerWidth);

        // 面板 2：本回合战术行动 / 终局胜负卡片 (Tactical Action Card)
        curY = drawTacticalCard(g2, state, x + pad, curY + 12, innerWidth);

        // 面板 3：极简战局图例与快捷操作指南 (Compact Legend & Keybinds)
        drawCompactGuide(g2, x + pad, curY + 12, innerWidth);

        // 底部状态通知
        if (!statusNotification.isEmpty()) {
            g2.setColor(new Color(56, 189, 248));
            g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g2.drawString("ℹ " + statusNotification, x + pad, height - 16);
        }
    }

    private int drawVersusHud(Graphics2D g2, GameState state, int x, int y, int w) {
        int cardH = 196;
        g2.setColor(new Color(21, 32, 54));
        g2.fill(new RoundRectangle2D.Float(x, y, w, cardH, 14, 14));
        g2.setColor(new Color(51, 65, 85));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, cardH, 14, 14));

        // 头部标题条
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(new Color(148, 163, 184));
        String modeText = controller.getModeName() + " (" + state.getRows() + "×" + state.getCols() + ")";
        g2.drawString(modeText, x + 14, y + 22);

        String regenText = "⚡ 回合恢复: +" + state.getEnergyRegen();
        int rw = g2.getFontMetrics().stringWidth(regenText);
        g2.setColor(new Color(56, 189, 248));
        g2.drawString(regenText, x + w - rw - 14, y + 22);

        // 分隔线
        g2.setColor(new Color(30, 41, 59));
        g2.drawLine(x + 10, y + 32, x + w - 10, y + 32);

        int maxEnergy = state.getMaxEnergy();
        boolean p1Turn = (state.getCurrentTurn() == 1 && !state.isOver());
        boolean p2Turn = (state.getCurrentTurn() == 2 && !state.isOver());

        // P1 玩家席位 (上部)
        drawPlayerRow(g2, x + 12, y + 42, w - 24, state.getP1(), player1Img,
                new Color(6, 182, 212), p1Turn, maxEnergy, state.getCurrentTurnSteps());

        // 中间 VS 刻度徽章
        int vsY = y + 114;
        g2.setColor(new Color(30, 41, 59));
        g2.drawLine(x + 14, vsY, x + w - 14, vsY);
        g2.setColor(new Color(21, 32, 54));
        g2.fillRoundRect(x + w / 2 - 20, vsY - 10, 40, 20, 6, 6);
        g2.setColor(new Color(71, 85, 105));
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("VS", x + w / 2 - 7, vsY + 4);

        // P2 玩家席位 (下部)
        drawPlayerRow(g2, x + 12, y + 126, w - 24, state.getP2(), player2Img,
                new Color(245, 158, 11), p2Turn, maxEnergy, state.getCurrentTurnSteps());

        return y + cardH;
    }

    private void drawPlayerRow(Graphics2D g2, int rx, int ry, int rw, PlayerState player, Image avatar, Color accent, boolean isTurn, int maxEnergy, int currentSteps) {
        int avSize = 44;

        // 1. 头像区
        if (isTurn) {
            // 行动方呼吸高光底盘
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
            g2.fillOval(rx - 2, ry - 2, avSize + 4, avSize + 4);
        }

        if (avatar != null) {
            Shape oldClip = g2.getClip();
            g2.setClip(new java.awt.geom.Ellipse2D.Float(rx, ry, avSize, avSize));
            g2.drawImage(avatar, rx, ry, avSize, avSize, null);
            g2.setClip(oldClip);
        }
        g2.setColor(isTurn ? accent : new Color(71, 85, 105));
        g2.setStroke(new BasicStroke(isTurn ? 2.2f : 1.2f));
        g2.drawOval(rx, ry, avSize, avSize);

        // 2. 姓名与状态徽章
        int textX = rx + avSize + 12;
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(isTurn ? accent : new Color(241, 245, 249));
        g2.drawString(abbreviateName(player.getName(), 10), textX, ry + 18);

        // 状态徽章 (行动中 vs 等待)
        if (isTurn) {
            g2.setColor(accent);
            g2.fill(new RoundRectangle2D.Float(rx + rw - 60, ry + 3, 58, 18, 6, 6));
            g2.setColor(new Color(15, 23, 42));
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("行动中 ▶", rx + rw - 54, ry + 16);
        } else {
            g2.setColor(new Color(51, 65, 85));
            g2.fill(new RoundRectangle2D.Float(rx + rw - 48, ry + 3, 46, 18, 6, 6));
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString("等待", rx + rw - 36, ry + 16);
        }

        // 3. 动态能量刻度槽 (Energy Meter)
        int meterY = ry + 28;
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(accent);
        g2.drawString(String.format("⚡ %d/%d", player.getEnergy(), maxEnergy), textX, meterY + 11);

        // 能量点阵 / 分段槽
        int dotStartX = textX + 54;
        int dotCount = maxEnergy;
        int maxDotWidth = rw - (dotStartX - rx) - 6;
        int dotSpacing = Math.min(13, Math.max(7, maxDotWidth / Math.max(1, dotCount)));

        for (int i = 0; i < dotCount; i++) {
            int dx = dotStartX + i * dotSpacing;
            int dy = meterY + 3;
            if (isTurn && i < currentSteps) {
                g2.setColor(new Color(239, 68, 68)); // 红色：本回合已消耗步数
                g2.fillOval(dx, dy, 8, 8);
            } else if (i < player.getEnergy()) {
                g2.setColor(accent); // 当前可用能量
                g2.fillOval(dx, dy, 8, 8);
            } else {
                g2.setColor(new Color(51, 65, 85)); // 灰色容量槽
                g2.drawOval(dx, dy, 8, 8);
            }
        }
    }

    private int drawTacticalCard(Graphics2D g2, GameState state, int x, int y, int w) {
        if (state.isOver()) {
            // 终局胜利卡片 (胜利荣耀红金霓虹)
            int cardH = 78;
            g2.setColor(new Color(69, 10, 10, 200));
            g2.fill(new RoundRectangle2D.Float(x, y, w, cardH, 12, 12));
            g2.setColor(new Color(239, 68, 68));
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Float(x, y, w, cardH, 12, 12));

            String winnerName = (state.getWinner() == 3) ? "双方平局！"
                    : state.getPlayer(state.getWinner()).getName() + " 获得胜利！";
            g2.setColor(new Color(254, 202, 202));
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2.drawString("🏆 " + winnerName, x + 16, y + 28);

            g2.setColor(new Color(226, 232, 240));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String scoreStr = String.format("最终领地: %d格 vs %d格  |  连通边: %d vs %d",
                    state.getP1Territory(), state.getP2Territory(),
                    state.getP1UnblockedEdges(), state.getP2UnblockedEdges());
            g2.drawString(scoreStr, x + 16, y + 54);

            return y + cardH;
        }

        // 行动中：极简战术提示卡片
        int cardH = 72;
        PlayerState currP = state.getCurrentPlayer();
        Color accent = (state.getCurrentTurn() == 1) ? new Color(6, 182, 212) : new Color(245, 158, 11);

        g2.setColor(new Color(21, 32, 54));
        g2.fill(new RoundRectangle2D.Float(x, y, w, cardH, 12, 12));
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, cardH, 12, 12));

        // 第 1 行：行动者与步数
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.setColor(new Color(241, 245, 249));
        int currentSteps = state.getCurrentTurnSteps();
        int remaining = state.getRemainingSteps();
        g2.drawString(String.format("▶ %s  已走 %d 步 · 剩余 %d 步", abbreviateName(currP.getName(), 8), currentSteps, remaining), x + 14, y + 26);

        // 第 2 行：战术操作指令 (键鼠双模支持)
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        if (remaining > 0) {
            g2.setColor(new Color(148, 163, 184));
            g2.drawString("💡 左键点击移动 · 右键定向锁边 | 或使用 WASD + L 键", x + 14, y + 50);
        } else {
            g2.setColor(new Color(251, 146, 60));
            g2.drawString("⚠️ 步数已耗尽，请右键定向锁边或按 L 键交权", x + 14, y + 50);
        }

        return y + cardH;
    }

    private void drawCompactGuide(Graphics2D g2, int x, int y, int w) {
        int cardH = 118;
        g2.setColor(new Color(15, 23, 42));
        g2.fill(new RoundRectangle2D.Float(x, y, w, cardH, 12, 12));
        g2.setColor(new Color(40, 52, 75));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, cardH, 12, 12));

        // 标题
        g2.setColor(new Color(226, 232, 240));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString("战局图例 & 键鼠操作", x + 14, y + 20);

        // 图例色块
        int legendY = y + 42;
        drawLegendBadge(g2, x + 14, legendY, new Color(34, 211, 238), "P1壁障");
        drawLegendBadge(g2, x + 84, legendY, new Color(251, 191, 36), "P2壁障");
        drawLegendBadge(g2, x + 154, legendY, new Color(148, 163, 184), "迷宫墙");

        // 快捷键指南
        int keyY = y + 68;
        drawKeyTag(g2, x + 14, keyY, "左键/WASD", "移动走子");
        drawKeyTag(g2, x + 126, keyY, "右键/L键", "定向锁边");

        keyY += 24;
        drawKeyTag(g2, x + 14, keyY, "P 键", "寻路高亮");
        drawKeyTag(g2, x + 126, keyY, "F11", "全屏切换");
        drawKeyTag(g2, x + 206, keyY, "+ 键", "分先");
    }

    private void drawKeyTag(Graphics2D g2, int x, int y, String key, String desc) {
        g2.setColor(new Color(2, 132, 199));
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.drawString(key, x, y);
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(desc, x + g2.getFontMetrics(new Font("Consolas", Font.BOLD, 11)).stringWidth(key) + 6, y);
    }

    private void drawLegendBadge(Graphics2D g2, int x, int y, Color color, String label) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x, y - 4, x + 16, y - 4);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(203, 213, 225));
        g2.drawString(label, x + 22, y);
    }

    private String abbreviateName(String name, int maxLen) {
        if (name == null) return "";
        if (name.length() <= maxLen) return name;
        return name.substring(0, maxLen - 1) + "…";
    }

    // ==========================================
    // 鼠标交互与悬停预览系统 (v2.6 全新特性)
    // ==========================================

    private void setupMouseListeners() {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GameCanvas.this.requestFocusInWindow();
                handleMouseClick(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverR = -1;
                hoverC = -1;
                hoverDir = null;
                repaint();
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e);
            }
        });
    }

    private void handleMouseMove(MouseEvent e) {
        if (lastCellSize <= 0) return;
        int mx = e.getX();
        int my = e.getY();

        int c = (mx - lastStartX) / lastCellSize;
        int r = (my - lastStartY) / lastCellSize;

        if (r >= 0 && r < lastRows && c >= 0 && c < lastCols && mx >= lastStartX && my >= lastStartY) {
            hoverR = r;
            hoverC = c;
            double cx = lastStartX + c * lastCellSize + lastCellSize / 2.0;
            double cy = lastStartY + r * lastCellSize + lastCellSize / 2.0;
            hoverDir = getDirectionFromOffset(mx - cx, my - cy);
        } else {
            hoverR = -1;
            hoverC = -1;
            hoverDir = null;
        }
        repaint();
    }

    private void handleMouseClick(MouseEvent e) {
        if (lastCellSize <= 0) return;
        int mx = e.getX();
        int my = e.getY();

        int c = (mx - lastStartX) / lastCellSize;
        int r = (my - lastStartY) / lastCellSize;

        if (r < 0 || r >= lastRows || c < 0 || c >= lastCols || mx < lastStartX || my < lastStartY) {
            return;
        }

        GameState state = controller.getGameState();
        if (state.isOver()) return;

        int myPlayerId = controller.getMyPlayerId();
        if (myPlayerId != 0 && state.getCurrentTurn() != myPlayerId) {
            return; // 非玩家回合或AI思考中
        }

        PlayerState currP = state.getCurrentPlayer();
        PlayerState oppP = state.getOpponentPlayer();
        Board board = state.getBoard();

        double cx = lastStartX + c * lastCellSize + lastCellSize / 2.0;
        double cy = lastStartY + r * lastCellSize + lastCellSize / 2.0;
        Direction clickDir = getDirectionFromOffset(mx - cx, my - cy);

        if (SwingUtilities.isLeftMouseButton(e)) {
            // 左键：移动或在当前格调整朝向
            if (r == currP.getR() && c == currP.getC()) {
                controller.handleUserAction(GameAction.changeDirMove(clickDir));
            } else {
                List<Direction> path = GameEvaluator.findPathAvoidingOpponent(
                        board, currP.getR(), currP.getC(), r, c, oppP.getR(), oppP.getC());
                if (!path.isEmpty()) {
                    int distFromStart = GameEvaluator.getDistanceAvoidingOpponent(
                            board, state.getTurnStartR(), state.getTurnStartC(), r, c, oppP.getR(), oppP.getC());
                    if (distFromStart >= 0 && distFromStart <= currP.getEnergy()) {
                        for (Direction step : path) {
                            controller.handleUserAction(GameAction.changeDirMove(step));
                        }
                    }
                }
            }
        } else if (SwingUtilities.isRightMouseButton(e)) {
            // 右键：根据鼠标所在位置确定朝向并锁边
            if (r == currP.getR() && c == currP.getC()) {
                controller.handleUserAction(GameAction.lock(clickDir));
            } else {
                List<Direction> path = GameEvaluator.findPathAvoidingOpponent(
                        board, currP.getR(), currP.getC(), r, c, oppP.getR(), oppP.getC());
                int distFromStart = GameEvaluator.getDistanceAvoidingOpponent(
                        board, state.getTurnStartR(), state.getTurnStartC(), r, c, oppP.getR(), oppP.getC());
                if (!path.isEmpty() && distFromStart >= 0 && distFromStart <= currP.getEnergy()) {
                    for (Direction step : path) {
                        controller.handleUserAction(GameAction.changeDirMove(step));
                    }
                    controller.handleUserAction(GameAction.lock(clickDir));
                } else {
                    for (Direction adj : Direction.values()) {
                        if (currP.getR() + adj.getDr() == r && currP.getC() + adj.getDc() == c) {
                            controller.handleUserAction(GameAction.lock(adj));
                            break;
                        }
                    }
                }
            }
        }
    }

    public static Direction getDirectionFromOffset(double dx, double dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            return dy > 0 ? Direction.DOWN : Direction.UP;
        }
    }

    private void drawMouseHoverIndicator(Graphics2D g2, GameState state, int startX, int startY, int cellSize, int rows, int cols, Set<Long> reachable) {
        if (state.isOver() || hoverR < 0 || hoverR >= rows || hoverC < 0 || hoverC >= cols) return;
        int myId = controller.getMyPlayerId();
        if (myId != 0 && state.getCurrentTurn() != myId) return;

        int hx = startX + hoverC * cellSize;
        int hy = startY + hoverR * cellSize;
        PlayerState currP = state.getCurrentPlayer();
        boolean isCurrentCell = (hoverR == currP.getR() && hoverC == currP.getC());
        boolean isReachable = (reachable != null && reachable.contains(GameEvaluator.encode(hoverR, hoverC)));

        if (!isCurrentCell && !isReachable) return;

        Color highlightColor = (state.getCurrentTurn() == 1) ? new Color(56, 189, 248) : new Color(251, 191, 36);

        // 1. 悬停目标格外边框微光
        g2.setColor(new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), 120));
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new RoundRectangle2D.Float(hx + 1, hy + 1, cellSize - 2, cellSize - 2, 6, 6));

        // 2. 悬停锁边朝向预览指示 (高亮朝向边与小三角标)
        if (hoverDir != null) {
            drawLockPreviewIndicator(g2, hx, hy, cellSize, hoverDir, highlightColor);
        }
    }

    private void drawLockPreviewIndicator(Graphics2D g2, int hx, int hy, int cellSize, Direction dir, Color color) {
        float previewLineWidth = Math.max(3.0f, cellSize * 0.08f);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 220));
        g2.setStroke(new BasicStroke(previewLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int triSize = Math.max(4, cellSize / 8);
        Path2D.Float tri = new Path2D.Float();

        switch (dir) {
            case UP -> {
                g2.drawLine(hx + 2, hy, hx + cellSize - 2, hy);
                int midX = hx + cellSize / 2;
                tri.moveTo(midX, hy + 2);
                tri.lineTo(midX - triSize, hy + 2 + triSize);
                tri.lineTo(midX + triSize, hy + 2 + triSize);
                tri.closePath();
            }
            case DOWN -> {
                g2.drawLine(hx + 2, hy + cellSize, hx + cellSize - 2, hy + cellSize);
                int midX = hx + cellSize / 2;
                tri.moveTo(midX, hy + cellSize - 2);
                tri.lineTo(midX - triSize, hy + cellSize - 2 - triSize);
                tri.lineTo(midX + triSize, hy + cellSize - 2 - triSize);
                tri.closePath();
            }
            case LEFT -> {
                g2.drawLine(hx, hy + 2, hx, hy + cellSize - 2);
                int midY = hy + cellSize / 2;
                tri.moveTo(hx + 2, midY);
                tri.lineTo(hx + 2 + triSize, midY - triSize);
                tri.lineTo(hx + 2 + triSize, midY + triSize);
                tri.closePath();
            }
            case RIGHT -> {
                g2.drawLine(hx + cellSize, hy + 2, hx + cellSize, hy + cellSize - 2);
                int midY = hy + cellSize / 2;
                tri.moveTo(hx + cellSize - 2, midY);
                tri.lineTo(hx + cellSize - 2 - triSize, midY - triSize);
                tri.lineTo(hx + cellSize - 2 - triSize, midY + triSize);
                tri.closePath();
            }
        }
        g2.fill(tri);
    }
}
