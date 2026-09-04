package person.kinman.cogame.ai;

/**
 * AI 流派与性格枚举：供玩家选择不同风格的对手对战
 */
public enum AiPlaystyle {

    ANTIGRAVITY(
            "🧠 莫衡 · 控局大师",
            "全局领地划分 · 蓄力长途奔袭",
            "莫衡 (控盘大师)",
            "#38bdf8",
            "大局观控盘，战术蓄力后长途穿插反切"
    ),

    CODEX(
            "⚔️ 荆刺 · 破局猎手",
            "贴身压制缠斗 · 强攻封锁要道",
            "荆刺 (破局猎手)",
            "#f59e0b",
            "进攻性极强，紧贴身位封死逃生出口"
    ),

    CLASSIC(
            "🛡️ 玄岳 · 铁壁守卫",
            "通路阻截破坏 · 稳健防守反击",
            "玄岳 (铁壁守卫)",
            "#10b981",
            "经典稳健守门，步步为营截断最短连通"
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
