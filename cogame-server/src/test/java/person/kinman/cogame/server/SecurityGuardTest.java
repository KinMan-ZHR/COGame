package person.kinman.cogame.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import person.kinman.cogame.server.security.SecurityGuard;

public class SecurityGuardTest {

    @Test
    public void testInputSanitization() {
        // Player name validations
        Assertions.assertTrue(SecurityGuard.isValidPlayerName("Alice"));
        Assertions.assertTrue(SecurityGuard.isValidPlayerName("KinMan_99"));
        Assertions.assertTrue(SecurityGuard.isValidPlayerName("端脑探索者"));

        // Invalid: too short, too long, or injection chars
        Assertions.assertFalse(SecurityGuard.isValidPlayerName("a"));
        Assertions.assertFalse(SecurityGuard.isValidPlayerName("very_long_player_name_exceeding_sixteen"));
        Assertions.assertFalse(SecurityGuard.isValidPlayerName("<script>alert(1)</script>"));
        Assertions.assertFalse(SecurityGuard.isValidPlayerName("test' OR 1=1 --"));

        // Room ID validations
        Assertions.assertTrue(SecurityGuard.isValidRoomId("1001"));
        Assertions.assertTrue(SecurityGuard.isValidRoomId("room-alpha-12"));
        Assertions.assertFalse(SecurityGuard.isValidRoomId(""));
        Assertions.assertFalse(SecurityGuard.isValidRoomId("room#123!"));
        Assertions.assertFalse(SecurityGuard.isValidRoomId("room with spaces"));
    }

    @Test
    public void testBanMechanism() {
        SecurityGuard guard = new SecurityGuard();
        String attackerIp = "198.51.100.23";

        Assertions.assertFalse(guard.isIpBanned(attackerIp));

        // Trigger 3 violations
        guard.recordViolation(attackerIp, "恶意探测 1");
        guard.recordViolation(attackerIp, "恶意探测 2");
        guard.recordViolation(attackerIp, "恶意探测 3");

        // Should now be banned
        Assertions.assertTrue(guard.isIpBanned(attackerIp), "IP 违规达阈值后应被列入黑名单");
    }
}
