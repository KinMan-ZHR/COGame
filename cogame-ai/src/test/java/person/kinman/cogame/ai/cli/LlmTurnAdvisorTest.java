package person.kinman.cogame.ai.cli;

import org.junit.jupiter.api.Test;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmTurnAdvisorTest {

    @Test
    void identifiesImmediateWinningCut() {
        GameState state = new GameState(6);
        state.getP1().setR(1);
        state.getP1().setC(0);
        state.setTurnStartR(1);
        state.setTurnStartC(0);
        state.getP2().setR(0);
        state.getP2().setC(0);
        state.getBoard().lockEdge(0, 0, Direction.RIGHT, 2);

        LlmTurnAdvisor.Candidate candidate = findCandidate(
                LlmTurnAdvisor.enumerateCandidates(state), 1, 0, Direction.UP, "-");

        assertTrue(candidate.terminal());
        assertEquals(1, candidate.winner());
        assertEquals(35, candidate.myTerritory());
        assertEquals(1, candidate.opponentTerritory());
        assertEquals(LlmTurnAdvisor.Outcome.WIN, candidate.outcome());
    }

    @Test
    void identifiesSelfLockingLoss() {
        GameState state = new GameState(6);
        state.getBoard().lockEdge(0, 0, Direction.DOWN, 2);

        LlmTurnAdvisor.Candidate candidate = findCandidate(
                LlmTurnAdvisor.enumerateCandidates(state), 0, 0, Direction.RIGHT, "-");

        assertTrue(candidate.terminal());
        assertEquals(2, candidate.winner());
        assertEquals(1, candidate.myTerritory());
        assertEquals(35, candidate.opponentTerritory());
        assertEquals(LlmTurnAdvisor.Outcome.LOSS, candidate.outcome());
    }

    @Test
    void enumerationIsStableAndDoesNotMutateOriginalState() {
        GameState state = new GameState(6);
        state.getP1().setDirection(Direction.RIGHT);
        int originalEnergy = state.getP1().getEnergy();

        List<LlmTurnAdvisor.Candidate> first = LlmTurnAdvisor.enumerateCandidates(state);
        List<LlmTurnAdvisor.Candidate> second = LlmTurnAdvisor.enumerateCandidates(state);

        assertEquals(first, second);
        assertFalse(first.isEmpty());
        for (int index = 0; index < first.size(); index++) {
            assertEquals(index, first.get(index).id());
            if (index > 0) {
                assertTrue(compare(first.get(index - 1), first.get(index)) <= 0);
            }
        }
        assertEquals(0, state.getP1().getR());
        assertEquals(0, state.getP1().getC());
        assertEquals(Direction.RIGHT, state.getP1().getDirection());
        assertEquals(originalEnergy, state.getP1().getEnergy());
        assertEquals(1, state.getCurrentTurn());
        assertFalse(state.isOver());
        assertTrue(state.getBoard().isConnected(0, 0, Direction.RIGHT));
        assertTrue(state.getBoard().isConnected(0, 0, Direction.DOWN));
    }

    @Test
    void promptContainsRulesBoardHistoryAndCandidateConsequences() {
        GameState state = new GameState(6);
        state.getBoard().lockEdge(0, 0, Direction.DOWN, 2);
        List<LlmTurnAdvisor.Candidate> candidates = LlmTurnAdvisor.enumerateCandidates(state);

        String prompt = LlmTurnAdvisor.buildPrompt(
                "test-match", 3, state, "test-model", candidates,
                List.of("T1 P1 path=R at=0,1 lock=DOWN", "T2 P2 path=L at=5,4 lock=UP"));

        assertTrue(prompt.contains("移动距离不能超过当前能量"));
        assertTrue(prompt.contains("格数相同再比较领地内开放边数"));
        assertTrue(prompt.contains("+---+---+"));
        assertTrue(prompt.contains("T1 P1 path=R"));
        assertTrue(prompt.contains("outcome=LOSS"));
        assertTrue(prompt.contains("area=1:35"));
        assertTrue(prompt.contains("{\"choice\":整数}"));
    }

    @Test
    void parsesLastStructuredChoiceAndPreservesIllegalNegativeChoice() throws IOException {
        assertEquals(7, LlmBattleCli.parseChoice("banner\n{\"choice\":2}\nfinal {\"choice\":7}"));
        assertEquals(-1, LlmBattleCli.parseChoice("{\"choice\":-1}"));
        assertThrows(IOException.class, () -> LlmBattleCli.parseChoice("I choose candidate seven"));
    }

    @Test
    void retriesFormattingAndAuthenticationFailuresButNotTimeouts() {
        assertTrue(LlmBattleCli.isRetryable(new IOException("authentication failed")));
        assertTrue(LlmBattleCli.isRetryable(new IOException("Model returned no JSON choice")));
        assertFalse(LlmBattleCli.isRetryable(new IOException("response timed out after 600 seconds")));
    }

    private static LlmTurnAdvisor.Candidate findCandidate(
            List<LlmTurnAdvisor.Candidate> candidates,
            int row,
            int col,
            Direction lockDirection,
            String path) {
        return candidates.stream()
                .filter(candidate -> candidate.row() == row)
                .filter(candidate -> candidate.col() == col)
                .filter(candidate -> candidate.lockDirection() == lockDirection)
                .filter(candidate -> LlmTurnAdvisor.pathText(candidate.path()).equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static int compare(LlmTurnAdvisor.Candidate left, LlmTurnAdvisor.Candidate right) {
        int result = Integer.compare(left.path().size(), right.path().size());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(left.row(), right.row());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(left.col(), right.col());
        if (result != 0) {
            return result;
        }
        return Integer.compare(left.lockDirection().getIndex(), right.lockDirection().getIndex());
    }
}
