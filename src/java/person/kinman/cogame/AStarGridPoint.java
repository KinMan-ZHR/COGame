package person.kinman.cogame;

import java.awt.*;
import java.util.ArrayList;

/**
 * Single Path， Single Destination
 * 根据屏幕距离判断的代价
 * 需要使用addLink来增加关联点；
 * 需要使用removeLink来移除关联点
 */
public abstract class AStarGridPoint {
    /**
     * 编号
     */
    private final int serialNum;
    private final String name;
    /**
     * 从哪里来
     */
    private AStarGridPoint pre;
    /**
     * where to go
     */
    private static AStarGridPoint destination;
    /**
     * where could go可以向哪里去,即和谁有关联
     */
    private final ArrayList<AStarGridPoint> link =new ArrayList<>();
    /**
     * 来过了吗
     */
    private boolean isGone=false;
    /**
     * 是最终路线吗
     */
    private boolean isFinalPath=false;
    /**
     * G值 当前代价，从起点到这里花费的代价--真值
     */
    private double G=0;
    /**
     * H值 当前代价，从这里到终点花费的代价--预估值
     */
    private double H=0;
    /**
     * 屏幕上的x坐标
     */
    private int x;
    /**
     * 屏幕上的y坐标
     */
    private int y;
    public static int GridWIDTH = 70;
    public static int GridHEIGHT = 70;
    static class MySearchMethod {
        /**
         * 使用ArrayList数组作为“开启列表”和“关闭列表”
         */
        static final ArrayList<AStarGridPoint> hasSeen = new ArrayList<>();
        static final ArrayList<AStarGridPoint> hasGone = new ArrayList<>();
        private static void seeEBySelf(AStarGridPoint currentE){
            //判断该点是否具有连接点
            if(currentE.getLinkPoints().size()>0){
                //遍历所有的点
                for (AStarGridPoint linkE : currentE.getLinkPoints()){
                    //因为之前的点已经把相应点放入hasSeen里了和已经走过了，所以不在hasSeen的
                    //其实可以使用hashSet避免重复
                    if(!hasSeen.contains(linkE)&&!linkE.isGone){
                        //未放入hasSeen里的
                    linkE.setPreNode(currentE);//认识一下爸爸
                    //计算一下相关值
                    linkE.setG(linkE.calculateG());
                    linkE.setH(linkE.calculateHBL());
                    hasSeen.add(linkE);
                }
                }
            }
            //展开了
            currentE.setGone(true);
            hasGone.add(currentE);
            hasSeen.remove(currentE);
        }


        /**
         * 使用冒泡排序将开启列表中的节点按F值从小到大排序
         */
        private static void sortF(){
            for (int i = 0; i< MySearchMethod.hasSeen.size()-1; i++){
                for (int j = i+1; j< MySearchMethod.hasSeen.size(); j++){
                    if(MySearchMethod.hasSeen.get(i).getFValue() > MySearchMethod.hasSeen.get(j).getFValue()){
                        AStarGridPoint tmp  = MySearchMethod.hasSeen.get(i);
                        MySearchMethod.hasSeen.set(i, MySearchMethod.hasSeen.get(j));
                        MySearchMethod.hasSeen.set(j, tmp);
                    }
                }
            }
        }
        /**
         * 使用冒泡排序将开启列表中的节点按H值从小到大排序
         */
        private static void sortH(){
            for (int i = 0; i< MySearchMethod.hasSeen.size()-1; i++){
                for (int j = i+1; j< MySearchMethod.hasSeen.size(); j++){
                    if(MySearchMethod.hasSeen.get(i).getHValue() > MySearchMethod.hasSeen.get(j).getHValue()){
                        AStarGridPoint tmp  = MySearchMethod.hasSeen.get(i);
                        MySearchMethod.hasSeen.set(i, MySearchMethod.hasSeen.get(j));
                        MySearchMethod.hasSeen.set(j, tmp);
                    }
                }
            }
        }
        /**
         * 使用冒泡排序将开启列表中的节点按G值从小到大排序
         */
        private static void sortG(){
            for (int i = 0; i< MySearchMethod.hasSeen.size()-1; i++){
                for (int j = i+1; j< MySearchMethod.hasSeen.size(); j++){
                    if(MySearchMethod.hasSeen.get(i).getGValue() > MySearchMethod.hasSeen.get(j).getGValue()){
                        AStarGridPoint tmp  = MySearchMethod.hasSeen.get(i);
                        MySearchMethod.hasSeen.set(i, MySearchMethod.hasSeen.get(j));
                        MySearchMethod.hasSeen.set(j, tmp);
                    }
                }
            }
        }

