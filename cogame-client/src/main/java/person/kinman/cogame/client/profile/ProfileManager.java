package person.kinman.cogame.client.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import person.kinman.cogame.client.ui.FontHelper;

/**
 * 本地玩家档案与配置管理器：记住玩家昵称与登录状态 (全链路强制 UTF-8 编码与防乱码自愈)
 */
public class ProfileManager {
    private static final String PROFILE_DIR = System.getProperty("user.home") + File.separator + ".cogame";
    private static final String PROFILE_FILE = PROFILE_DIR + File.separator + "profile.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static class Profile {
        public String nickname = "我";
        public boolean hasLoggedInOnline = false;
        public String lastServerUrl = "ws://127.0.0.1:8088";
    }

    private static Profile cachedProfile = null;

    public static synchronized Profile loadProfile() {
        if (cachedProfile != null) {
            return cachedProfile;
        }

        File file = new File(PROFILE_FILE);
        if (file.exists()) {
            // 优先尝试使用标准 UTF-8 读取
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                cachedProfile = gson.fromJson(reader, Profile.class);
                if (cachedProfile != null) {
                    cachedProfile.nickname = FontHelper.sanitizeName(cachedProfile.nickname, "我");
                    return cachedProfile;
                }
            } catch (Exception ignored) {}

            // 若 UTF-8 解析异常或文件此前由 GBK 系统默认字符集写入，尝试用 GBK/默认编码降级挽救
            try (BufferedReader gbkReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), "GBK"))) {
                cachedProfile = gson.fromJson(gbkReader, Profile.class);
                if (cachedProfile != null) {
                    cachedProfile.nickname = FontHelper.sanitizeName(cachedProfile.nickname, "我");
                    // 重新以标准 UTF-8 覆盖保存，永久自愈本地文件
                    saveProfile(cachedProfile.nickname, cachedProfile.lastServerUrl);
                    return cachedProfile;
                }
            } catch (Exception ignored) {}
        }

        cachedProfile = new Profile();
        return cachedProfile;
    }

    public static synchronized void saveProfile(String nickname, String serverUrl) {
        Profile profile = loadProfile();
        if (nickname != null && !nickname.trim().isEmpty()) {
            profile.nickname = FontHelper.sanitizeName(nickname, "我");
            profile.hasLoggedInOnline = true;
        }
        if (serverUrl != null && !serverUrl.trim().isEmpty()) {
            profile.lastServerUrl = serverUrl.trim();
        }

        File dir = new File(PROFILE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 显式指定 UTF-8 写入
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(PROFILE_FILE), StandardCharsets.UTF_8))) {
            gson.toJson(profile, writer);
        } catch (Exception ignored) {}
    }

    /**
     * 获取单机/人机对战时 P1 应显示的名称：若在线登录过则显示玩家名，否则显示“我”
     */
    public static String getDisplayName() {
        Profile profile = loadProfile();
        if (profile.hasLoggedInOnline && profile.nickname != null && !profile.nickname.trim().isEmpty()) {
            return FontHelper.sanitizeName(profile.nickname, "我");
        }
        return "我";
    }

    public static String getLastServerUrl() {
        return loadProfile().lastServerUrl;
    }
}
