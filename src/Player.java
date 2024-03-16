import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * @author KinMan谨漫
 * 自己管多个自己的成功实践（单例模式的变种）
 */
public class Player {
    private Image image;
    private static final int playerMaxNum=12;
    private static final ArrayList<Player>players=new ArrayList<>();
    /**
     * 编号
     */
    private final int serialNumber;
    private String name;
    //这里直接按得相关格子进行的位移所以没有用到相关数据
    private final int displacement = 1;
    protected Direction direction;
    protected GameGrid location;

    /**
     * 成员内部类
     */
    public enum Direction {
        U("上", 0), RU("右上", 1), R("右", 2), RD("右下", 3), D("下", 4), LD("左下", 5), L("左", 6), LU("左上", 7);
        // 成员变量
        private String name;
        private int index;

        // 构造方法
        Direction(String name, int index) {
            this.name = name;
            this.index = index;
        }

        // 普通方法
        public static String getName(int index) {
            for (Direction c : Direction.values()) {
                if (c.getIndex() == index) {
                    return c.name;
                }
            }
            return null;
        }

        // get set 方法
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }
    /**
     * 私有构造方法(一般性)
     */
    private Player(int index,String pictureName){
        this.serialNumber =index+1;
        direction=Direction.U;
        try {
            image= ImageIO.read(Objects.requireNonNull(Player.class.getClassLoader().getResource(pictureName)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * @param serialNumber 想要创建的玩家的编号
     * @param pictureName 加载对应的图片，在resource文件夹下就直接使用名字
     */
    public static void createPlayer(int serialNumber,String pictureName){
        players.add(serialNumber-1,new Player(serialNumber-1,pictureName));
    }

    public void setImage(String pictureName) {
        try {
            this.image = ImageIO.read(Objects.requireNonNull(Player.class.getClassLoader().getResource(pictureName)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * @return 所有参加的选手
     */
    public static ArrayList<Player> getPlayers(){
        return players;
    }
    /**
     * 自己所占位置为location
     */
    public GameGrid getLocation() {
        return location;
    }

    public void setLocation(GameGrid location) {
        this.location = location;
    }
    public void move() {
        switch (direction) {
            case D -> {
                if (location.down != null) location = location.down;
            }
            case R -> {
                if (location.right != null) location = location.right;
            }
            case L -> {
                if (location.left != null) location = location.left;
            }
            case U -> {
                if (location.up != null) location = location.up;
            }
        }
    }
    /**
     * 顺时针旋转
     */
    public void clockwiseRotation() {
        switch (direction) {
            case D -> direction=Direction.L;
            case R -> direction=Direction.D;
            case L -> direction=Direction.U;
            case U -> direction=Direction.R;
        }

    }
    /**
     * 控制方向移动
     * 根据键盘指令行动者按方向移动
     * wasd上下左右键控制移动
     */
    public void controlDirectMove(int e) {
        switch (e){
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> {
                direction=Direction.L;
                move();
            }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
                direction=Direction.D;
                move();
            }
            case KeyEvent.VK_W, KeyEvent.VK_UP -> {
                direction=Direction.U;
                move();
            }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> {
                direction=Direction.R;
                move();
            }
        }


    }
    /**
     * 封锁
     */
    public boolean lock(){
        switch (direction){
            case D -> {
                if (location.down != null) {
                    location.down.removeLinkList(location.down.up);
                    location.removeLinkList(location.down);
                    location.down.up = null;
                    location.down=null;
                    return true;
                }
                return false;
            }
            case R -> {
                if (location.right != null) {
                    location.right.removeLinkList(location.right.left);
                    location.removeLinkList(location.right);
                     location.right.left=null;
                     location.right=null;
                    return true;
                }
                return false;
            }
            case L -> {
                if (location.left != null) {
                    location.left.removeLinkList(location.left.right);
                    location.removeLinkList(location.left);
                     location.left.right=null;
                     location.left=null;
                    return true;
                }
                return false;
            }
            case U -> {
                if (location.up != null) {
                    location.up.removeLinkList(location.up.down);
                    location.removeLinkList(location.up);
                    location.up.down=null;
                    location.up=null;
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    /**
     * 返回选手
     * @param serialNumber 索引，所以1号选手的索引为0
     * @return 如果索引超界会返回一个null值
     */
    public static Player getPlayer(int serialNumber){
        return players.get(serialNumber-1);
    }
    /**
     * 基准信息函数
     * @return 返回一个编号
     */
    public int getSerialNumber() {
        return serialNumber;
    }
    /**
     * 基准信息函数
     * @return 返回一个名字
     */
    public String getName() {
        return name;
    }
    /**
     * 基准信息函数
     * @return 返回一个相片
     */
    public Image getImage() {
        return image;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 不同玩家加载不同的资源图片但请注意同一张图片请只加载一次
     *
     * @param g
     */
    public void draw(Graphics g) {
        g.drawImage(image,location.getX()-GameGrid.GridWIDTH/3, location.getY() -GameGrid.GridHEIGHT/2,
                236*GameGrid.GridWIDTH/320,GameGrid.GridHEIGHT,null);
        g.setColor(Color.cyan);
        switch (direction){
            case L -> g.fillOval(location.getX()-GameGrid.GridWIDTH/2, location.getY(), 10,10);
            case D -> g.fillOval(location.getX(), location.getY()+GameGrid.GridHEIGHT/4, 10,10);
            case R -> g.fillOval(location.getX()+GameGrid.GridWIDTH/4, location.getY(), 10,10);
            case U -> g.fillOval(location.getX(), location.getY()-GameGrid.GridHEIGHT/2, 10,10);
        }
    }
}
