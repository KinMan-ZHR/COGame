package person.kinman.cogame.ai;

/**
 * AI 流派与性格枚举 (v2.6 双国手战略棋手架构)
 */
public enum AiPlaystyle {

    CE_TIAN(
            "🧠 策天 · 盘枢国手",
            "全局领地切割 · 虚实蓄力长策",
            "策天 (盘枢国手)",
            "#38bdf8",
            "大局观控盘，擅长宏观划分全图势力范围，深算步数步步为营，战术蓄力后长途穿插反切绝杀"
    ),

    JUE_YING(
            "⚔️ 绝影 · 贴身猎手",
            "近身刺刀缠斗 · 极限封锁窒息",
            "绝影 (贴身猎手)",
            "#f59e0b",
            "进攻性极强，曼哈顿紧贴对手身位，层层封死所有出口与逃生通道，窒息式贴身死斗"
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
        return createStrategy(5);
    }

    public AiStrategy createStrategy(int searchDepth) {
        return switch (this) {
            case CE_TIAN -> new CeTianStrategy(searchDepth);
            case JUE_YING -> new JueYingStrategy(searchDepth);
        };
    }
}
