package person.kinman.cogame.client.audio;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * 游戏音频与背景音乐管理器（支持安全降级，无声卡环境下静默不报错）
 * 支持绝对无缝、跨平台的无限循环播放（附带 LineListener 驱动事件自愈重播）
 */
public class AudioPlayer {
    private static Clip currentClip;
    private static String currentMusicName;
    private static long playToken = 0;

    public static synchronized void playMusic(String resourceName) {
        if (resourceName == null || resourceName.equals(currentMusicName)) {
            return;
        }

        currentMusicName = resourceName;
        final long token = ++playToken;

        stopCurrentClipOnly();

        new Thread(() -> {
            try {
                InputStream is = AudioPlayer.class.getClassLoader().getResourceAsStream(resourceName);
                if (is == null) return;

                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);

                synchronized (AudioPlayer.class) {
                    if (token != playToken) {
                        // 期间已切换了其他曲目或执行了 stopMusic
                        try { clip.close(); } catch (Throwable ignored) {}
                        return;
                    }
                    currentClip = clip;
                }

                // 注册底层行事件监听器：若某些平台驱动层播完单轮未自动无缝循环，自动帧复位并重启循环，确保永不停歇
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        synchronized (AudioPlayer.class) {
                            if (token == playToken && clip == currentClip) {
                                try {
                                    clip.setFramePosition(0);
                                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                });

                // 启动连续无限循环（注意：切勿在此处叠加调用 clip.start()，否则某些系统驱动会重置 loopCount 为 0 导致单次播完即停）
                clip.loop(Clip.LOOP_CONTINUOUSLY);

            } catch (Throwable t) {
                // 静默降级（例如无图形界面/无声卡服务器环境）
            }
        }, "COGame-Audio-Thread").start();
    }

    public static synchronized void playOnce(String resourceName) {
        if (resourceName == null) {
            return;
        }

        currentMusicName = resourceName;
        final long token = ++playToken;

        stopCurrentClipOnly();

        new Thread(() -> {
            try {
                InputStream is = AudioPlayer.class.getClassLoader().getResourceAsStream(resourceName);
                if (is == null) return;

                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);

                synchronized (AudioPlayer.class) {
                    if (token != playToken) {
                        try { clip.close(); } catch (Throwable ignored) {}
                        return;
                    }
                    currentClip = clip;
                }

                clip.start();
            } catch (Throwable t) {
                // 静默降级
            }
        }, "COGame-Audio-Thread").start();
    }

    private static void stopCurrentClipOnly() {
        if (currentClip != null) {
            try {
                currentClip.stop();
                currentClip.close();
            } catch (Throwable ignored) {}
            currentClip = null;
        }
    }

    public static synchronized void stopMusic() {
        playToken++;
        currentMusicName = null;
        stopCurrentClipOnly();
    }

    public static synchronized String getCurrentMusicName() {
        return currentMusicName;
    }
}
