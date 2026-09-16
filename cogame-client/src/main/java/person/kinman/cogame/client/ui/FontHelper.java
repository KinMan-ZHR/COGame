package person.kinman.cogame.client.ui;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 跨平台中文字体自适应解析与编码安全辅助：
 * 解决 Windows/Linux/macOS 下 SansSerif 缺字导致的方块/乱码(tofu)问题，
 * 优先采用高质量中文字体（微软雅黑、苹方、思源黑体等），并以 Java 逻辑复合字体 Dialog 兜底。
 */
public class FontHelper {
    private static String preferredUiFontName = null;
    private static String preferredMonoFontName = null;

    public static synchronized String getPreferredUiFontName() {
        if (preferredUiFontName != null) {
            return preferredUiFontName;
        }

        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Set<String> available = new HashSet<>(Arrays.asList(ge.getAvailableFontFamilyNames()));

            // 优先匹配高质量现代中文字体
            String[] candidates = {
                    "Microsoft YaHei UI",
                    "Microsoft YaHei",
                    "PingFang SC",
                    "Hiragino Sans GB",
                    "Noto Sans CJK SC",
                    "Source Han Sans SC",
                    "Source Han Sans CN",
                    "WenQuanYi Micro Hei",
                    "SimHei"
            };

            for (String candidate : candidates) {
                if (available.contains(candidate)) {
                    preferredUiFontName = candidate;
                    return preferredUiFontName;
                }
            }
        } catch (Throwable ignored) {}

        // 兜底采用 Java 内建复合字体 Dialog（保证通过 fontconfig 映射本地多语言字形）
        preferredUiFontName = Font.DIALOG;
        return preferredUiFontName;
    }

    public static synchronized String getPreferredMonoFontName() {
        if (preferredMonoFontName != null) {
            return preferredMonoFontName;
        }

        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Set<String> available = new HashSet<>(Arrays.asList(ge.getAvailableFontFamilyNames()));
            String[] candidates = {"Consolas", "Cascadia Code", "JetBrains Mono", "Courier New"};
            for (String candidate : candidates) {
                if (available.contains(candidate)) {
                    preferredMonoFontName = candidate;
                    return preferredMonoFontName;
                }
            }
        } catch (Throwable ignored) {}

        preferredMonoFontName = Font.MONOSPACED;
        return preferredMonoFontName;
    }

    public static Font getFont(int style, int size) {
        return new Font(getPreferredUiFontName(), style, size);
    }

    public static Font getMonospaceFont(int style, int size) {
        return new Font(getPreferredMonoFontName(), style, size);
    }

    /**
     * 安全过滤/净化玩家昵称：防止编码失真、替换符 \uFFFD 或不可见控制字符导致的乱码
     */
    public static String sanitizeName(String name, String defaultName) {
        if (name == null || name.trim().isEmpty()) {
            return defaultName;
        }
        String trimmed = name.trim();
        // 如果含有替换符（如 UTF-8 乱码导致的 \uFFFD、锟斤拷、鎴等 GBK 误解码特征或连续问号），则降级到默认安全名
        if (trimmed.contains("\uFFFD") || trimmed.contains("锟斤拷") || trimmed.contains("???") || trimmed.contains("鎴")) {
            return defaultName;
        }
        // 过滤不可见控制字符
        String cleaned = trimmed.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        if (cleaned.isEmpty()) {
            return defaultName;
        }
        return cleaned;
    }

    /**
     * 安全按 Unicode CodePoint 截断字符串，杜绝生硬切割导致 Emoji 或双字节字形断裂
     */
    public static String abbreviate(String text, int maxCodePoints) {
        if (text == null) return "";
        int cpCount = text.codePointCount(0, text.length());
        if (cpCount <= maxCodePoints) {
            return text;
        }
        int endIndex = text.offsetByCodePoints(0, maxCodePoints - 1);
        return text.substring(0, endIndex) + "…";
    }
}
