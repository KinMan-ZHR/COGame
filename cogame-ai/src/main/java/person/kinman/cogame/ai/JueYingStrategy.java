package person.kinman.cogame.ai;

import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.Set;

/**
 * 绝影 · 贴身猎手 (极致贴身压迫流)：
 * 核心特征：曼哈顿极近身位刺刀战、步步紧逼扼杀对手出口、压缩逃生走廊窒息决战
 */
public class JueYingStrategy extends AbstractDeepLookaheadAi {

    public JueYingStrategy() {
        this(5);
    }

    public JueYingStrategy(int searchDepth) {
        super(searchDepth);
    }

    @Override
    public String getStrategistName() {
        return "绝影";
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

        // 1. 自身安全防御
        int myDegree = board.getOpenDirections(myR, myC).size();
        if (myDegree <= 1) return -5000.0;

        // 2. 对手出口窒息封锁 (核心亮点：若逼得对手仅剩1个出口，极大嘉奖)
        int oppDegree = board.getOpenDirections(oppR, oppC).size();
        double suffocation = switch (oppDegree) {
            case 1 -> 4000.0; // 对手只剩1个生天，极易困杀！
            case 2 -> 1200.0; // 对手进入狭窄走廊
            case 3 -> 100.0;
            default -> -200.0; // 对手空间过大，惩罚
        };

        // 3. 曼哈顿极近身贴身压制 (越近得分越高)
        int manhattan = Math.abs(myR - oppR) + Math.abs(myC - oppC);
        double proximity = -manhattan * 22.0;

        // 4. 辅助领地意识
        int voronoi = calculateVoronoi(board, myR, myC, oppR, oppC);

        return (voronoi * 15.0) + suffocation + proximity + (myDegree * 15.0);
    }
}
