package person.kinman.cogame.client.cli;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import person.kinman.cogame.ai.AiDecision;
import person.kinman.cogame.ai.cli.BattleCli;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.net.WsMessage;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 命令行网络联机客户端：支持 Antigravity 与 Codex 通过本地 8088 端口真实对战
 */
public class OnlineCli {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_GRAY = "\u001B[90m";
    private static final String ANSI_BOLD = "\u001B[1m";

    public static void main(String[] args) throws Exception {
        String serverUrl = "ws://127.0.0.1:8088";
        String roomId = "2026";
        String playerName = "Player";
        int boardSize = 6;
        String strategyType = "antigravity";
        boolean render = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--server" -> { if (i + 1 < args.length) serverUrl = args[++i]; }
                case "--room", "-r" -> { if (i + 1 < args.length) roomId = args[++i]; }
                case "--name", "-n" -> { if (i + 1 < args.length) playerName = args[++i]; }
                case "--size", "-s" -> { if (i + 1 < args.length) boardSize = Integer.parseInt(args[++i]); }
                case "--strategy" -> { if (i + 1 < args.length) strategyType = args[++i].toLowerCase(); }
                case "--no-render" -> render = false;
                case "--help", "-h" -> {
                    System.out.println("用法: online-cli --room <房号> --name <玩家名> [--size 6|9|12] [--strategy antigravity|codex]");
                    return;
                }
            }
        }

        System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "       COGame - 网络联机对战 CLI (Online WebSocket Client)      " + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);
        System.out.printf("服务器: %s | 房间号: %s | 玩家名: %s | 策略: %s\n\n", serverUrl, roomId, playerName, strategyType.toUpperCase());

        BattleCli.CombatantAi strategy = "codex".equals(strategyType) ? new BattleCli.CodexAi() : new BattleCli.AntigravityAi();
        CountDownLatch gameOverLatch = new CountDownLatch(1);
        AtomicBoolean isMyTurnActing = new AtomicBoolean(false);

        final String finalRoomId = roomId;
        final String finalPlayerName = playerName;
        final int finalBoardSize = boardSize;
        final boolean finalRender = render;

        WebSocketClient client = new WebSocketClient(new URI(serverUrl)) {
            private int myPlayerId = 0;
            private GameState state = null;

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("● 已连接服务器，正在加入/创建房间 [" + finalRoomId + "]...");
                WsMessage joinMsg = WsMessage.joinRoom(finalRoomId, finalPlayerName, finalBoardSize, null);
                send(joinMsg.toJson());
            }

            @Override
            public void onMessage(String message) {
                WsMessage msg = WsMessage.fromJson(message);
                if (msg == null) return;

                switch (msg.getType()) {
                    case WsMessage.TYPE_ROOM_INFO -> {
                        myPlayerId = msg.getAssignedPlayerId();
                        System.out.printf("⏳ 房间等待中... 我是席位 P%d，等待对手加入房间 [%s]\n", myPlayerId, finalRoomId);
                    }
                    case WsMessage.TYPE_GAME_START -> {
                        myPlayerId = msg.getAssignedPlayerId();
                        state = msg.getState();
                        System.out.println(ANSI_GREEN + ANSI_BOLD + "\n⚔️ 对手已加入，游戏正式开战！我是 P" + myPlayerId + " (" + finalPlayerName + ")" + ANSI_RESET);
                        if (finalRender) renderBoard(state);
                        checkAndAct();
                    }
                    case WsMessage.TYPE_STATE_UPDATE -> {
                        state = msg.getState();
                        if (finalRender) renderBoard(state);
                        checkAndAct();
                    }
                    case WsMessage.TYPE_GAME_OVER -> {
                        state = msg.getState();
                        if (finalRender) renderBoard(state);
                        String winnerName = (state.getWinner() == 1) ? state.getP1().getName() : (state.getWinner() == 2 ? state.getP2().getName() : "平局");
                        System.out.println(ANSI_GREEN + ANSI_BOLD + "\n🏆 对局结束！胜者: 【" + winnerName + "】 | 判定: " + state.getWinReason() + ANSI_RESET);
                        System.out.printf("最终领地: P1(%s)=%d格, P2(%s)=%d格\n",
                                state.getP1().getName(), state.getP1Territory(),
                                state.getP2().getName(), state.getP2Territory());
                        gameOverLatch.countDown();
                    }
                    case WsMessage.TYPE_PLAYER_LEFT -> {
                        System.out.println("⚠️ 对手离开了房间！");
                        gameOverLatch.countDown();
                    }
                    case WsMessage.TYPE_ERROR -> {
                        System.out.println("❌ 提示: " + msg.getMessage());
                    }
                }
            }

            private void checkAndAct() {
                if (state == null || state.isOver()) return;
                if (state.getCurrentTurn() == myPlayerId && isMyTurnActing.compareAndSet(false, true)) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(150); // 微量延迟增强对战观赏性
                            PlayerState p = state.getCurrentPlayer();
                            AiDecision decision = strategy.planTurn(state, myPlayerId);

                            String color = (myPlayerId == 1) ? ANSI_CYAN : ANSI_YELLOW;
                            System.out.printf("\n%s▶ [P%d | %s] 能量:%d/%d | 决策: %s%s\n",
                                    color, myPlayerId, finalPlayerName, p.getEnergy(), state.getMaxEnergy(),
                                    decision.getDescription(), ANSI_RESET);

                            for (GameAction act : decision.getActions()) {
                                send(WsMessage.action(act).toJson());
                                Thread.sleep(40);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            isMyTurnActing.set(false);
                        }
                    }).start();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("● 连接关闭 (" + reason + ")");
                gameOverLatch.countDown();
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("❌ 网络异常: " + (ex != null ? ex.getMessage() : ""));
            }
        };

        client.connect();
        gameOverLatch.await();
        client.close();
        System.out.println("程序退出。");
    }

    private static void renderBoard(GameState state) {
        Board board = state.getBoard();
        int rows = board.getRows();
        int cols = board.getCols();
        PlayerState p1 = state.getP1();
        PlayerState p2 = state.getP2();

        System.out.println("   " + ANSI_GRAY + "+---".repeat(cols) + "+" + ANSI_RESET);
        for (int r = 0; r < rows; r++) {
            StringBuilder line = new StringBuilder("   |");
            for (int c = 0; c < cols; c++) {
                String mark = "   ";
                if (p1.getR() == r && p1.getC() == c) mark = ANSI_CYAN + " 1 " + ANSI_RESET;
                else if (p2.getR() == r && p2.getC() == c) mark = ANSI_YELLOW + " 2 " + ANSI_RESET;
                line.append(mark);

                if (c < cols - 1) {
                    boolean open = board.isConnected(r, c, Direction.RIGHT);
                    int locker = board.getEdgeLocker(r, c, Direction.RIGHT);
                    if (open) line.append(ANSI_GRAY + " " + ANSI_RESET);
                    else if (locker == 1) line.append(ANSI_CYAN + "|" + ANSI_RESET);
                    else if (locker == 2) line.append(ANSI_YELLOW + "|" + ANSI_RESET);
                    else if (locker == 3) line.append(ANSI_GRAY + "#" + ANSI_RESET);
                    else line.append(ANSI_GRAY + "|" + ANSI_RESET);
                } else {
                    line.append(ANSI_GRAY + "|" + ANSI_RESET);
                }
            }
            System.out.println(line);

            if (r < rows - 1) {
                StringBuilder hLine = new StringBuilder("   +");
                for (int c = 0; c < cols; c++) {
                    boolean open = board.isConnected(r, c, Direction.DOWN);
                    int locker = board.getEdgeLocker(r, c, Direction.DOWN);
                    if (open) hLine.append(ANSI_GRAY + "   +" + ANSI_RESET);
                    else if (locker == 1) hLine.append(ANSI_CYAN + "---+" + ANSI_RESET);
                    else if (locker == 2) hLine.append(ANSI_YELLOW + "---+" + ANSI_RESET);
                    else if (locker == 3) hLine.append(ANSI_GRAY + "###+" + ANSI_RESET);
                    else hLine.append(ANSI_GRAY + "---+" + ANSI_RESET);
                }
                System.out.println(hLine);
            }
        }
        System.out.println("   " + ANSI_GRAY + "+---".repeat(cols) + "+" + ANSI_RESET);
    }
}
