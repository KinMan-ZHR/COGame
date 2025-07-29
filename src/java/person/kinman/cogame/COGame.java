package person.kinman.cogame;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
/**
 * 单例(内部维护了一个自己)创建获取都是get,
 * 自营多例（内部维护了一个自己的集合，并且所有成员共享该集合见wav和player类）创建用create
 * 普通多例创建用new
 */
public class COGame {
    private static volatile COGame game;
    private static final GameMap map= GameMap.getMap();//用单例模式是真的香
    private static Player actor;//当前的操作者
    private static boolean over=false;

    /**
     * 初始化游戏
     */
    private COGame() {
        loadPlayers();
        loadGameMusic();
    }
    /**
     * 获取游戏资源
     */
    public static COGame getGame() {
        if (game==null){
            synchronized (COGame.class){
                if (game==null)game=new COGame();
            }
        }
        return game;
    }

    /**
     *@return 返回地图
     */
    public GameMap getMap() {
        return map;
    }
    /**
     * 创建音乐
     */
    private static void loadGameMusic(){
        WavMusic.createMusic("Brand X Music - Get Bent.wav");
       WavMusic.createMusic("imagine dragonslil wayne - believer.wav");
       WavMusic.createMusic("Varien-Future Funk.wav");
    }
    /**
     * 游戏玩家的载入
     */
    private static void loadPlayers() {
        Player.createPlayer(1,"player1.jpg");
        Player.getPlayer(1).setLocation(map.getaStarGridPoints()[0][0]);
        Player.getPlayer(1).setName("谨漫KinMan");
        Player.createPlayer(2,"player2.jpg");
        Player.getPlayer(2).setLocation(map.getaStarGridPoints()[5][5]);
        Player.getPlayer(2).setName("无名之辈");
        actor = Player.getPlayer(1);
    }
    /**
     * 获取当前操作者信息
     */
    public Player getActor() {
        return actor;
    }
    /**
     * 结束的判断条件
     * @return 返回游戏是否结束
     */
    private  boolean over() {
        map.startNode = Player.getPlayer(1).location;
        map.endNode = Player.getPlayer(2).location;
        over=!AStarGridPoint.MySearchMethod.GreedSearch(map.startNode, map.endNode);
        return over;
    }

    public  boolean isOver() {
        return over;
    }

    /**
     * 胜利的判断条件:格子数
     */
    public String win1() {
           AStarGridPoint.MySearchMethod.coverAllSearch(map.startNode,map.endNode);
          int result1=AStarGridPoint.MySearchMethod.hasGone.size();
           AStarGridPoint.MySearchMethod.coverAllSearch(map.endNode, map.startNode);
          int result2=AStarGridPoint.MySearchMethod.hasGone.size();
          return  judge(result1, result2)+";"+
                  Player.getPlayer(1).getName()+":"+ result1+"格;"+
                  Player.getPlayer(2).getName()+":" + result2+"格;";
    }
    /**
     * 胜利的判断条件
     */
    public String win2() {
        int result1=0,result2=0;
           AStarGridPoint.MySearchMethod.coverAllSearch(map.startNode, map.endNode);
           for (AStarGridPoint gameGrid:AStarGridPoint.MySearchMethod.hasGone)
           {
               result1+=gameGrid.getLinkPoints().size();
           }
           AStarGridPoint.MySearchMethod.coverAllSearch(map.endNode, map.startNode);
           for (AStarGridPoint gameGrid:AStarGridPoint.MySearchMethod.hasGone)
           {
               result2+=gameGrid.getLinkPoints().size();
           }

           return  judge(result1, result2)+";"+
                   Player.getPlayer(1).getName()+":"+ result1/2+"个隔断;"+
                   Player.getPlayer(2).getName()+":" + result2/2+"个隔断;";

    }

    private String judge(int result1, int result2) {
        if(result1==result2)
            return "平局";
        if(result1>result2)
           return Player.getPlayer(1).getName()+"胜利";
        return Player.getPlayer(2).getName()+"胜利";
    }

    /**
     * 能根据player的序号来切换行动者
     */
    private void shiftPlayer() {
        if (actor.getSerialNumber() < Player.getPlayers().size())
            actor = Player.getPlayer(actor.getSerialNumber()+1);
        else actor = Player.getPlayer(1);
    }
    /**
     * 可以做到一人一个切换音乐
     */
    private void shiftMusic() {
       if (actor==Player.getPlayer(1)){
           WavMusic.getOnWavMusic("Varien-Future Funk.wav");
       }else {
           WavMusic.getOnWavMusic("imagine dragonslil wayne - believer.wav");
       }
    }
    /**
     * 控制台，与玩家交互的部分
     */
    public  KeyAdapter worktop() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode()==KeyEvent.VK_ADD){
                    over=false;
                    map.resetMap();
                }
                if (over){
                    return;
                }
                switch (e.getKeyCode()) {

                    case KeyEvent.VK_P -> {
                        over();
                        AStarGridPoint.MySearchMethod.showPath(map.startNode, map.endNode);
                    }
                    case KeyEvent.VK_T -> shiftPlayer();
                    case KeyEvent.VK_R -> actor.clockwiseRotation();
                    case KeyEvent.VK_SPACE -> actor.move();
                    case KeyEvent.VK_L -> {
                        //判断封锁是否有效
                        if(actor.lock()){
                            shiftPlayer();
                            shiftMusic();
                        }
                        //游戏结束
                        if(over()){
                            WavMusic.getOnWavMusic("Brand X Music - Get Bent.wav");
                        }
                    }
                    //根据键盘指令行动者按方向移动
                    default -> actor.controlDirectMove(e.getKeyCode());
                }
                //有变动就画图
                GameWin.gameWindow.repaint();
            }
        };

    }

}