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
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("当前运行环境为无头环境 (Headless)，不支持启动图形界面。");
            System.err.println("请将 cogame-client.jar 下载到拥有桌面显示环境 (Windows / macOS / Linux GUI) 的客户端机器运行。");
            System.exit(0);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            if (args.length > 0) {
                switch (args[0]) {
                    case "--local" -> new GameFrame(new LocalController()).display();
                    case "--ai" -> new GameFrame(new AiController()).display();
                    case "--online" -> {
                        String server = (args.length > 1) ? args[1] : "ws://127.0.0.1:8088";
                        String room = (args.length > 2) ? args[2] : "1001";
                        String name = (args.length > 3) ? args[3] : "Player";
                        new GameFrame(new OnlineController(server, room, name)).display();
                    }
                    default -> new MainMenuFrame().setVisible(true);
                }
            } else {
                new MainMenuFrame().setVisible(true);
            }
        });
    }
}
