package person.kinman.cogame.ai.cli;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * COGame 外部大模型逐回合竞技场。
 *
 * <p>每回合向模型提供当前完整局面和所有合法的“移动路径 + 锁边”候选，模型只负责选择候选编号，
 * 最终状态变更仍由 {@link GameEngine} 校验和执行。此入口不会调用任何内置 AI 策略。</p>
 */
public final class LlmBattleCli {
    private static final Pattern CHOICE_PATTERN = Pattern.compile("\\\"choice\\\"\\s*:\\s*(\\d+)");
    private static final Duration MODEL_TIMEOUT = Duration.ofMinutes(10);
    private static final int MAX_TURNS = 200;

    private LlmBattleCli() {
    }

    private record Candidate(int id, List<Direction> path, Direction lockDirection, int row, int col) {
    }

    private interface ModelPlayer {
        String name();

        int choose(String prompt) throws IOException, InterruptedException;
    }

    private static final class CommandModelPlayer implements ModelPlayer {
        private final String name;
        private final List<String> commandPrefix;

        private CommandModelPlayer(String name, List<String> commandPrefix) {
            this.name = Objects.requireNonNull(name, "name");
            this.commandPrefix = List.copyOf(commandPrefix);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int choose(String prompt) throws IOException, InterruptedException {
            List<String> command = new ArrayList<>(commandPrefix);
            command.add(prompt);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            process.getOutputStream().close();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thread outputReader = new Thread(() -> {
                try {
                    process.getInputStream().transferTo(output);
                } catch (IOException ignored) {
                    // The main thread reports process failures and timeouts with the output captured so far.
                }
            }, "cogame-" + name + "-output-reader");
            outputReader.start();
            boolean finished = process.waitFor(MODEL_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(TimeUnit.SECONDS.toMillis(5));
                throw new IOException(name + " response timed out after " + MODEL_TIMEOUT.toSeconds() + " seconds");
            }
            outputReader.join(TimeUnit.SECONDS.toMillis(5));
            String response = output.toString(StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IOException(name + " exited with code " + process.exitValue() + ": " + abbreviate(response));
            }

            Matcher matcher = CHOICE_PATTERN.matcher(response);
            Integer lastChoice = null;
            while (matcher.find()) {
                lastChoice = Integer.parseInt(matcher.group(1));
            }
            if (lastChoice == null) {
                throw new IOException(name + " returned no JSON choice: " + abbreviate(response));
            }
            return lastChoice;
        }

        private static String abbreviate(CharSequence value) {
            String text = value.toString().replace('\n', ' ').trim();
            return text.length() <= 500 ? text : text.substring(text.length() - 500);
        }
    }

    public static void main(String[] args) {
        int size = 6;
        String p1Type = "gemini";
        String p2Type = "codex";
        String matchId = "llm-match-" + System.currentTimeMillis();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--size", "-s" -> size = Integer.parseInt(requireValue(args, ++i, "--size"));
                case "--p1" -> p1Type = requireValue(args, ++i, "--p1").toLowerCase(Locale.ROOT);
                case "--p2" -> p2Type = requireValue(args, ++i, "--p2").toLowerCase(Locale.ROOT);
                case "--match-id" -> matchId = requireValue(args, ++i, "--match-id");
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }

        if (size != 6 && size != 12) {
            throw new IllegalArgumentException("Only board sizes 6 and 12 are supported");
        }
        ModelPlayer p1 = createPlayer(p1Type);
        ModelPlayer p2 = createPlayer(p2Type);
        runGame(size, matchId, p1, p2);
    }

