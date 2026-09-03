package person.kinman.cogame.client.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * 本地玩家档案与配置管理器：记住玩家昵称与登录状态
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
            try (FileReader reader = new FileReader(file)) {
                cachedProfile = gson.fromJson(reader, Profile.class);
                if (cachedProfile != null) {
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
            profile.nickname = nickname.trim();
            profile.hasLoggedInOnline = true;
        }
        if (serverUrl != null && !serverUrl.trim().isEmpty()) {
            profile.lastServerUrl = serverUrl.trim();
        }

        File dir = new File(PROFILE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(PROFILE_FILE)) {
            gson.toJson(profile, writer);
        } catch (Exception ignored) {}
    }

    /**
     * 获取单机/人机对战时 P1 应显示的名称：若在线登录过则显示玩家名，否则显示“我”
     */
    public static String getDisplayName() {
        Profile profile = loadProfile();
        if (profile.hasLoggedInOnline && profile.nickname != null && !profile.nickname.trim().isEmpty()) {
            return profile.nickname.trim();
        }
        return "我";
    }

    public static String getLastServerUrl() {
        return loadProfile().lastServerUrl;
    }
}
