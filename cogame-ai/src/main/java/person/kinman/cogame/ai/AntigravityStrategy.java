package person.kinman.cogame.ai;

/**
 * 兼容旧版本 AntigravityStrategy -> 映射至 策天·盘枢国手
 */
public class AntigravityStrategy extends CeTianStrategy {
    public AntigravityStrategy() {
        super(5);
    }
    public AntigravityStrategy(int depth) {
        super(depth);
    }
}
