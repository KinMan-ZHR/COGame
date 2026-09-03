package person.kinman.cogame.ai;

/**
 * AI 流派与性格枚举：供玩家选择不同风格的对手对战
 */
public enum AiPlaystyle {

    ANTIGRAVITY(
            "🧠 Antigravity · 策略控盘流",
            "大局观领地 · 战术节能蓄力 · 擅长大盘长途反包抄",
            "Antigravity AI",
            "#38bdf8",
            "注重全局 Voronoi 领地划分与节能储备。不轻易浪费步数，倾向于蓄满能量后打出惊天大转移反切对手领地！"
    ),

    CODEX(
            "⚔️ Codex · 极限压迫流",
            "近战刺刀肉搏 · 贴身撕咬 · 强力封死通道出口",
            "Codex AI",
            "#f59e0b",
            "激进侵略型打法。每回合嗅觉敏锐，直奔对手身位贴脸肉搏，疯狂压缩玩家的逃生出口，带来强烈的窒息感！"
    ),

    CLASSIC(
            "🛡️ 端脑守卫 · 均衡图论流",
            "动态最短路阻断 · 攻守兼备 · 稳健推进防守",
            "端脑守卫 AI",
            "#10b981",
            "经典端脑标准防守图论算法。攻防一体，重视自身绝对安全，绝不误入死局禁手，擅长稳扎稳打的最短路隔断。"
    );

    private final String displayName;
    private final String tagline;
    private final String playerName;
    private final String colorHex;
    private final String detailDescription;

    AiPlaystyle(String displayName, String tagline, String playerName, String colorHex, String detailDescription) {
        this.displayName = displayName;
        this.tagline = tagline;
        this.playerName = playerName;
        this.colorHex = colorHex;
        this.detailDescription = detailDescription;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTagline() {
        return tagline;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getDetailDescription() {
        return detailDescription;
    }

    public AiStrategy createStrategy() {
        return switch (this) {
            case ANTIGRAVITY -> new AntigravityStrategy();
            case CODEX -> new CodexStrategy();
            case CLASSIC -> new HeuristicAi(3);
        };
    }
}
