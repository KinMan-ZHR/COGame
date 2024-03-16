import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * @author KinMan谨漫
 * 注意该类只能播放wav文件（但属于一个通用文件）
 * please use createMusic method
 * and use getOnMusic method
 * and setPause method to control
 */
public class WavMusic extends Thread {
    private boolean live=true;
    private boolean pause=true;
    private final BufferedInputStream bufferedInputStream;
    private AudioInputStream audioInputStream;
    private SourceDataLine auLine;
    //缓冲
    private final byte[] readData = new byte[512];
    //自己维护自己
    private static final ArrayList<WavMusic> wavMusics=new ArrayList<>();
    private final String name;
    private WavMusic(String wavFile) {//以后只需调用就可以了，wavFile即为为文件名
        name=wavFile;
        //用包装流读取音乐文件
        bufferedInputStream = new BufferedInputStream(Objects.requireNonNull(GameWin.class.getClassLoader().getResourceAsStream(wavFile)));
        //将包装流得到音频输入流
        try {
            audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            return;//这里的return有一股不成功，便成仁的意味
        }
        //标起始点，和缓存大小，音乐一般要2的23次方到26次方之间
        //也可以通过该方法获得本地文件的大小audioInputStream.getFrameLength()
        audioInputStream.mark((int) Math.pow(2,26));
        //得到音乐格式
        AudioFormat format = audioInputStream.getFormat();
        //将格式加载到数据流信息上(下面两句不要了，被try里第一句给代替了)
        //DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        //auLine = (SourceDataLine) AudioSystem.getLine(info);
        //将数据信道打开
        try {
            auLine=AudioSystem.getSourceDataLine(audioInputStream.getFormat());
            auLine.open(format);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return;
        }
        //音量控制
        FloatControl control = (FloatControl) auLine.getControl(FloatControl.Type.MASTER_GAIN);
         /*
         -80.0~6.0206,数值越小音量越小
       	 */
        control.setValue(-20);
        //启动音乐线程
        this.start();
        //加入音乐集合之中
        wavMusics.add(this);
    }
    /**
     * 创建音乐
     */
    public static void createMusic(String wavFileName){
        new WavMusic(wavFileName);
    }
    /**
     * 打开音乐
     * @param wavFileName 音乐文件名字
     * @return 音乐
     */
    public static WavMusic getOnWavMusic(String wavFileName) {
        WavMusic wavMusic=null;
        for (WavMusic music :wavMusics){
           if(Objects.equals(music.name, wavFileName)){
               wavMusic=music;
           }else {
               music.pause=true;
           }
       }
        if (wavMusic != null) {
            wavMusic.pause=false;
        }else throw new RuntimeException("你想要播放的文件尚未create");
        return wavMusic;
    }
    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }
    @Override
    public void run() {
        while (live) {
            //是否在暂停状态
            while (pause){
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //启动信道
            int nBytesRead;
            auLine.start();
            try {
                //不停地读文件(播放音乐)
                while (live&& !pause) {
                    nBytesRead = audioInputStream.read(readData, 0, readData.length);
                    if (nBytesRead >= 0)
                        auLine.write(readData, 0, readData.length);
                    else
                        audioInputStream.reset();//读完了就重置到mark的位置
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //auLine.drain();
                auLine.stop();//到这里的是暂停
            }
        }
        //当生命消失之时就关闭一切
        try {
            auLine.close();
            audioInputStream.close();
            //因为对bufferedInputStream是底层数据流，只能最后关闭，其他流的操作都是在其打开的基础上进行的
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}