         /**
          * AStar搜寻路径，若想要展示路径请使用方法showPath
          * @param start 出发点
          * @param destination 结束点
          * @return 是否找到
          */
        public static boolean AStarSearch(AStarGridPoint start, AStarGridPoint destination){
            //初始化点格信息以保证搜寻路线的有效性
            initialPath();
            AStarGridPoint.setDestination(destination);
            //将起点放入展开队列
            hasSeen.add(start);
            //按第一处展开
            while (hasSeen.size()!=0&&hasSeen.get(0)!=destination) {
                seeEBySelf(hasSeen.get(0));//展开
                sortF();
            }
            return getConclusion(destination);
        }
        /**
          * 浪费了G值
          * 贪婪搜寻路径，若想要展示路径请使用方法showPath
          * @param start 出发点
          * @param destination 结束点
          * @return 是否找到
          */
        public static boolean GreedSearch(AStarGridPoint start, AStarGridPoint destination){
            //初始化点格信息以保证搜寻路线的有效性
            initialPath();
            AStarGridPoint.setDestination(destination);
            //将起点放入展开队列
            hasSeen.add(start);
            //按第一处展开
            while (hasSeen.size()!=0&&hasSeen.get(0)!=destination) {
                seeEBySelf(hasSeen.get(0));//展开
                sortH();
            }
            return getConclusion(destination);
        }
        /**
          * 浪费了H值
          * 广度优先搜寻路径，若想要展示路径请使用方法showPath
          * @param start 出发点
          * @param destination 结束点
          * @return 是否找到
          */
        public static boolean BFSearch(AStarGridPoint start, AStarGridPoint destination){
            //初始化点格信息以保证搜寻路线的有效性
            initialPath();
            AStarGridPoint.setDestination(destination);
            //将起点放入展开队列
            hasSeen.add(start);
            //按第一处展开
            while (hasSeen.size()!=0&&hasSeen.get(0)!=destination) {
                seeEBySelf(hasSeen.get(0));//展开
                sortG();
            }
            return getConclusion(destination);
        }
        /**
         * 给出搜索结论
         * @param destination 结束点
         * @return 是否找到
         */
        private static boolean getConclusion(AStarGridPoint destination) {
            if(hasSeen.size()==0){
                return false;
            }
            if(hasSeen.get(0)==destination){
                destination.setGone(true);
                hasGone.add(destination);
                hasSeen.remove(destination);
                return true;
            }

            return false;
        }
        /**
         * 浪费了G,H值
          * 遍历地图所有点搜寻路径，若想要展示路径请使用方法showPath
          * 可以通过该方法得到与起点连通的的所有点遍历hasGone即可
          * @param start 出发点
          * @param destination 结束点
          * @return 是否找到
          */
        public static boolean coverAllSearch(AStarGridPoint start, AStarGridPoint destination){
            //初始化点格信息以保证搜寻路线的有效性
            initialPath();
            AStarGridPoint.setDestination(destination);
            //将起点放入展开队列
            hasSeen.add(start);
            //按第一处展开找到所有的点都已被展开
            while (hasSeen.size()!=0) {
                seeEBySelf(hasSeen.get(0));
            }
            if(hasGone.contains(destination)){
                return true;
            }
            return false;

        }

