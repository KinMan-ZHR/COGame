package person.kinman.cogame.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import person.kinman.cogame.client.profile.ProfileManager;
import person.kinman.cogame.client.ui.FontHelper;

import java.awt.*;

public class FontAndProfileTest {

    @Test
    public void testFontHelperResolution() {
        String uiFontName = FontHelper.getPreferredUiFontName();
        Assertions.assertNotNull(uiFontName);
        Assertions.assertFalse(uiFontName.trim().isEmpty());

        Font boldFont = FontHelper.getFont(Font.BOLD, 14);
        Assertions.assertNotNull(boldFont);
        Assertions.assertEquals(Font.BOLD, boldFont.getStyle());
        Assertions.assertEquals(14, boldFont.getSize());

        Font monoFont = FontHelper.getMonospaceFont(Font.PLAIN, 12);
        Assertions.assertNotNull(monoFont);
    }

    @Test
    public void testNameSanitization() {
        // 正常中文名字
        Assertions.assertEquals("策天", FontHelper.sanitizeName("策天", "我"));
        Assertions.assertEquals("玩家123", FontHelper.sanitizeName("  玩家123  ", "我"));

        // 空串、纯空白
        Assertions.assertEquals("我", FontHelper.sanitizeName("", "我"));
        Assertions.assertEquals("我", FontHelper.sanitizeName("   ", "我"));
        Assertions.assertEquals("默认玩家", FontHelper.sanitizeName(null, "默认玩家"));

        // 包含乱码替换符、问号乱码、GBK误读特殊字符
        Assertions.assertEquals("我", FontHelper.sanitizeName("玩家\uFFFD", "我"));
        Assertions.assertEquals("我", FontHelper.sanitizeName("???", "我"));
        Assertions.assertEquals("我", FontHelper.sanitizeName("鎴", "我"));

        // 包含不可见控制字符时应被滤除
        Assertions.assertEquals("Alpha", FontHelper.sanitizeName("Alpha\u0000\u0007", "我"));
    }

    @Test
    public void testSafeAbbreviation() {
        Assertions.assertEquals("", FontHelper.abbreviate(null, 5));
        Assertions.assertEquals("策天", FontHelper.abbreviate("策天", 5));
        Assertions.assertEquals("一二三四…", FontHelper.abbreviate("一二三四五六七", 5));
        Assertions.assertEquals("Super…", FontHelper.abbreviate("SuperLongPlayerName", 6));
    }

    @Test
    public void testProfileManagerSaveAndLoad() {
        String testName = "单元测试玩家";
        String testUrl = "ws://localhost:9999";
        ProfileManager.saveProfile(testName, testUrl);

        ProfileManager.Profile profile = ProfileManager.loadProfile();
        Assertions.assertNotNull(profile);
        Assertions.assertEquals(testName, profile.nickname);
        Assertions.assertEquals(testUrl, profile.lastServerUrl);
        Assertions.assertTrue(profile.hasLoggedInOnline);
        Assertions.assertEquals(testName, ProfileManager.getDisplayName());
    }
}
