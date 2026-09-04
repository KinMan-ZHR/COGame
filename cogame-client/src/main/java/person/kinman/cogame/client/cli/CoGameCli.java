package person.kinman.cogame.client.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import person.kinman.cogame.client.cli.agent.AgentFactory;
import person.kinman.cogame.client.cli.agent.PlayerAgent;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.net.WsMessage;
import person.kinman.cogame.core.rule.GameEngine;

import java.io.FileWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * COGame 终极统一全功能 CLI 竞技场：
 * 深度融合「人与人」「人与AI」「AI与AI」于一体，全面支持本地对战、网络联机与多轮压测基准
 */
public class CoGameCli {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_GRAY = "\u001B[90m";
    private static final String ANSI_BOLD = "\u001B[1m";

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
            printRootHelp();
            return;
        }

        String subcmd = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subcmd) {
            case "play" -> runPlayCommand(subArgs);
            case "online" -> runOnlineCommand(subArgs);
            case "bench" -> runBenchCommand(subArgs);
            case "step" -> runStepCommand(subArgs);
            default -> {
                System.err.println("未知子命令: " + subcmd + "，请参阅 --help");
                printRootHelp();
            }
        }
    }

    private static void runStepCommand(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("用法: cogame step <start|move> [选项]");
            return;
        }
        String action = args[0].toLowerCase();
        String file = "/tmp/cogame_step.json";
        int size = 6;
        String engine = "antigravity";
        int choice = 0;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--file", "-f" -> { if (i + 1 < args.length) file = args[++i]; }
                case "--size", "-s" -> { if (i + 1 < args.length) size = Integer.parseInt(args[++i]); }
                case "--engine", "-e" -> { if (i + 1 < args.length) engine = args[++i]; }
                case "--choice", "-c" -> { if (i + 1 < args.length) choice = Integer.parseInt(args[++i]); }
            }
        }

        if (action.equals("start")) {
            StepBattle.start(size, engine, file);
        } else if (action.equals("move")) {
            StepBattle.move(file, choice);
        } else {
            System.err.println("未知 step 动作: " + action + " (支持 start 或 move)");
        }
    }

    // =========================================================================
    // 1. 本地模式 (play): 人与人 / 人与AI / AI与AI
    // =========================================================================
    private static void runPlayCommand(String[] args) throws Exception {
        int size = 6;
        String p1Spec = "human";
        String p2Spec = "antigravity";
        boolean render = true;
        int delayMs = 150;
        String recordPath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--size", "-s" -> { if (i + 1 < args.length) size = Integer.parseInt(args[++i]); }
                case "--p1" -> { if (i + 1 < args.length) p1Spec = args[++i]; }
                case "--p2" -> { if (i + 1 < args.length) p2Spec = args[++i]; }
                case "--no-render" -> render = false;
                case "--delay", "-d" -> { if (i + 1 < args.length) delayMs = Integer.parseInt(args[++i]); }
                case "--record" -> { if (i + 1 < args.length) recordPath = args[++i]; }
            }
        }

        try (PlayerAgent p1 = AgentFactory.create(p1Spec);
             PlayerAgent p2 = AgentFactory.create(p2Spec)) {

            System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);
            System.out.println(ANSI_CYAN + ANSI_BOLD + "       COGame - 本地对战对弈 (Local Match Runner)               " + ANSI_RESET);
            System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);
            System.out.printf("棋盘: %d×%d | P1 (先手·青): %s | P2 (后手·金): %s\n\n",
                    size, size, p1.getName(), p2.getName());

            GameState state = new GameState(size);
            state.getP1().setName(p1.getName());
            state.getP2().setName(p2.getName());

            List<Map<String, Object>> replayTurns = new ArrayList<>();
            int turn = 0;

            if (render) renderBoard(state);

            while (!state.isOver() && turn < 200) {
                turn++;
                int currId = state.getCurrentTurn();
                PlayerAgent current = (currId == 1) ? p1 : p2;
                PlayerState playerState = state.getCurrentPlayer();

                long startTime = System.currentTimeMillis();
                List<GameAction> actions = current.act(state, currId);
                long latency = System.currentTimeMillis() - startTime;

                int steps = 0;
                Direction lockedDir = null;
                for (GameAction act : actions) {
                    if (act.getType() == GameAction.Type.CHANGE_DIR_MOVE || act.getType() == GameAction.Type.MOVE) {
                        steps++;
                    } else if (act.getType() == GameAction.Type.LOCK) {
                        lockedDir = playerState.getDirection();
                    }
                    GameEngine.executeAction(state, currId, act);
                }

                String pColor = (currId == 1) ? ANSI_CYAN : ANSI_YELLOW;
                System.out.printf("  %s[T%02d | %s(P%d)]%s 走%d步 锁[%s] | 耗时:%dms | 当前坐标:(%d,%d)\n",
                        pColor, turn, current.getName(), currId, ANSI_RESET,
                        steps, (lockedDir != null ? lockedDir.getName() : "-"), latency, playerState.getR(), playerState.getC());

                if (render) renderBoard(state);

                if (recordPath != null) {
                    Map<String, Object> tLog = new LinkedHashMap<>();
                    tLog.put("turn", turn);
                    tLog.put("player", currId);
                    tLog.put("name", current.getName());
                    tLog.put("steps", steps);
                    tLog.put("latencyMs", latency);
                    tLog.put("pos", List.of(playerState.getR(), playerState.getC()));
                    replayTurns.add(tLog);
                }

                if (delayMs > 0 && !p1Spec.contains("human") && !p2Spec.contains("human")) {
                    Thread.sleep(delayMs);
                }
            }

            p1.onGameOver(state, 1);
            p2.onGameOver(state, 2);

            String winnerName = (state.getWinner() == 1) ? p1.getName() : (state.getWinner() == 2 ? p2.getName() : "平局");
            System.out.println(ANSI_GREEN + ANSI_BOLD + "\n🏆 对局结束！胜者: 【" + winnerName + "】 | 判定: " + state.getWinReason() + ANSI_RESET);
            System.out.printf("最终领地: P1(%s)=%d格, P2(%s)=%d格 | 总回合数: %d\n",
                    p1.getName(), state.getP1Territory(), p2.getName(), state.getP2Territory(), turn);

            if (recordPath != null) {
                saveReplayFile(recordPath, size, p1.getName(), p2.getName(), winnerName, state, replayTurns);
            }
        }
    }

    // =========================================================================
    // 2. 网络联机模式 (online): 任意实体连接 8088 端口
    // =========================================================================
    private static void runOnlineCommand(String[] args) throws Exception {
        String serverUrl = "ws://127.0.0.1:8088";
        String roomId = "8888";
        int boardSize = 6;
        String playerSpec = "human";
        String customName = null;
        boolean render = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--server" -> { if (i + 1 < args.length) serverUrl = args[++i]; }
                case "--room", "-r" -> { if (i + 1 < args.length) roomId = args[++i]; }
                case "--size", "-s" -> { if (i + 1 < args.length) boardSize = Integer.parseInt(args[++i]); }
                case "--player", "-p" -> { if (i + 1 < args.length) playerSpec = args[++i]; }
                case "--name", "-n" -> { if (i + 1 < args.length) customName = args[++i]; }
                case "--no-render" -> render = false;
            }
        }

        try (PlayerAgent agent = AgentFactory.create(playerSpec)) {
            String finalPlayerName = (customName != null) ? customName : agent.getName();
            CountDownLatch finishLatch = new CountDownLatch(1);
            AtomicBoolean isActing = new AtomicBoolean(false);

            final String fRoom = roomId;
            final int fSize = boardSize;
            final boolean fRender = render;

            System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);
            System.out.println(ANSI_CYAN + ANSI_BOLD + "       COGame - 网络联机模式 (Online WebSocket Arena)           " + ANSI_RESET);
            System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);
            System.out.printf("服务器: %s | 房间: [%s] | 实体: %s (%s)\n\n",
                    serverUrl, roomId, finalPlayerName, playerSpec);

            WebSocketClient client = new WebSocketClient(new URI(serverUrl)) {
                private int myPlayerId = 0;
                private GameState state = null;

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("● 已连接服务器，正在加入房间 [" + fRoom + "]...");
                    WsMessage joinMsg = WsMessage.joinRoom(fRoom, finalPlayerName, fSize, null);
                    send(joinMsg.toJson());
                }

                @Override
                public void onMessage(String message) {
                    WsMessage msg = WsMessage.fromJson(message);
                    if (msg == null) return;

                    switch (msg.getType()) {
                        case WsMessage.TYPE_ROOM_INFO -> {
                            myPlayerId = msg.getAssignedPlayerId();
                            System.out.printf("⏳ 房间就绪: 我是席位 P%d，等待对手加入房间 [%s]...\n", myPlayerId, fRoom);
                        }
                        case WsMessage.TYPE_GAME_START -> {
                            myPlayerId = msg.getAssignedPlayerId();
                            state = msg.getState();
                            System.out.println(ANSI_GREEN + ANSI_BOLD + "\n⚔️ 比赛正式开战！我是席位 P" + myPlayerId + " (" + finalPlayerName + ")" + ANSI_RESET);
                            if (fRender) renderBoard(state);
                            checkAndAct();
                        }
                        case WsMessage.TYPE_STATE_UPDATE -> {
                            state = msg.getState();
                            if (fRender) renderBoard(state);
                            checkAndAct();
                        }
                        case WsMessage.TYPE_GAME_OVER -> {
                            state = msg.getState();
                            if (fRender) renderBoard(state);
                            String winner = (state.getWinner() == 1) ? state.getP1().getName() :
                                    (state.getWinner() == 2 ? state.getP2().getName() : "平局");
                            System.out.println(ANSI_GREEN + ANSI_BOLD + "\n🏆 对局结束！胜者: 【" + winner + "】 | " + state.getWinReason() + ANSI_RESET);
                            System.out.printf("最终领地: P1(%s)=%d格, P2(%s)=%d格\n",
                                    state.getP1().getName(), state.getP1Territory(),
                                    state.getP2().getName(), state.getP2Territory());
                            agent.onGameOver(state, myPlayerId);
                            finishLatch.countDown();
                        }
                        case WsMessage.TYPE_PLAYER_LEFT -> {
                            System.out.println("⚠️ 对手离开了房间！");
                            finishLatch.countDown();
                        }
                        case WsMessage.TYPE_ERROR -> {
                            System.out.println("❌ 提示: " + msg.getMessage());
                        }
                    }
                }

                private void checkAndAct() {
                    if (state == null || state.isOver()) return;
                    if (state.getCurrentTurn() == myPlayerId && isActing.compareAndSet(false, true)) {
                        new Thread(() -> {
                            try {
                                List<GameAction> actions = agent.act(state, myPlayerId);
                                for (GameAction act : actions) {
                                    send(WsMessage.action(act).toJson());
                                    Thread.sleep(30);
                                }
                            } catch (Exception e) {
                                System.err.println("决策执行异常: " + e.getMessage());
                            } finally {
                                isActing.set(false);
                            }
                        }).start();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("● 连接关闭 (" + reason + ")");
                    finishLatch.countDown();
                }

                @Override
                public void onError(Exception ex) {
                    System.out.println("❌ 网络异常: " + (ex != null ? ex.getMessage() : ""));
                }
            };

            client.connect();
            finishLatch.await();
            client.close();
        }
    }

    // =========================================================================
    // 3. 基准压测模式 (bench): 批量极速对战评测
    // =========================================================================
    private static void runBenchCommand(String[] args) throws Exception {
        int size = 6;
        String p1Spec = "antigravity";
        String p2Spec = "codex";
        int games = 5;
        String summaryJson = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--size", "-s" -> { if (i + 1 < args.length) size = Integer.parseInt(args[++i]); }
                case "--p1" -> { if (i + 1 < args.length) p1Spec = args[++i]; }
                case "--p2" -> { if (i + 1 < args.length) p2Spec = args[++i]; }
                case "--games", "-g" -> { if (i + 1 < args.length) games = Integer.parseInt(args[++i]); }
                case "--summary-json" -> { if (i + 1 < args.length) summaryJson = args[++i]; }
            }
        }

        System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);
        System.out.printf("       COGame AI Benchmark: %s vs %s (%d 局基准评测)\n", p1Spec, p2Spec, games);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);

        int p1Wins = 0, p2Wins = 0, draws = 0;
        int totalTurns = 0;

        for (int g = 1; g <= games; g++) {
            try (PlayerAgent p1 = AgentFactory.create(p1Spec);
                 PlayerAgent p2 = AgentFactory.create(p2Spec)) {
                GameState state = new GameState(size);
                int turn = 0;
                while (!state.isOver() && turn < 200) {
                    turn++;
                    int currId = state.getCurrentTurn();
                    PlayerAgent current = (currId == 1) ? p1 : p2;
                    List<GameAction> actions = current.act(state, currId);
                    for (GameAction act : actions) {
                        GameEngine.executeAction(state, currId, act);
                    }
                }
                totalTurns += turn;
                if (state.getWinner() == 1) p1Wins++;
                else if (state.getWinner() == 2) p2Wins++;
                else draws++;

                System.out.printf("  [局 %02d/%02d] 胜者: P%d (%s) | 领地: %d:%d | 回合: %d\n",
                        g, games, state.getWinner(),
                        (state.getWinner() == 1 ? p1.getName() : (state.getWinner() == 2 ? p2.getName() : "平局")),
                        state.getP1Territory(), state.getP2Territory(), turn);
            }
        }

        System.out.println("\n----------------------------------------------------------------");
        System.out.printf("评测结果: P1 胜率: %.1f%% (%d胜) | P2 胜率: %.1f%% (%d胜) | 平局: %d\n",
                p1Wins * 100.0 / games, p1Wins, p2Wins * 100.0 / games, p2Wins, draws);
        System.out.printf("平均对局回合数: %.1f 回合\n", totalTurns * 1.0 / games);
        System.out.println("----------------------------------------------------------------");

        if (summaryJson != null) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("timestamp", Instant.now().toString());
            summary.put("p1", p1Spec);
            summary.put("p2", p2Spec);
            summary.put("boardSize", size);
            summary.put("totalGames", games);
            summary.put("p1Wins", p1Wins);
            summary.put("p2Wins", p2Wins);
            summary.put("draws", draws);
            summary.put("p1WinRate", p1Wins * 1.0 / games);
            summary.put("p2WinRate", p2Wins * 1.0 / games);
            summary.put("avgTurns", totalTurns * 1.0 / games);
            try (FileWriter fw = new FileWriter(summaryJson, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(summary, fw);
                System.out.println("基准报告已成功保存至: " + summaryJson);
            }
        }
    }

    private static void saveReplayFile(String path, int size, String p1, String p2, String winner, GameState state, List<Map<String, Object>> turns) {
        try (FileWriter fw = new FileWriter(path, StandardCharsets.UTF_8)) {
            Map<String, Object> replay = new LinkedHashMap<>();
            replay.put("game", "COGame");
            replay.put("timestamp", Instant.now().toString());
            replay.put("size", size);
            replay.put("p1", p1);
            replay.put("p2", p2);
            replay.put("winner", winner);
            replay.put("p1Territory", state.getP1Territory());
            replay.put("p2Territory", state.getP2Territory());
            replay.put("turns", turns);
            new GsonBuilder().setPrettyPrinting().create().toJson(replay, fw);
            System.out.println("📁 完整对局棋谱录像已保存至: " + path);
        } catch (Exception e) {
            System.err.println("保存录像失败: " + e.getMessage());
        }
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

    private static void printRootHelp() {
        System.out.println("================================================================");
        System.out.println("         COGame - 终极统一全功能对弈与竞技 CLI                  ");
        System.out.println("================================================================");
        System.out.println("支持场景: [人与人] [人与AI] [AI与AI] (本地/联机/多轮基准)");
        System.out.println("");
        System.out.println("可用子命令:");
        System.out.println("  play     本地单机对决 (支持任意玩家类型组合)");
        System.out.println("  online   网络联机对决 (连接 8088 端口房间，支持真人或 AI 挂载参赛)");
        System.out.println("  bench    多轮无头自动化压测基准 (AI vs AI 胜率与策略评估)");
        System.out.println("");
        System.out.println("参赛实体 (Player Spec) 支持类型:");
        System.out.println("  human                  人类终端交互输入 (编号或快捷键)");
        System.out.println("  antigravity            内置策略控盘流 AI");
        System.out.println("  codex                  内置极限压迫流 AI");
        System.out.println("  classic                内置端脑守卫均衡流 AI");
        System.out.println("  llm:codex              真实调起本地 OpenAI Codex 大模型参赛");
        System.out.println("  llm:gemini             真实调起本地 Google Gemini 大模型参赛");
        System.out.println("  cmd:\"python3 bot.py\"   挂载任意第三方外部脚本 (通过 Stdin/Stdout JSON 对战)");
        System.out.println("");
        System.out.println("示例:");
        System.out.println("  1. 人机终端对决:   cogame play --p1 human --p2 codex");
        System.out.println("  2. 双AI本地神仙架: cogame play --p1 llm:gemini --p2 llm:codex --size 12");
        System.out.println("  3. 挂载Python脚本: cogame play --p1 human --p2 'cmd:python3 my_bot.py'");
        System.out.println("  4. AI 联网打天梯:  cogame online --room 8888 --player llm:codex");
        System.out.println("  5. 多轮算法压测:   cogame bench --p1 antigravity --p2 codex --games 10");
        System.out.println("================================================================");
    }
}
