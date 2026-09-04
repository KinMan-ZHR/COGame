package person.kinman.cogame.ai.cli;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEngine;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Builds deterministic legal turns and explains their consequences to an external model. */
public final class LlmTurnAdvisor {
    public enum Outcome {
        WIN, LOSS, DRAW, PRESSURE, FRAGILE, NORMAL
    }

    public record Candidate(
            int id,
            List<Direction> path,
            Direction lockDirection,
            int row,
            int col,
            boolean terminal,
            int winner,
            int myTerritory,
            int opponentTerritory,
            int myEdges,
            int opponentEdges,
            int distance,
            int myDegree,
            int opponentDegree,
            Outcome outcome) {
    }

    private record RawCandidate(List<Direction> path, Direction lockDirection, int row, int col) {
    }

    private LlmTurnAdvisor() {
    }

    public static List<Candidate> enumerateCandidates(GameState state) {
        List<RawCandidate> rawCandidates = enumerateRawCandidates(state);
        rawCandidates.sort(Comparator
                .comparingInt((RawCandidate candidate) -> candidate.path().size())
                .thenComparingInt(RawCandidate::row)
                .thenComparingInt(RawCandidate::col)
                .thenComparingInt(candidate -> candidate.lockDirection().getIndex()));

        List<Candidate> candidates = new ArrayList<>(rawCandidates.size());
        for (int id = 0; id < rawCandidates.size(); id++) {
            candidates.add(analyze(state, id, rawCandidates.get(id)));
        }
        return List.copyOf(candidates);
    }

    public static String buildPrompt(
            String matchId,
            int turn,
            GameState state,
            String modelName,
            List<Candidate> candidates,
            List<String> history) {
        PlayerState me = state.getCurrentPlayer();
        PlayerState opponent = state.getOpponentPlayer();
        StringBuilder prompt = new StringBuilder(32_768);
        prompt.append("你是 COGame 的真实参赛模型。裁判已经枚举并预演所有合法回合，请选择一个候选。\n")
                .append("规则：棋盘格之间由边连接；每回合可沿开放边移动，不能进入对手格；移动距离不能超过当前能量；")
                .append("随后必须封锁落点旁的一条开放边。锁边不可恢复并结束回合，移动距离会消耗能量，下次轮到该玩家时恢复能量。")
                .append("双方不再连通时立即结束：先比较各自连通领地格数，格数相同再比较领地内开放边数。\n")
                .append("决策纪律：优先选择 outcome=WIN；严禁选择 outcome=LOSS；FRAGILE 表示自己的出口只剩 1 条；")
                .append("PRESSURE 表示对手出口只剩 1 条。area、edges、distance、degree 都是裁判预演后的客观结果。\n")
                .append("只输出一行 JSON，禁止解释、Markdown 或工具调用：{\"choice\":整数}\n")
                .append("match=").append(matchId)
                .append(" turn=").append(turn)
                .append(" model=").append(modelName)
                .append(" board=").append(state.getRows()).append('x').append(state.getCols())
                .append(" regen=").append(state.getEnergyRegen())
                .append(" maxEnergy=").append(state.getMaxEnergy()).append('\n')
                .append("me=P").append(me.getId()).append('@').append(me.getR()).append(',').append(me.getC())
                .append(" facing=").append(shortDirection(me.getDirection()))
                .append(" energy=").append(me.getEnergy())
                .append(" currentTurnStepsUsed=").append(state.getCurrentTurnSteps()).append('\n')
                .append("opponent=P").append(opponent.getId()).append('@').append(opponent.getR()).append(',').append(opponent.getC())
                .append(" facing=").append(shortDirection(opponent.getDirection()))
                .append(" energy=").append(opponent.getEnergy()).append('\n')
                .append("boardLayout:\n")
                .append(asciiBoard(state))
                .append("recentHistory:\n");
        if (history.isEmpty()) {
            prompt.append("- none\n");
        } else {
            for (String entry : history) {
                prompt.append("- ").append(entry).append('\n');
            }
        }
        prompt.append("legalCandidates:\n");
        for (Candidate candidate : candidates) {
            prompt.append(String.format(
                    "- id=%d path=%s lock=%s target=%d,%d outcome=%s area=%d:%d edges=%d:%d dist=%d degree=%d:%d%n",
                    candidate.id(),
                    pathText(candidate.path()),
                    candidate.lockDirection(),
                    candidate.row(),
                    candidate.col(),
                    candidate.outcome(),
                    candidate.myTerritory(),
                    candidate.opponentTerritory(),
                    candidate.myEdges(),
                    candidate.opponentEdges(),
                    candidate.distance(),
                    candidate.myDegree(),
                    candidate.opponentDegree()));
        }
        return prompt.toString();
    }

