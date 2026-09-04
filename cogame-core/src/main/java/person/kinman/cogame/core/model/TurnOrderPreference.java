package person.kinman.cogame.core.model;

import java.util.Random;

/**
 * 分先偏好枚举与仲裁器
 */
public enum TurnOrderPreference {
    FIRST("FIRST", "执先 (先手 P1)"),
    SECOND("SECOND", "执后 (后手 P2)"),
    RANDOM("RANDOM", "随机分先");

    private final String code;
    private final String displayName;

    TurnOrderPreference(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TurnOrderPreference fromCode(String code) {
        if (code == null) return RANDOM;
        for (TurnOrderPreference p : values()) {
            if (p.code.equalsIgnoreCase(code.trim()) || p.name().equalsIgnoreCase(code.trim())) {
                return p;
            }
        }
        return RANDOM;
    }

    /**
     * 分先仲裁算法：
     * 根据玩家1 (房主) 与玩家2 (挑战者) 的偏好，决定谁执先手 (获得 P1 席位)。
     *
     * @param p1Pref 房主意愿
     * @param p2Pref 挑战者意愿
     * @param rng 随机数生成器
     * @return 1 表示房主执先 (P1)，2 表示挑战者执先 (P1)
     */
    public static int resolveFirstPlayer(TurnOrderPreference p1Pref, TurnOrderPreference p2Pref, Random rng) {
        if (p1Pref == null) p1Pref = RANDOM;
        if (p2Pref == null) p2Pref = RANDOM;
        if (rng == null) rng = new Random();

        // 1. 双方意愿一致：同选先手或同选后手 -> 公平掷骰 50/50 随机分配
        if (p1Pref == FIRST && p2Pref == FIRST) {
            return rng.nextBoolean() ? 1 : 2;
        }
        if (p1Pref == SECOND && p2Pref == SECOND) {
            return rng.nextBoolean() ? 1 : 2;
        }

        // 2. 双方意愿互补：一方先手，一方后手 -> 各得其所
        if (p1Pref == FIRST && p2Pref == SECOND) {
            return 1;
        }
        if (p1Pref == SECOND && p2Pref == FIRST) {
            return 2;
        }

        // 3. 一方随机，另一方有明确意向 -> 满足明确意向方
        if (p1Pref == FIRST && p2Pref == RANDOM) {
            return 1;
        }
        if (p1Pref == SECOND && p2Pref == RANDOM) {
            return 2;
        }
        if (p1Pref == RANDOM && p2Pref == FIRST) {
            return 2;
        }
        if (p1Pref == RANDOM && p2Pref == SECOND) {
            return 1;
        }

        // 4. 双方均随机 -> 50/50 掷骰
        return rng.nextBoolean() ? 1 : 2;
    }

    /**
     * 生成对局分先仲裁结果的人类友好提示文案
     */
    public static String getResolutionDescription(String p1Name, TurnOrderPreference p1Pref,
                                                 String p2Name, TurnOrderPreference p2Pref,
                                                 int chosenFirstPlayer) {
        if (p1Pref == null) p1Pref = RANDOM;
        if (p2Pref == null) p2Pref = RANDOM;
        String firstPlayerName = (chosenFirstPlayer == 1) ? p1Name : p2Name;
        String secondPlayerName = (chosenFirstPlayer == 1) ? p2Name : p1Name;

        if (p1Pref == FIRST && p2Pref == FIRST) {
            return String.format("双方均选择【执先】，系统掷骰随机裁定：由 [%s] 执先 (P1)！", firstPlayerName);
        }
        if (p1Pref == SECOND && p2Pref == SECOND) {
            return String.format("双方均选择【执后】，系统掷骰随机裁定：由 [%s] 执先 (P1)！", firstPlayerName);
        }
        if ((p1Pref == FIRST && p2Pref == SECOND) || (p1Pref == SECOND && p2Pref == FIRST)) {
            return String.format("双方分先达成一致：由 [%s] 执先 (P1)，[%s] 执后 (P2)！", firstPlayerName, secondPlayerName);
        }
        if (p1Pref == RANDOM && p2Pref == RANDOM) {
            return String.format("双方均选择【随机分先】，系统掷骰裁定：由 [%s] 执先 (P1)！", firstPlayerName);
        }
        return String.format("根据分先偏好分配：由 [%s] 执先 (P1)，[%s] 执后 (P2)！", firstPlayerName, secondPlayerName);
    }
}
