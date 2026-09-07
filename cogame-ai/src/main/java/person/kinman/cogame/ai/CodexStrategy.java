package person.kinman.cogame.ai;

/**
 * 兼容旧版本 CodexStrategy -> 映射至 绝影·贴身猎手
 */
public class CodexStrategy extends JueYingStrategy {
    public CodexStrategy() {
        super(5);
    }
    public CodexStrategy(int depth) {
        super(depth);
    }
}
