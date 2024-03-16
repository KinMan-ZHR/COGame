import java.awt.*;

public class GameGrid extends AStarGridPoint{
    GameGrid up;
    GameGrid down;
    GameGrid right;
    GameGrid left;
    public GameGrid(int index, String name) {
        super(index, name);
    }
    public GameGrid(int locationX, int locationY, int index, String name) {
        super(locationX, locationY, index, name);
    }

    /**
     * 如何去画一个格点，记住已有宽高和位置
     * 如若想要在显示经过的路径，请使用isFinalPath区分画图方式
     *
     * @param g 在哪里画
     */
    @Override
    public void drawGridPoint(Graphics g) {
        //选取底色
     if(isFinalPath()){
         g.setColor(Color.blue);

     }else {
         g.setColor(Color.black);
     }
     //铺底
        g.fillRect(getX()-GridWIDTH/2,getY()-GridHEIGHT/2,GridWIDTH,GridHEIGHT);
     //写编号
        g.setColor(Color.GRAY);
        g.setFont(new Font("宋体", Font.BOLD, 25));
        g.drawString(""+ getSerialNum(), getX()-10, getY()+10);
     //画边框

         drawBounds(g,getX()-GridWIDTH/2,getY()-GridHEIGHT/2,GridWIDTH,GridHEIGHT);

    }
    public void addLink(){
        addLinkList(up);
        addLinkList(right);
        addLinkList(down);
        addLinkList(left);
    }
    public void drawBounds(Graphics g,int x, int y, int width, int height) {
        Color warnColor= Color.RED;
        Color safeColor= Color.GREEN;
        if ((width < 0) || (height < 0)) {
            return;
        }
        //没有宽度或高度就画一根线
        if (height == 0 || width == 0) {
            g.drawLine(x, y, x + width, y + height);
        } else {
            if(null==up) g.setColor(warnColor);
            else g.setColor(safeColor);
            //g.drawLine(x, y, x + width - 1, y);//上方
            g.fillRect(x,y,width-2,2);
            if(null==right) g.setColor(warnColor);
            else g.setColor(safeColor);
            //g.drawLine(x + width, y, x + width, y + height - 1);
            g.fillRect(x+width-1,y,2,height-2);
            if(null==down) g.setColor(warnColor);
            else g.setColor(safeColor);
            //g.drawLine(x + width, y + height, x + 1, y + height);
            g.fillRect(x+2,y+height-1,width-2,2);
            if(null==left)g.setColor(warnColor);
            else g.setColor(safeColor);
            //g.drawLine(x, y + height, x, y + 1);
            g.fillRect(x,y+2,2,height-2);
        }
    }
}

