import javax.swing.*;
import java.awt.*;
import java.util.StringTokenizer;

/**
 * 间隔棋子游戏（代码行级1000-1200行）
 *36格，49点
 * 已完成的隔断任务：
 * 1. 玩家可以移动
 * 让玩家和一张图片绑定
 * 拥查剩该玩家的方向提示（之后可以考虑在右侧小标出当前玩家的头像和方向)
 * 2. 玩家、根据当前方向可以隔绝两格的通路
 * (注意要两个索引同时置nu11)
 * 3. 胜利的条件判断 个算连通格子数（用AStar的弱化版即可）统计 hasGone的个数
 * (难点）算未封锁的边的个数
 * 根据每个格点的索引引和/2
 *尚未来得及做的（先考虑联网再说）
 *附加功能：允许悔棋，即回到上一步操作
 * 直接做成Version2.(该种支持多种游戏模式和多玩家，隔断单向通过拓展：
 * 让不同玩家拥有不用不同颜色
 * 创建边类，让格点引用4个，边拥有颜色字段
 * 此时玩家的隔绝就把对应边要置成自己的颜色
 * 应当由Map类创建边
 * 胜利四条件B就是属满足默认颜色的个数，
 * 让格子自己也拥有一个颜色字段.
 * 玩家踩上去格子自动变成玩家的颜色 然后变回原来的颜色
 *
 */

public class GameWin extends JFrame {
    private Image offScreenImage = null;      //声明一个新的Image对象，即第二缓存
    private Graphics gOffScreen = null;
    public static final int ScreenWIDTH = 800;
    public static final int ScreenHEIGHT = 600;
    public static final int GameScreenWIDTH=600;
    public static final COGame game=COGame.getGame();
    public static GameWin gameWindow;
    public static void main(String[] args){
        gameWindow=new GameWin();
    }
    public GameWin(){
        this.setSize(ScreenWIDTH, ScreenHEIGHT);//设置窗体的宽和高
        //设置窗体关闭行为，当用户点击窗体的关闭图标时，结束程序
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        //设置居中
        this.setLocationRelativeTo(null);
        this.addKeyListener(game.worktop());
        this.setVisible(true);
    }
    public void paint(Graphics g) {
        if (offScreenImage == null) {
            offScreenImage = this.createImage(ScreenWIDTH, ScreenHEIGHT);
            //获得截取图片的画布
            gOffScreen = offScreenImage.getGraphics();
        }
        //需要画的图案，对gOffScreen操作
        //获取原画笔颜色
        Color originalColor=gOffScreen.getColor();
        //设置画笔颜色开始覆盖刷色（本游戏无需随时重刷，记得优化）
        gOffScreen.setColor(Color.GRAY);
        gOffScreen.fillRect(0, 0, GameScreenWIDTH, ScreenHEIGHT);
        gOffScreen.setColor(Color.PINK);
        gOffScreen.fillRect(GameScreenWIDTH, 0, ScreenWIDTH - GameScreenWIDTH, ScreenHEIGHT);
        gOffScreen.drawImage(game.getActor().getImage(),GameScreenWIDTH,20,118,160,null);
        gOffScreen.setColor(Color.BLUE);
        gOffScreen.setFont(new Font("宋体",Font.BOLD,35));
        gOffScreen.drawString(game.getActor().direction.getName(), GameScreenWIDTH+140,60);
        gOffScreen.setFont(new Font("楷体",Font.BOLD,30));
        gOffScreen.drawString(game.getActor().getName(), GameScreenWIDTH,220);
        game.getMap().drawMap(gOffScreen);
        for (Player player:Player.getPlayers()) {
            player.draw(gOffScreen);
        }
        if(game.isOver()){
            gOffScreen.setFont(new Font("楷体", Font.PLAIN,20));
            gOffScreen.setColor(Color.red);
            StringTokenizer result1 = new StringTokenizer(game.win1(), ";");
            gOffScreen.drawString(result1.nextToken(),GameScreenWIDTH,260);
            gOffScreen.drawString(result1.nextToken(),GameScreenWIDTH,290);
            gOffScreen.drawString(result1.nextToken(),GameScreenWIDTH,320);
            StringTokenizer result2 = new StringTokenizer(game.win2(), ";");
            gOffScreen.drawString(result2.nextToken(),GameScreenWIDTH,350);
            gOffScreen.drawString(result2.nextToken(),GameScreenWIDTH,380);
            gOffScreen.drawString(result2.nextToken(),GameScreenWIDTH,410);
        }
        gOffScreen.setColor(originalColor);
        g.drawImage(offScreenImage, 0, 0, null);
    }
}