        public static void showPath(AStarGridPoint start, AStarGridPoint destination) {
            if(hasGone.size()>0){
                AStarGridPoint e=destination;
                //从后向前寻历
                while(e!=start){
                    e.setFinalPath(true);
                    e=e.getPreNode();//向前移动位置
                }
            }
            //对起点作标记
            start.setFinalPath(true);
        }
        private static void initialPath(){
            hasSeen.clear();
            for (AStarGridPoint e:hasGone){
                e.setGone(false);
                e.setFinalPath(false);
            }
            hasGone.clear();
        }


    }
    public AStarGridPoint(int serialNum, String name){
        this.serialNum = serialNum;
        this.name=name;
    }
    public AStarGridPoint(int locationX, int locationY, int serialNum, String name){
        this.serialNum = serialNum;
        this.name=name;
        this.x=locationX;
        this.y=locationY;
    }

    /**
     * 增加该点可以通向的地方（单程路线）
     */
    public void addLinkList(AStarGridPoint nextE){
       if(nextE!=null)link.add(nextE);
    }
    /**
     * 移除该点可以通向的地方（单程路线）
     */
    public void removeLinkList(AStarGridPoint nextE){
        link.remove(nextE);
    }
    /**
     * 设置格点的长宽,注意所有有的格点都一样大，若想修改其重新更改该类，建议直接复制粘贴重写一个类
     */
    public static void setBounds(int gridWIDTH ,int gridHEIGHT){
        GridWIDTH= gridWIDTH;
        GridHEIGHT= gridHEIGHT;
    }

    /**
     * 基准信息函数
     * @return 返回一个编号
     */
    public int getSerialNum() {
        return serialNum;
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
     * @return 返回屏幕上的x坐标
     */
    public int getX() {
        return x;
    }
    /**
     * 基准信息函数
     * @return 返回屏幕上的y坐标
     */
    public int getY() {
        return y;
    }
    /**
     * 设计关联（单向链表）（可以有多个点到达这里，但实际上只有一个点来到这里）
     */
    public AStarGridPoint getPreNode() {
        return pre;
    }
    public void setPreNode(AStarGridPoint pre) {
        this.pre = pre;
    }
    public double getGValue() {
        return G;
    }
    public double getHValue() { return H; }
    public void setG(double g) {
        G = g;
    }
    public void setH(double h) {
        H = h;
    }
    public double getFValue() {return G + H;}

    /**
     * 静态方法，若要想要拥有不同的目的地，请更改MyAStar类中对destination的设置
     * 并移除static修饰
     * @param destination 静态的目的，即所有格点的最终目标
     */
    public static void setDestination(AStarGridPoint destination) {
        AStarGridPoint.destination = destination;
    }

    /**
     * 获取G值 当前代价，从起点到这里花费的代价--真值
     */
    public double calculateG() {
        if (pre!=null){//累计代价加移动代价
            return pre.G+calculateHBL(this,pre);
        }
        //没有来源就应当是起点
        return 0;
    }
    /**
     * 获取H值 启发式预估值--欧拉算法直线距离
     */
    public double calculateHL(){
        if(destination==null){
            throw new RuntimeException("未指定目的地却想要计算H值");
        }
        return Math.sqrt(Math.pow(x - destination.x,2) + Math.pow(y - destination.y,2));
    }
    /**
     * 获取H值 启发式预估值--曼哈顿算法折线
     */
    public double calculateHBL(){
        if(destination==null){
            throw new RuntimeException("未指定目的地却想要计算H值");
        }
        return Math.abs(x - destination.x) + Math.abs(y - destination.y);
    }
    /**
     * 获取H值 启发式预估值--曼哈顿算法折线(任意两点间)
     */
    public double calculateHBL(AStarGridPoint e1, AStarGridPoint e2){
        return Math.abs(e1.x - e2.x) + Math.abs(e1.y - e2.y);
    }
    public  ArrayList<AStarGridPoint> getLinkPoints() {
        return link;
    }
    public boolean isGone() {
        return isGone;
    }
    public boolean isFinalPath() {
        return isFinalPath;
    }
    public void setFinalPath(boolean finalPath) {
        isFinalPath = finalPath;
    }
    public void setGone(boolean gone) {
        isGone = gone;
    }

    /**
     * 如何去画一个格点，记住已有宽高和位置
     * 如若想要在显示经过的路径，请使用isFinalPath区分画图方式
     * @param g 在哪里画
     */
    public abstract void drawGridPoint(Graphics g);

}



