
import java.util.ArrayList;
public class AStar {
	/** 
   * 使用ArrayList数组作为“开启列表”和“关闭列表” 
   */
	ArrayList<Node> open = new ArrayList<Node>();
	ArrayList<Node> close = new ArrayList<Node>();
	/** 
   * 获取H值 
   * @param currentNode：当前节点 
   * @param endNode：终点 
   * @return 
   */
	public double getHValue(Node currentNode,Node endNode){
		return (Math.abs(currentNode.getX() - endNode.getX()) + Math.abs(currentNode.getY() - endNode.getY()))*10;
	}
	/** 
   * 获取G值 
   * @param currentNode：当前节点 
   * @return 
   */
	public double getGValue(Node currentNode){
		//当前的节点的爸爸不为空
		if(currentNode.getPNode()!=null){
			if(currentNode.getX()==currentNode.getPNode().getX()||currentNode.getY()==currentNode.getPNode().getY()){
				//判断当前节点与其父节点之间的位置关系（水平？对角线）
				//默认按邻近一格的范围搜寻
				return currentNode.getGValue()+10;
			}//斜向搜寻
			return currentNode.getGValue()+14;
		}
		return currentNode.getGValue();
	}
	/** 
   * 获取F值 ： G + H 
   * @param currentNode 
   * @return 
   */
	public double getFValue(Node currentNode){
		return currentNode.getGValue()+currentNode.getHValue();
	}
	/** 
   * 将选中节点周围的节点添加进“开启列表” 
   * @param node 
   * @param map 
   */
	public void inOpen(Node node,Map map){
		int x = node.getX();
		int y = node.getY();
		//向第四个方向默认拓展
		for (int i = 0;i<3;i++){
			for (int j = 0;j<3;j++){
				//判断条件为：节点为可到达的（即不是障碍物，不在关闭列表中），开启列表中不包含，不是选中节点 
				if(map.getMap()[x-1+i][y-1+j].isReachable()&&!open.contains(map.getMap()[x-1+i][y-1+j])&&!(x==(x-1+i)&&y==(y-1+j))){
					map.getMap()[x-1+i][y-1+j].setPNode(map.getMap()[x][y]);
					//将选中节点作为父节点 
					map.getMap()[x-1+i][y-1+j].setGValue(getGValue(map.getMap()[x-1+i][y-1+j]));
					map.getMap()[x-1+i][y-1+j].setHValue(getHValue(map.getMap()[x-1+i][y-1+j],map.getEndNode()));
					//再次证明getFValue方法应当放在点类里
					map.getMap()[x-1+i][y-1+j].setFValue(getFValue(map.getMap()[x-1+i][y-1+j]));
					//将其加入开启链表中
					open.add(map.getMap()[x-1+i][y-1+j]);
				}
			}
		}
	}
	/** 
   * 使用冒泡排序将开启列表中的节点按F值从小到大排序 
   * @param arr 
   */
	public void sort(ArrayList<Node> arr){
		for (int i = 0;i<arr.size()-1;i++){
			for (int j = i+1;j<arr.size();j++){
				if(arr.get(i).getFValue() > arr.get(j).getFValue()){
					//将大值存入暂时变量中
					Node tmp= arr.get(i);
					//对数组的第i，j位置进行替换
					arr.set(i, arr.get(j));
					arr.set(j, tmp);
					//最后就是从小到大排序的冒泡排序
				}
			}
		}
	}
	/** 
   * 将节点添加进”关闭列表“ 
   * @param node 
   * @param open 
   */
	public void inClose(Node node,ArrayList<Node> open){
		if(open.contains(node)){
			node.setReachable(false);
			//设置为不可达 并移除开启链表
			open.remove(node);
			//将其加入到close链表中
			close.add(node);
		}
	}
	//核心代码
	public void search(Map map){
		//对起点即起点周围的节点进行操作
		//这一步创建了起点对周围的开启链表
		inOpen(map.getMap()[map.getStartNode().getX()][map.getStartNode().getY()],map);
		//将起点丢入关闭链表中，并设为不可到达，写复杂了兄弟！
		close.add(map.getMap()[map.getStartNode().getX()][map.getStartNode().getY()]);
		map.getMap()[map.getStartNode().getX()][map.getStartNode().getY()].setReachable(false);
		//自己当自己的爸爸
		map.getMap()[map.getStartNode().getX()][map.getStartNode().getY()].setPNode(map.getMap()[map.getStartNode().getX()][map.getStartNode().getY()]);
		//对创建出来的开启链表做一个基本处理操作，根据f值排序
		sort(open);
		//重复步骤 直到开启列表中包含终点时，循环退出，即遍历到了
		do{
			//都对f值最小的进行扩展，再把这个点给关掉
			inOpen(open.get(0), map);
			inClose(open.get(0), open);
			//排序当前代价（之前没有用的拓展点还在）
			sort(open);
		}
		while(!open.contains(map.getMap()[map.getEndNode().getX()][map.getEndNode().getY()]));
		//知道开启列表中包含终点时，循环退出 
		inClose(map.getMap()[map.getEndNode().getX()][map.getEndNode().getY()], open);
		//将该点也丢入关闭链表里
		showPath(close,map);
	}
	/** 
   * 将路径标记出来 
   * @param arr 
   * @param map 
   */
	//丢进了关闭列表是说经过了的
	//open链表那人比喻的话就是see,hasGone==go
	//缺陷会有分支出现，之后应当对关闭链表进一步处理
	public void showPath(ArrayList<Node> arr,Map map) {
		if(arr.size()>0){
			Node node = new Node();
			node = map.getMap()[map.getEndNode().getX()][map.getEndNode().getY()];
			while(!(node.getX() ==map.getStartNode().getX()&&node.getY() ==map.getStartNode().getY())){
				node.getPNode().setValue("*");
				node = node.getPNode();
			}
//			<span style="white-space:pre">    </span>node = map.getMap()[map.getEndNode().getX()][map.getEndNode().getY()];
//			<span style="white-space:pre">    </span>while(!(node.getX() ==map.getStartNode().getX()&&node.getY() ==map.getStartNode().getY())){
//			<span style="white-space:pre">    </span>node.getPNode().setValue("*");
//			<span style="white-space:pre">    </span>node = node.getPNode();
//			<span style="white-space:pre">  </span>}
		}
		//<span style="white-space:pre">  </span>map.getMap()[map.getStartNode().getX()][map.getStartNode().getY()].setValue("A");
		map.getMap()[map.getStartNode().getX()][map.getStartNode().getY()].setValue("A");
	}
}