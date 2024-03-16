import java.awt.event.ActionEvent;

import java.awt.event.ActionListener;

import java.awt.event.KeyEvent;

import javax.swing.*;

import java.applet.*;

import java.net.*;

public class musicMenu extends JMenu{

JCheckBoxMenuItem [] MusicList;

ButtonGroup b;

public musicMenu()

{

// 音乐菜单

this.setText("音乐(M)");

this.setMnemonic (KeyEvent.VK_M);

/* URL file=getClass().getResource("music/爱的代价.mid");

AudioClip sound=java.applet.Applet.newAudioClip(file);

sound.play();*/

MainFrame.bar.add(this);

init();

addListener();

}

//添加监听器

private void addListener(){

MusicList[0].addActionListener(new ActionListener(){

public void actionPerformed(ActionEvent e) {

// TODO 自动生成方法存根

if(e.getSource()==MusicList[0]){

URL file=getClass().getResource("music/爱的代价.mid");

AudioClip sound=java.applet.Applet.newAudioClip(file);

sound.play();

}

}

});

MusicList[1].addActionListener(new ActionListener(){

public void actionPerformed(ActionEvent e) {

if(e.getSource()==MusicList[1]){

URL file=getClass().getResource("music/爱的就是你.mid");

AudioClip sound=java.applet.Applet.newAudioClip(file);

sound.play();

}

}

});

MusicList[2].addActionListener(new ActionListener(){

public void actionPerformed(ActionEvent e) {

if(e.getSource()==MusicList[2]){

URL file=getClass().getResource("music/当你孤单你会想起谁.mid");

AudioClip sound=java.applet.Applet.newAudioClip(file);

sound.play();

}

}

});

MusicList[3].addActionListener(new ActionListener(){

public void actionPerformed(ActionEvent e) {

if(e.getSource()==MusicList[3]){

URL file=getClass().getResource("music/第一次.mid");

AudioClip sound=java.applet.Applet.newAudioClip(file);

sound.play();

}

}

});

MusicList[4].addActionListener(new ActionListener(){

public void actionPerformed(ActionEvent e) {

if(e.getSource()==MusicList[4]){

URL file=getClass().getResource("music/七里香-钢琴版.mid");

AudioClip sound=java.applet.Applet.newAudioClip(file);

sound.play();

}

}

});

MusicList[5].addActionListener(new ActionListener(){

public void actionPerformed(ActionEvent e) {

if(e.getSource()==MusicList[5]){

URL file=getClass().getResource("music/盛夏的果实.mid");

AudioClip sound=java.applet.Applet.newAudioClip(file);

sound.play();

}

}

});

MusicList[6].addActionListener(new ActionListener(){

public void actionPerformed(ActionEvent e) {

if(e.getSource()==MusicList[6]){

URL file=getClass().getResource("music/唯一.mid");

AudioClip sound=java.applet.Applet.newAudioClip(file);

sound.play();

}

}

});

MusicList[7].addActionListener(new ActionListener(){

public void actionPerformed(ActionEvent e) {

if(e.getSource()==MusicList[7]){

URL file=getClass().getResource("music/");

AudioClip sound=java.applet.Applet.newAudioClip(file);

sound.stop();

}

}

});

}

//初始化面板

private void init(){

MusicList=new JCheckBoxMenuItem[8];

b=new ButtonGroup();

for(int i=0;i<8;i++)

{

MusicList[i]=new JCheckBoxMenuItem();

b.add(MusicList[i]);

this.add(MusicList[i]);

}

MusicList[0].setText("爱的代价");

MusicList[0].setToolTipText("梁咏琪");

MusicList[1].setText("爱的就是你");

MusicList[1].setToolTipText("王力宏");

MusicList[2].setText("当你孤单你会想起谁");

MusicList[2].setToolTipText("张栋梁");

MusicList[3].setText("第一次");

MusicList[3].setToolTipText("光良");

MusicList[4].setText("七里香");

MusicList[4].setToolTipText("周杰伦");

MusicList[5].setText("盛夏的果实");

MusicList[5].setToolTipText("莫文蔚");

MusicList[6].setText("唯一");

MusicList[6].setToolTipText("王力宏");

MusicList[7].setText(" i am woring now");

MusicList[7].setToolTipText("No music");

MusicList[7].setSelected(true);

}

}
————————————————
版权声明：本文为CSDN博主「周恰恰」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/weixin_31188927/article/details/114566446