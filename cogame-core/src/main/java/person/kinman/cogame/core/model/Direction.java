package person.kinman.cogame.core.model;

/**
 * 4向方位枚举：上、右、下、左
 */
public enum Direction {
    UP(0, "上", -1, 0),
    RIGHT(1, "右", 0, 1),
    DOWN(2, "下", 1, 0),
    LEFT(3, "左", 0, -1);

    private final int index;
    private final String name;
    private final int dr;
    private final int dc;

    Direction(int index, String name, int dr, int dc) {
        this.index = index;
        this.name = name;
        this.dr = dr;
        this.dc = dc;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public int getDr() {
        return dr;
    }

    public int getDc() {
        return dc;
    }

    /**
     * 顺时针旋转90度
     */
    public Direction clockwise() {
        return switch (this) {
            case UP -> RIGHT;
            case RIGHT -> DOWN;
            case DOWN -> LEFT;
            case LEFT -> UP;
        };
    }

    /**
     * 相反方向
     */
    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case RIGHT -> LEFT;
            case DOWN -> UP;
            case LEFT -> RIGHT;
        };
    }

    public static Direction fromIndex(int index) {
        for (Direction d : values()) {
            if (d.index == index) return d;
        }
        return UP;
    }
}
