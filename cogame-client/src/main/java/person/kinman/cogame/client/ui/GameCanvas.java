package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.audio.AudioPlayer;
import person.kinman.cogame.client.controller.GameController;
import person.kinman.cogame.client.controller.OnlineController;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEvaluator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * 游戏核心画板：支持 6x6~13x13 动态规格、自适应窗口/全屏缩放与高对比度现代暗色视觉
 */
public class GameCanvas extends JPanel {
    private final GameController controller;
    private Image player1Img;
    private Image player2Img;
    private boolean showPath = false;
    private String statusNotification = "";

    public GameCanvas(GameController controller) {
        this.controller = controller;
        this.setBackground(new Color(11, 17, 32)); // 深邃墨蓝底色
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

                // 格子填充色（高对比度）
                if (inPath) {
                    g2.setColor(new Color(14, 116, 144)); // 连通路径高亮青蓝
                } else if (r == state.getP1().getR() && c == state.getP1().getC()) {
                    g2.setColor(new Color(8, 51, 68)); // P1所处位置光晕
                } else if (r == state.getP2().getR() && c == state.getP2().getC()) {
                    g2.setColor(new Color(69, 26, 3)); // P2所处位置光晕
                } else if (isReachable) {
                    g2.setColor(new Color(24, 45, 75)); // 当前回合3步可达范围柔和高亮
                } else {
                    g2.setColor(new Color(30, 41, 59)); // 默认深岩蓝格子
                }
                g2.fillRect(cx, cy, cellSize, cellSize);

                // 格子内部微弱网格分隔线
                g2.setColor(new Color(51, 65, 85, 120));
                g2.setStroke(new BasicStroke(1));
                g2.drawRect(cx, cy, cellSize, cellSize);

                // 绘制高对比度格子编号
                g2.setColor(new Color(148, 163, 184)); // 清晰亮灰字
                g2.setFont(cellFont);
                int cellIndex = r * cols + 1 + c;
                g2.drawString(String.valueOf(cellIndex), cx + 6, cy + fontSize + 4);
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
        drawPlayer(g2, state.getP1(), player1Img, new Color(6, 182, 212), "P1", startX, startY, cellSize);
        drawPlayer(g2, state.getP2(), player2Img, new Color(245, 158, 11), "P2", startX, startY, cellSize);

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
                // 畅通通道：清新翠绿细线，对比度明亮
                g2.setColor(new Color(16, 185, 129, 210));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, y1, x2, y2);
            } else {
                // 已封锁边：根据玩家归属进行高对比区分
                Color glowColor;
                Color coreColor;
                if (locker == 1) {
                    // P1 (先手) 锁边：电光亮青霓虹壁障
                    glowColor = new Color(6, 182, 212, 110);
                    coreColor = new Color(34, 211, 238);
                } else if (locker == 2) {
                    // P2 (后手/AI) 锁边：暖金琥珀/炽焰霓虹壁障
                    glowColor = new Color(245, 158, 11, 110);
                    coreColor = new Color(251, 191, 36);
                } else if (locker == 3) {
                    // 中立预置阻隔墙：钛合金冷灰/玄武岩废墟
                    glowColor = new Color(100, 116, 139, 90);
                    coreColor = new Color(148, 163, 184);
                } else {
                    // 默认警告红
                    glowColor = new Color(239, 68, 68, 100);
                    coreColor = new Color(244, 63, 94);
                }

                // 1. 发光辉光外层
                g2.setColor(glowColor);
                g2.setStroke(new BasicStroke(lockedWidth + 3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, y1, x2, y2);

                // 2. 核心鲜亮实体线
                g2.setColor(coreColor);
                g2.setStroke(new BasicStroke(lockedWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void drawPlayer(Graphics2D g2, PlayerState player, Image img, Color accentColor, String tag, int startX, int startY, int cellSize) {
        int px = startX + player.getC() * cellSize;
        int py = startY + player.getR() * cellSize;

        int margin = Math.max(4, cellSize / 10);
        int avatarSize = cellSize - margin * 2;

        // 玩家光圈底座
        g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60));
        g2.fillOval(px + margin - 2, py + margin - 2, avatarSize + 4, avatarSize + 4);

        // 头像绘制
        if (img != null) {
            Shape oldClip = g2.getClip();
            g2.setClip(new java.awt.geom.Ellipse2D.Float(px + margin, py + margin, avatarSize, avatarSize));
            g2.drawImage(img, px + margin, py + margin, avatarSize, avatarSize, null);
            g2.setClip(oldClip);
        }

        // 外围高对比轮廓圆环
        g2.setColor(accentColor);
        g2.setStroke(new BasicStroke(3.0f));
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
        g2.setColor(new Color(17, 24, 39));
        g2.fillRect(x, y, width, height);
        g2.setColor(new Color(31, 41, 55));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x, y, x, y + height);

        int pad = 20;
        int innerWidth = width - pad * 2;
        int curY = y + 24;

        // 顶部模式徽章
        g2.setColor(new Color(30, 41, 59));
        g2.fill(new RoundRectangle2D.Float(x + pad, curY, innerWidth, 34, 10, 10));
        g2.setColor(new Color(56, 189, 248));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        String titleStr = controller.getModeName() + " (" + state.getRows() + "×" + state.getCols() + ")";
        g2.drawString(titleStr, x + pad + 12, curY + 22);
        curY += 46;

        // 玩家 1 卡片 (先手 - 青色系)
        drawPlayerCard(g2, x + pad, curY, innerWidth, state.getP1(), player1Img,
                new Color(6, 182, 212), state.getCurrentTurn() == 1,
                state.isOver() ? state.getP1Territory() : -1,
                state.isOver() ? state.getP1UnblockedEdges() : -1);
        curY += 105;

        // 玩家 2 卡片 (后手 - 琥珀色系)
        drawPlayerCard(g2, x + pad, curY, innerWidth, state.getP2(), player2Img,
                new Color(245, 158, 11), state.getCurrentTurn() == 2,
                state.isOver() ? state.getP2Territory() : -1,
                state.isOver() ? state.getP2UnblockedEdges() : -1);
        curY += 115;

        // 动态能量池卡片
        g2.setColor(new Color(30, 41, 59));
        g2.fill(new RoundRectangle2D.Float(x + pad, curY, innerWidth, 58, 10, 10));
        g2.setColor(new Color(56, 189, 248));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(x + pad, curY, innerWidth, 58, 10, 10));

        PlayerState currP = state.getCurrentPlayer();
        int maxEnergy = state.getMaxEnergy();
        int regen = state.getEnergyRegen();
        int curEnergy = currP.getEnergy();
        int currentSteps = state.getCurrentTurnSteps();

        g2.setColor(new Color(241, 245, 249));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String energyTitle = String.format("能量池: %d/%d (每回合+%d)", curEnergy, maxEnergy, regen);
        g2.drawString(energyTitle, x + pad + 12, curY + 20);

        int dotCount = maxEnergy;
        int dotSpacing = Math.min(18, Math.max(10, (innerWidth - 180) / dotCount));
        for (int i = 0; i < dotCount; i++) {
            int dotX = x + pad + 170 + i * dotSpacing;
            int dotY = curY + 10;
            if (i < currentSteps) {
                g2.setColor(new Color(239, 68, 68)); // 红色：本回合已消耗移动步数
                g2.fillOval(dotX, dotY, 12, 12);
            } else if (i < curEnergy) {
                g2.setColor(new Color(56, 189, 248)); // 亮青色：当前可用剩余能量
                g2.fillOval(dotX, dotY, 12, 12);
            } else {
                g2.setColor(new Color(71, 85, 105)); // 灰色圆环：未蓄满容量
                g2.drawOval(dotX, dotY, 12, 12);
            }
        }

        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString("本回合已走 " + currentSteps + " 步，还可走 " + state.getRemainingSteps() + " 步 | L键锁边交权", x + pad + 12, curY + 44);
        curY += 70;

        // 操作指南小卡片
        g2.setColor(new Color(30, 41, 59));
        int guideHeight = 190;
        g2.fill(new RoundRectangle2D.Float(x + pad, curY, innerWidth, guideHeight, 12, 12));
        g2.setColor(new Color(51, 65, 85));
        g2.draw(new RoundRectangle2D.Float(x + pad, curY, innerWidth, guideHeight, 12, 12));

        g2.setColor(new Color(241, 245, 249));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString("操作指南 & 边框图例", x + pad + 14, curY + 22);

        // 边框图例展示
        int legendY = curY + 40;
        drawLegendBadge(g2, x + pad + 12, legendY, new Color(16, 185, 129), "通路");
        drawLegendBadge(g2, x + pad + 82, legendY, new Color(34, 211, 238), "P1锁边");
        drawLegendBadge(g2, x + pad + 158, legendY, new Color(251, 191, 36), "P2锁边");
        drawLegendBadge(g2, x + pad + 234, legendY, new Color(148, 163, 184), "中立墙");

        int lineY = curY + 65;
        drawKeyGuideRow(g2, x + pad + 14, lineY, "WASD / 方向键", "移动并设定朝向"); lineY += 21;
        drawKeyGuideRow(g2, x + pad + 14, lineY, "SPACE", "沿朝向前进一步"); lineY += 21;
        drawKeyGuideRow(g2, x + pad + 14, lineY, "R 键", "顺时针旋转90°"); lineY += 21;
        drawKeyGuideRow(g2, x + pad + 14, lineY, "L 键", "封锁边 (切回合)"); lineY += 21;
        drawKeyGuideRow(g2, x + pad + 14, lineY, "P 键 / F11", "寻路高亮 / 全屏"); lineY += 21;
        drawKeyGuideRow(g2, x + pad + 14, lineY, "+ 键", "重置棋局");
        curY += guideHeight + 20;

        // 游戏终局结算面板
        if (state.isOver()) {
            g2.setColor(new Color(239, 68, 68, 30));
            g2.fill(new RoundRectangle2D.Float(x + pad, curY, innerWidth, 75, 12, 12));
            g2.setColor(new Color(239, 68, 68));
            g2.draw(new RoundRectangle2D.Float(x + pad, curY, innerWidth, 75, 12, 12));

            g2.setColor(new Color(248, 113, 113));
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            String winnerText = (state.getWinner() == 3) ? "★ 双方战平！"
                    : "★ " + state.getPlayer(state.getWinner()).getName() + " 获胜！";
            g2.drawString(winnerText, x + pad + 16, curY + 28);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(Color.WHITE);
            String scoreText = String.format("领地: %d格 vs %d格 | 连通边: %d vs %d",
                    state.getP1Territory(), state.getP2Territory(),
                    state.getP1UnblockedEdges(), state.getP2UnblockedEdges());
            g2.drawString(scoreText, x + pad + 16, curY + 54);
            curY += 85;
        }

        // 底部通知栏
        if (!statusNotification.isEmpty()) {
            g2.setColor(new Color(56, 189, 248));
            g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g2.drawString("ℹ " + statusNotification, x + pad, height - 16);
        }
    }

    private void drawPlayerCard(Graphics2D g2, int cx, int cy, int cWidth, PlayerState player, Image avatar, Color accent, boolean isTurn, int territory, int edges) {
        // 卡片底色
        g2.setColor(isTurn ? new Color(30, 41, 59) : new Color(15, 23, 42));
        g2.fill(new RoundRectangle2D.Float(cx, cy, cWidth, 90, 12, 12));

        // 轮到自己行动时的突出发光边框
        if (isTurn) {
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new RoundRectangle2D.Float(cx, cy, cWidth, 90, 12, 12));

            // 行动中标签
            g2.setColor(accent);
            g2.fill(new RoundRectangle2D.Float(cx + cWidth - 75, cy + 8, 65, 20, 6, 6));
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("行动中 ▶", cx + cWidth - 68, cy + 22);
        } else {
            g2.setColor(new Color(51, 65, 85));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(cx, cy, cWidth, 90, 12, 12));
        }

        // 头像
        int avSize = 56;
        if (avatar != null) {
            Shape oldClip = g2.getClip();
            g2.setClip(new java.awt.geom.Ellipse2D.Float(cx + 14, cy + 17, avSize, avSize));
            g2.drawImage(avatar, cx + 14, cy + 17, avSize, avSize, null);
            g2.setClip(oldClip);
        }
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval(cx + 14, cy + 17, avSize, avSize);

        // 昵称与身位
        g2.setColor(new Color(248, 250, 252));
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.drawString(player.getName(), cx + 80, cy + 30);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(148, 163, 184));
        g2.drawString("身位: " + (player.getId() == 1 ? "先手 (P1)" : "后手 (P2)"), cx + 80, cy + 50);
        g2.drawString("朝向: " + player.getDirection().getName() + " | 位置: (" + player.getR() + "," + player.getC() + ")", cx + 80, cy + 70);

        // 若已终局显示领地
        if (territory >= 0) {
            g2.setColor(accent);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString(territory + " 格 / " + edges + " 边", cx + cWidth - 85, cy + 70);
        }
    }

    private void drawKeyGuideRow(Graphics2D g2, int x, int y, String key, String desc) {
        g2.setColor(new Color(2, 132, 199));
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.drawString(String.format("%-14s", key), x, y);
        g2.setColor(new Color(203, 213, 225));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(desc, x + 105, y);
    }

    private void drawLegendBadge(Graphics2D g2, int x, int y, Color color, String label) {
        // 绘制小样色条
        g2.setColor(color);
        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x, y - 4, x + 16, y - 4);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(203, 213, 225));
        g2.drawString(label, x + 22, y);
    }
}