    private static void runGame(int size, String matchId, ModelPlayer p1, ModelPlayer p2) {
        GameState state = new GameState(size);
        state.getP1().setName(p1.name());
        state.getP2().setName(p2.name());
        Instant startedAt = Instant.now();

        System.out.printf("START|match=%s|size=%d|p1=%s|p2=%s%n", matchId, size, p1.name(), p2.name());
        int turns = 0;
        Integer forcedWinnerId = null;
        String failure = null;

        while (!state.isOver() && turns < MAX_TURNS) {
            turns++;
            int playerId = state.getCurrentTurn();
            ModelPlayer current = playerId == 1 ? p1 : p2;
            List<Candidate> candidates = enumerateCandidates(state);
            if (candidates.isEmpty()) {
                forcedWinnerId = playerId == 1 ? 2 : 1;
                failure = current.name() + " has no legal turn";
                break;
            }

            int choice;
            try {
                String prompt = buildPrompt(matchId, turns, state, current, candidates);
                choice = current.choose(prompt);
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                forcedWinnerId = playerId == 1 ? 2 : 1;
                failure = e.getMessage();
                break;
            }

            if (choice < 0 || choice >= candidates.size()) {
                forcedWinnerId = playerId == 1 ? 2 : 1;
                failure = current.name() + " returned illegal choice " + choice;
                break;
            }

            Candidate selected = candidates.get(choice);
            if (!executeCandidate(state, playerId, selected)) {
                forcedWinnerId = playerId == 1 ? 2 : 1;
                failure = current.name() + " selected a candidate rejected by the referee";
                break;
            }
            System.out.printf(
                    "TURN|match=%s|turn=%d|model=%s|player=%d|choice=%d|path=%s|lock=%s|at=%d,%d%n",
                    matchId, turns, current.name(), playerId, selected.id(), pathText(selected.path()),
                    selected.lockDirection(), selected.row(), selected.col());
        }

        String winner;
        int winnerId;
        if (forcedWinnerId != null) {
            winnerId = forcedWinnerId;
            winner = winnerId == 1 ? p1.name() : p2.name();
        } else if (state.getWinner() == 1) {
            winnerId = 1;
            winner = p1.name();
        } else if (state.getWinner() == 2) {
            winnerId = 2;
            winner = p2.name();
        } else {
            winnerId = 0;
            winner = "DRAW";
        }
        long seconds = Duration.between(startedAt, Instant.now()).toSeconds();
        System.out.printf(
                "RESULT|match=%s|size=%d|p1=%s|p2=%s|winner=%s|winnerPlayer=P%d|territory=%d:%d|turns=%d|seconds=%d|failure=%s%n",
                matchId, size, p1.name(), p2.name(), winner, winnerId, state.getP1Territory(), state.getP2Territory(),
                turns, seconds, failure == null ? "none" : failure.replace('|', '/'));
    }

    private static ModelPlayer createPlayer(String type) {
        return switch (type) {
            case "gemini" -> new CommandModelPlayer(
                    "Gemini-3.8-Flash-High",
                    List.of("agy", "--model", "gemini-3.8-flash-high", "--effort", "high", "--prompt"));
            case "codex" -> new CommandModelPlayer(
                    "Codex-GPT-5.6-Sol",
                    List.of("codex", "--ask-for-approval", "never", "exec", "--ephemeral", "--sandbox",
                            "read-only", "--skip-git-repo-check", "-C", "/tmp"));
            default -> throw new IllegalArgumentException("Unsupported external model: " + type);
        };
    }

