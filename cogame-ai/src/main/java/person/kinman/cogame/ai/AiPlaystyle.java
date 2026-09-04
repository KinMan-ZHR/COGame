package person.kinman.cogame.ai;

/**
 * AI 流派与性格枚举：供玩家选择不同风格的对手对战
 */
public enum AiPlaystyle {

    ANTIGRAVITY(
            "🧠 莫衡 · 控局大师 (策略控盘流)",
            "大局观领地 · 战术节能蓄力 · 擅长大盘长途反包抄",
            "莫衡 (控盘大师)",
            "#38bdf8",
            "深谋远虑的控局者。注重全局 Voronoi 势力划分与战术节能，不轻易挥霍步数，擅长在关键回合发动大转移反切对手领地！"
    ),

    CODEX(
            "⚔️ 荆刺 · 破局猎手 (极限压迫流)",
            "近战刺刀肉搏 · 贴身撕咬 · 强力封死通道出口",
            "荆刺 (破局猎手)",
            "#f59e0b",
            "极具攻击性的突袭者。嗅觉敏锐，每回合直扑对手身位贴脸缠斗，疯狂封锁逃生通道，压迫感极强！"
    ),

    CLASSIC(
            "🛡️ 玄岳 · 铁壁守卫 (均衡图论流)",
            "动态最短路阻断 · 攻守兼备 · 稳健推进防守",
            "玄岳 (铁壁守卫)",
            "#10b981",
            "经典原版端脑守望者。攻防一体，注重自身绝对安全，绝不误入死局禁手，擅长滴水不漏的最短路阻截。"
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
