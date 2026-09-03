package person.kinman.cogame.core.model;

/**
 * 玩家状态信息
 */
public class PlayerState {
    private int id;
    private String name;
    private int r;
    private int c;
    private Direction direction;
    private int energy;

    public PlayerState() {}

    public PlayerState(int id, String name, int r, int c, Direction direction) {
        this.id = id;
        this.name = name;
        this.r = r;
        this.c = c;
        this.direction = direction;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public PlayerState copy() {
        PlayerState copy = new PlayerState(this.id, this.name, this.r, this.c, this.direction);
        copy.energy = this.energy;
        return copy;
    }
}