    private static List<Candidate> enumerateCandidates(GameState state) {
        Board board = state.getBoard();
        PlayerState me = state.getCurrentPlayer();
        PlayerState opponent = state.getOpponentPlayer();
        int maxSteps = me.getEnergy();

        Map<Long, List<Direction>> paths = new HashMap<>();
        Queue<int[]> queue = new ArrayDeque<>();
        paths.put(key(me.getR(), me.getC()), List.of());
        queue.add(new int[]{me.getR(), me.getC(), 0});

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            if (current[2] >= maxSteps) {
                continue;
            }
            List<Direction> path = paths.get(key(current[0], current[1]));
            for (Direction direction : Direction.values()) {
                if (!board.isConnected(current[0], current[1], direction)) {
                    continue;
                }
                int nextRow = current[0] + direction.getDr();
                int nextCol = current[1] + direction.getDc();
                if (nextRow == opponent.getR() && nextCol == opponent.getC()) {
                    continue;
                }
                long key = key(nextRow, nextCol);
                if (!paths.containsKey(key)) {
                    List<Direction> nextPath = new ArrayList<>(path);
                    nextPath.add(direction);
                    paths.put(key, List.copyOf(nextPath));
                    queue.add(new int[]{nextRow, nextCol, current[2] + 1});
                }
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        for (Map.Entry<Long, List<Direction>> entry : paths.entrySet()) {
            int row = row(entry.getKey());
            int col = col(entry.getKey());
            for (Direction lockDirection : Direction.values()) {
                if (board.isConnected(row, col, lockDirection)) {
                    candidates.add(new Candidate(
                            candidates.size(), entry.getValue(), lockDirection, row, col));
                }
            }
        }
        return candidates;
    }

    private static String buildPrompt(
            String matchId,
            int turn,
            GameState state,
            ModelPlayer current,
            List<Candidate> candidates) {
        PlayerState me = state.getCurrentPlayer();
        PlayerState opponent = state.getOpponentPlayer();
        StringBuilder prompt = new StringBuilder(16_384);
        prompt.append("你正在真实参加 COGame，不是写代码或模拟口头战报。只选择本回合一个合法候选。\n")
                .append("目标：锁边后让双方不再连通，并使自己的连通领地格数更多。\n")
                .append("每个候选格式 id:path>终点/lock方向；path 是本回合移动序列，- 表示不移动。\n")
                .append("你只能输出一行 JSON，禁止解释、Markdown 或工具调用：{\"choice\":整数}\n")
                .append("match=").append(matchId)
                .append(" turn=").append(turn)
                .append(" model=").append(current.name())
                .append(" board=").append(state.getRows()).append('x').append(state.getCols())
                .append(" me=P").append(me.getId()).append('@').append(me.getR()).append(',').append(me.getC())
                .append(" energy=").append(me.getEnergy())
                .append(" opponent=P").append(opponent.getId()).append('@').append(opponent.getR()).append(',')
                .append(opponent.getC()).append('\n')
                .append("已封锁边（坐标-方向-归属，N 为中立）：")
                .append(lockedEdges(state.getBoard())).append('\n')
                .append("合法候选：\n");
        for (Candidate candidate : candidates) {
            prompt.append(candidate.id()).append(':')
                    .append(pathText(candidate.path())).append('>')
                    .append(candidate.row()).append(',').append(candidate.col())
                    .append("/lock").append(shortDirection(candidate.lockDirection())).append(' ');
        }
        return prompt.toString();
    }

    private static String lockedEdges(Board board) {
        StringBuilder value = new StringBuilder();
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                appendLockedEdge(value, board, row, col, Direction.RIGHT);
                appendLockedEdge(value, board, row, col, Direction.DOWN);
            }
        }
        return value.length() == 0 ? "none" : value.toString();
    }

    private static void appendLockedEdge(StringBuilder value, Board board, int row, int col, Direction direction) {
        int nextRow = row + direction.getDr();
        int nextCol = col + direction.getDc();
        if (!board.isValidCoord(nextRow, nextCol) || board.isConnected(row, col, direction)) {
            return;
        }
        if (value.length() > 0) {
            value.append(' ');
        }
        int owner = board.getEdgeLocker(row, col, direction);
        value.append(row).append(',').append(col).append('-').append(shortDirection(direction)).append('-')
                .append(owner == 3 ? "N" : owner);
    }

    private static boolean executeCandidate(GameState state, int playerId, Candidate candidate) {
        for (Direction direction : candidate.path()) {
            if (!GameEngine.executeAction(state, playerId, GameAction.changeDirMove(direction))) {
                return false;
            }
        }
        PlayerState player = state.getCurrentPlayer();
        while (player.getDirection() != candidate.lockDirection()) {
            if (!GameEngine.executeAction(state, playerId, GameAction.rotate())) {
                return false;
            }
        }
        return GameEngine.executeAction(state, playerId, GameAction.lock());
    }

    private static String pathText(List<Direction> path) {
        if (path.isEmpty()) {
            return "-";
        }
        StringBuilder value = new StringBuilder(path.size());
        for (Direction direction : path) {
            value.append(shortDirection(direction));
        }
        return value.toString();
    }

    private static char shortDirection(Direction direction) {
        return switch (direction) {
            case UP -> 'U';
            case RIGHT -> 'R';
            case DOWN -> 'D';
            case LEFT -> 'L';
        };
    }

    private static long key(int row, int col) {
        return ((long) row << 32) | (col & 0xffffffffL);
    }

    private static int row(long key) {
        return (int) (key >> 32);
    }

    private static int col(long key) {
        return (int) key;
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static void printHelp() {
        System.out.println("Usage: llm-battle-cli --size <6|12> --p1 <gemini|codex> --p2 <gemini|codex> --match-id <id>");
    }
}
