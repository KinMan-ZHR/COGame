package person.kinman.cogame.original;

import java.awt.*;

public class GameMap {
    // 节点数组表示格子的排列的位置关系
    private final GameGrid[][] aStarGridPoints;
    private static final int rows=6;
    private static final int cols=6;
    //起点
    GameGrid startNode;
    //终点
    GameGrid endNode;
    private static volatile GameMap map=null;
    /**
     * 构造map
     */
    private GameMap(){
        GameGrid.GridWIDTH=80;
        GameGrid.GridHEIGHT=80;
        //创造并连接
        aStarGridPoints=  new GameGrid[rows][cols];
        /*
          构筑所有的点，不能边构筑边连接，如果存储的是null,他不会因为null的赋值而改变
         */
    for (int i=0;i<rows;i++){
        for (int j=0;j<cols;j++){
            aStarGridPoints[i][j]=new GameGrid(100+j*GameGrid.GridWIDTH,100+i*GameGrid.GridHEIGHT,i*cols+1+j,"("+i+","+j+")");
        }
    }
      resetMap();
    }
    /**
     * 重置地图资源连接所有的点
     */
    public  void resetMap(){
        for (int i=0;i<rows;i++){
            for (int j=0;j<cols;j++){
                if(i>0)
                    aStarGridPoints[i][j].up=aStarGridPoints[i-1][j];
                if(j>0)
                    aStarGridPoints[i][j].left= aStarGridPoints[i][j-1];
                if(i<rows-1)
                    aStarGridPoints[i][j].down=aStarGridPoints[i+1][j];
                if(j<cols-1)
                    aStarGridPoints[i][j].right=aStarGridPoints[i][j+1];
                aStarGridPoints[i][j].getLinkPoints().clear();
                aStarGridPoints[i][j].addLink();
            }

        }
    }
    /**
     * 获得地图
     * @return 返回一个地图
     */
    public static GameMap getMap() {
        if (map==null){
            synchronized (GameMap.class){
                if(map==null){
                    map=new GameMap();
                }
            }
        }
        return map;
    }
    /**
     * 获得地图的点阵
     * @return GameGrid的二维数组地址
     */
    public GameGrid[][] getaStarGridPoints() {
        return aStarGridPoints;
    }
    /**
     * 绘制地图
     */
    public void drawMap(Graphics g){
        for (AStarGridPoint[] es:aStarGridPoints) {
            for (AStarGridPoint e:es) {
                e.drawGridPoint(g);
            }
        }
    }
}
