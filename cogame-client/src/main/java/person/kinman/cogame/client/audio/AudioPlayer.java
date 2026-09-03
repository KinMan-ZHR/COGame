package person.kinman.cogame.client.audio;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * 游戏音频与背景音乐管理器（支持安全降级，无声卡环境下静默不报错）
 */
public class AudioPlayer {
    private static Clip currentClip;
    private static String currentMusicName;

    public static synchronized void playMusic(String resourceName) {
        if (resourceName == null || resourceName.equals(currentMusicName)) {
            return;
        }

        stopMusic();

        new Thread(() -> {
            try {
                InputStream is = AudioPlayer.class.getClassLoader().getResourceAsStream(resourceName);
                if (is == null) return;

                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();

                synchronized (AudioPlayer.class) {
                    currentClip = clip;
                    currentMusicName = resourceName;
                }
            } catch (Throwable t) {
                // 静默降级（例如无图形界面/无声卡服务器环境）
            }
        }, "COGame-Audio-Thread").start();
    }

    public static synchronized void stopMusic() {
        if (currentClip != null) {
            try {
                currentClip.stop();
                currentClip.close();
            } catch (Throwable ignored) {}
            currentClip = null;
            currentMusicName = null;
        }
    }
}