    public static boolean executeCandidate(GameState state, int playerId, Candidate candidate) {
        for (Direction direction : candidate.path()) {
            if (!GameEngine.executeAction(state, playerId, GameAction.changeDirMove(direction))) {
                return false;
            }
        }
        PlayerState player = state.getPlayer(playerId);
        while (player.getDirection() != candidate.lockDirection()) {
            if (!GameEngine.executeAction(state, playerId, GameAction.rotate())) {
                return false;
            }
        }
        return GameEngine.executeAction(state, playerId, GameAction.lock());
    }

    public static List<GameAction> buildCandidateActions(PlayerState player, Candidate candidate) {
        List<GameAction> actions = new ArrayList<>();
        Direction currentDir = player.getDirection();
        for (Direction direction : candidate.path()) {
            actions.add(GameAction.changeDirMove(direction));
            currentDir = direction;
        }
        while (currentDir != candidate.lockDirection()) {
            actions.add(GameAction.rotate());
            currentDir = currentDir.clockwise();
        }
        actions.add(GameAction.lock());
        return actions;
    }

    public static String pathText(List<Direction> path) {
        if (path.isEmpty()) {
            return "-";
        }
        StringBuilder value = new StringBuilder(path.size());
        for (Direction direction : path) {
            value.append(shortDirection(direction));
        }
        return value.toString();
    }

    private static List<RawCandidate> enumerateRawCandidates(GameState state) {
        Board board = state.getBoard();
        PlayerState me = state.getCurrentPlayer();
        PlayerState opponent = state.getOpponentPlayer();
        Map<Long, List<Direction>> paths = new LinkedHashMap<>();
        Queue<int[]> queue = new ArrayDeque<>();
        paths.put(GameEvaluator.encode(me.getR(), me.getC()), List.of());
        queue.add(new int[]{me.getR(), me.getC(), 0});

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            if (current[2] >= me.getEnergy()) {
                continue;
            }
            List<Direction> path = paths.get(GameEvaluator.encode(current[0], current[1]));
            for (Direction direction : Direction.values()) {
                if (!board.isConnected(current[0], current[1], direction)) {
                    continue;
                }
                int nextRow = current[0] + direction.getDr();
                int nextCol = current[1] + direction.getDc();
                if (nextRow == opponent.getR() && nextCol == opponent.getC()) {
                    continue;
                }
                long nextKey = GameEvaluator.encode(nextRow, nextCol);
                if (!paths.containsKey(nextKey)) {
                    List<Direction> nextPath = new ArrayList<>(path);
                    nextPath.add(direction);
                    paths.put(nextKey, List.copyOf(nextPath));
                    queue.add(new int[]{nextRow, nextCol, current[2] + 1});
                }
            }
        }

