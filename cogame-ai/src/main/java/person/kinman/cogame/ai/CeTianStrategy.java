package person.kinman.cogame.ai;

import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.List;
import java.util.Set;

/**
 * 策天 · 盘枢国手 (大局观控盘流)：
 * 核心特征：全局 Voronoi 领地宏观切割、长线通路空间拉伸、战术蓄能绝杀、严密自保防封
 */
public class CeTianStrategy extends AbstractDeepLookaheadAi {

    public CeTianStrategy() {
        this(5);
    }

    public CeTianStrategy(int searchDepth) {
        super(searchDepth);
    }

    @Override
    public String getStrategistName() {
        return "策天";
    }

    @Override
    protected double evaluateLeaf(GameState state, int aiPlayerId, int stepsUsed) {
        Board board = state.getBoard();
        PlayerState me = state.getPlayer(aiPlayerId);
        PlayerState opp = state.getPlayer(aiPlayerId == 1 ? 2 : 1);

        int myR = me.getR();
        int myC = me.getC();
        int oppR = opp.getR();
        int oppC = opp.getC();

        boolean connected = GameEvaluator.hasPath(board, myR, myC, oppR, oppC);
        if (!connected) {
            Set<Long> myTerr = GameEvaluator.getConnectedComponent(board, myR, myC);
            Set<Long> oppTerr = GameEvaluator.getConnectedComponent(board, oppR, oppC);
            int diff = myTerr.size() - oppTerr.size();
            return diff > 0 ? (10000000.0 + diff * 1000.0) : (-10000000.0 + diff * 1000.0);
        }

        // 1. 自身安全防御：严厉禁止让自己落入单出口狭窄死角
        int myDegree = board.getOpenDirections(myR, myC).size();
        if (myDegree <= 1) return -5000.0;

        // 2. Voronoi 领地全局控制力
        int voronoi = calculateVoronoi(board, myR, myC, oppR, oppC);

        // 3. 最短连通路径拉伸 (迫使对手大迂回走位)
        List<int[]> path = GameEvaluator.findPath(board, myR, myC, oppR, oppC);
        int pathLen = path.isEmpty() ? 0 : path.size();

        // 4. 战术蓄能储备：非必要不狂冲，保留能量蓄力用于关键长线大穿插
        double energyReserve = me.getEnergy() * 5.0;

        // 5. 对手扩张压制
        int oppDegree = board.getOpenDirections(oppR, oppC).size();
        double oppConstraint = (4 - oppDegree) * 20.0;

        return (voronoi * 35.0) + (pathLen * 18.0) + energyReserve + oppConstraint + (myDegree * 25.0) - (stepsUsed * 4.0);
    }
}
