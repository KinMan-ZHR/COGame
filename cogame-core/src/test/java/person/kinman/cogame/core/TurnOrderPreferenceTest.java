package person.kinman.cogame.core.model;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class TurnOrderPreferenceTest {

    @Test
    public void testComplementaryPreferences() {
        assertEquals(1, TurnOrderPreference.resolveFirstPlayer(TurnOrderPreference.FIRST, TurnOrderPreference.SECOND, new Random(42)));
        assertEquals(2, TurnOrderPreference.resolveFirstPlayer(TurnOrderPreference.SECOND, TurnOrderPreference.FIRST, new Random(42)));
    }

    @Test
    public void testBothWantFirst() {
        Random rng = new Random(42);
        boolean got1 = false;
        boolean got2 = false;
        for (int i = 0; i < 100; i++) {
            int winner = TurnOrderPreference.resolveFirstPlayer(TurnOrderPreference.FIRST, TurnOrderPreference.FIRST, rng);
            if (winner == 1) got1 = true;
            if (winner == 2) got2 = true;
        }
        assertTrue(got1 && got2, "Both want FIRST should result in 50/50 coin toss");
    }

    @Test
    public void testBothWantSecond() {
        Random rng = new Random(42);
        boolean got1 = false;
        boolean got2 = false;
        for (int i = 0; i < 100; i++) {
            int winner = TurnOrderPreference.resolveFirstPlayer(TurnOrderPreference.SECOND, TurnOrderPreference.SECOND, rng);
            if (winner == 1) got1 = true;
            if (winner == 2) got2 = true;
        }
        assertTrue(got1 && got2, "Both want SECOND should result in 50/50 coin toss");
    }

    @Test
    public void testRandomAndSpecific() {
        Random rng = new Random(42);
        assertEquals(2, TurnOrderPreference.resolveFirstPlayer(TurnOrderPreference.RANDOM, TurnOrderPreference.FIRST, rng));
        assertEquals(1, TurnOrderPreference.resolveFirstPlayer(TurnOrderPreference.RANDOM, TurnOrderPreference.SECOND, rng));
        assertEquals(1, TurnOrderPreference.resolveFirstPlayer(TurnOrderPreference.FIRST, TurnOrderPreference.RANDOM, rng));
        assertEquals(2, TurnOrderPreference.resolveFirstPlayer(TurnOrderPreference.SECOND, TurnOrderPreference.RANDOM, rng));
    }

    @Test
    public void testResolutionDescription() {
        String descBothFirst = TurnOrderPreference.getResolutionDescription("Alice", TurnOrderPreference.FIRST, "Bob", TurnOrderPreference.FIRST, 1);
        assertTrue(descBothFirst.contains("双方均选择【执先】"));
        assertTrue(descBothFirst.contains("Alice"));

        String descAgree = TurnOrderPreference.getResolutionDescription("Alice", TurnOrderPreference.FIRST, "Bob", TurnOrderPreference.SECOND, 1);
        assertTrue(descAgree.contains("双方分先达成一致"));
    }
}