        List<RawCandidate> candidates = new ArrayList<>();
        for (Map.Entry<Long, List<Direction>> entry : paths.entrySet()) {
            int row = GameEvaluator.decodeR(entry.getKey());
            int col = GameEvaluator.decodeC(entry.getKey());
            for (Direction lockDirection : Direction.values()) {
                if (board.isConnected(row, col, lockDirection)) {
                    candidates.add(new RawCandidate(entry.getValue(), lockDirection, row, col));
                }
            }
        }
        return candidates;
    }

    private static Candidate analyze(GameState original, int id, RawCandidate raw) {
        int playerId = original.getCurrentTurn();
        int opponentId = playerId == 1 ? 2 : 1;
        GameState simulated = original.copy();
        Candidate executable = new Candidate(
                id, raw.path(), raw.lockDirection(), raw.row(), raw.col(), false, 0,
                0, 0, 0, 0, 0, 0, 0, Outcome.NORMAL);
        if (!executeCandidate(simulated, playerId, executable)) {
            throw new IllegalStateException("Referee rejected enumerated candidate " + id);
        }

        Board board = simulated.getBoard();
        PlayerState me = simulated.getPlayer(playerId);
        PlayerState opponent = simulated.getPlayer(opponentId);
        Set<Long> myComponent = GameEvaluator.getConnectedComponent(board, me.getR(), me.getC());
        Set<Long> opponentComponent = GameEvaluator.getConnectedComponent(board, opponent.getR(), opponent.getC());
        int distance = simulated.isOver()
                ? -1
                : Math.max(0, GameEvaluator.findPath(
                        board, me.getR(), me.getC(), opponent.getR(), opponent.getC()).size() - 1);
        int myDegree = board.getOpenDirections(me.getR(), me.getC()).size();
        int opponentDegree = board.getOpenDirections(opponent.getR(), opponent.getC()).size();

        return new Candidate(
                id, raw.path(), raw.lockDirection(), raw.row(), raw.col(), simulated.isOver(),
                simulated.getWinner(), myComponent.size(), opponentComponent.size(),
                GameEvaluator.countConnectedEdges(board, myComponent),
                GameEvaluator.countConnectedEdges(board, opponentComponent), distance, myDegree, opponentDegree,
                classifyOutcome(simulated, playerId, myDegree, opponentDegree));
    }

    private static Outcome classifyOutcome(GameState state, int playerId, int myDegree, int opponentDegree) {
        if (state.isOver()) {
            if (state.getWinner() == playerId) {
                return Outcome.WIN;
            }
            if (state.getWinner() == 3) {
                return Outcome.DRAW;
            }
            return Outcome.LOSS;
        }
        if (myDegree <= 1) {
            return Outcome.FRAGILE;
        }
        if (opponentDegree <= 1) {
            return Outcome.PRESSURE;
        }
        return Outcome.NORMAL;
    }

    private static String asciiBoard(GameState state) {
        Board board = state.getBoard();
        StringBuilder value = new StringBuilder();
        appendHorizontalBoundary(value, board, 0, true);
        for (int row = 0; row < board.getRows(); row++) {
            value.append('|');
            for (int col = 0; col < board.getCols(); col++) {
                value.append(cellText(state, row, col));
                if (col == board.getCols() - 1) {
                    value.append('|');
                } else if (board.isConnected(row, col, Direction.RIGHT)) {
                    value.append(' ');
                } else {
                    value.append(ownerSymbol(board.getEdgeLocker(row, col, Direction.RIGHT)));
                }
            }
            value.append('\n');
            appendHorizontalBoundary(value, board, row, row == board.getRows() - 1);
        }
        return value.toString();
    }

    private static void appendHorizontalBoundary(StringBuilder value, Board board, int row, boolean outer) {
        value.append('+');
        for (int col = 0; col < board.getCols(); col++) {
            if (outer) {
                value.append("---");
            } else if (board.isConnected(row, col, Direction.DOWN)) {
                value.append("   ");
            } else {
                char owner = ownerSymbol(board.getEdgeLocker(row, col, Direction.DOWN));
                value.append(owner).append(owner).append(owner);
            }
            value.append('+');
        }
        value.append('\n');
    }

    private static String cellText(GameState state, int row, int col) {
        if (state.getP1().getR() == row && state.getP1().getC() == col) {
            return " 1 ";
        }
        if (state.getP2().getR() == row && state.getP2().getC() == col) {
            return " 2 ";
        }
        return " . ";
    }

    private static char ownerSymbol(int owner) {
        return switch (owner) {
            case 1 -> '1';
            case 2 -> '2';
            case 3 -> 'N';
            default -> '#';
        };
    }

    private static char shortDirection(Direction direction) {
        return switch (direction) {
            case UP -> 'U';
            case RIGHT -> 'R';
            case DOWN -> 'D';
            case LEFT -> 'L';
        };
    }
}
