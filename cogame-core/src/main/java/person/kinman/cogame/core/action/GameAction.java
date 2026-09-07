package person.kinman.cogame.core.action;

import person.kinman.cogame.core.model.Direction;

/**
 * 玩家操作动作
 */
public class GameAction {
    public enum Type {
        MOVE,               // 朝当前方向前进一格
        ROTATE,             // 顺时针旋转90度
        CHANGE_DIR_MOVE,    // 改变方向并移动（如按WASD）
        LOCK,               // 封锁当前方向的边
        RESET               // 重置棋局
    }

    private Type type;
    private Direction direction; // 用于 CHANGE_DIR_MOVE 指定新方向

    public GameAction() {}

    public GameAction(Type type) {
        this.type = type;
    }

    public GameAction(Type type, Direction direction) {
        this.type = type;
        this.direction = direction;
    }

    public static GameAction move() {
        return new GameAction(Type.MOVE);
    }

    public static GameAction rotate() {
        return new GameAction(Type.ROTATE);
    }

    public static GameAction changeDirMove(Direction dir) {
        return new GameAction(Type.CHANGE_DIR_MOVE, dir);
    }

    public static GameAction lock() {
        return new GameAction(Type.LOCK);
    }

    public static GameAction lock(Direction dir) {
        return new GameAction(Type.LOCK, dir);
    }

    public static GameAction reset() {
        return new GameAction(Type.RESET);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
