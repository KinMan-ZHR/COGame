package person.kinman.cogame.client;

import person.kinman.cogame.client.controller.AiController;
import person.kinman.cogame.client.controller.LocalController;
import person.kinman.cogame.client.controller.OnlineController;
import person.kinman.cogame.client.ui.GameFrame;
import person.kinman.cogame.client.ui.MainMenuFrame;

import javax.swing.*;
import java.awt.*;

/**
 * 客户端主程序入口
 */
public class ClientMain {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("当前运行环境为无头环境 (Headless)，不支持启动图形界面。");
            System.err.println("请将 cogame-client.jar 下载到拥有桌面显示环境 (Windows / macOS / Linux GUI) 的客户端机器运行。");
            System.exit(0);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            UIManager.put("Panel.background", new Color(15, 23, 42));
            UIManager.put("OptionPane.background", new Color(15, 23, 42));
            UIManager.put("OptionPane.messageForeground", new Color(248, 250, 252));
            UIManager.put("Label.foreground", new Color(248, 250, 252));
            UIManager.put("TextField.background", new Color(30, 41, 59));
            UIManager.put("TextField.foreground", new Color(248, 250, 252));
            UIManager.put("TextField.caretForeground", new Color(56, 189, 248));
            UIManager.put("PasswordField.background", new Color(30, 41, 59));
            UIManager.put("PasswordField.foreground", new Color(248, 250, 252));
            UIManager.put("Table.background", new Color(30, 41, 59));
            UIManager.put("Table.foreground", new Color(248, 250, 252));
            UIManager.put("Table.gridColor", new Color(51, 65, 85));
            UIManager.put("TableHeader.background", new Color(21, 32, 54));
            UIManager.put("TableHeader.foreground", new Color(56, 189, 248));

            if (args.length > 0) {
                switch (args[0]) {
                    case "--local" -> {
                        int size = (args.length > 1) ? parseSize(args[1]) : 6;
                        new GameFrame(new LocalController(size)).display();
                    }
                    case "--ai" -> {
                        int size = (args.length > 1) ? parseSize(args[1]) : 6;
                        new GameFrame(new AiController(size)).display();
                    }
                    case "--online" -> {
                        String server = (args.length > 1) ? args[1] : "ws://127.0.0.1:8088";
                        String room = (args.length > 2) ? args[2] : "1001";
                        String name = (args.length > 3) ? args[3] : "Player";
                        int size = (args.length > 4) ? parseSize(args[4]) : 6;
                        new GameFrame(new OnlineController(server, room, name, size)).display();
                    }
                    default -> new MainMenuFrame().setVisible(true);
                }
            } else {
                new MainMenuFrame().setVisible(true);
            }
        });
    }

    private static int parseSize(String arg) {
        try {
            int s = Integer.parseInt(arg.trim());
            return Math.max(6, Math.min(13, s));
        } catch (NumberFormatException e) {
            return 6;
        }
    }
}
