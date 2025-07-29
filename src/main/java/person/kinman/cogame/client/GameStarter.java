// client/GameStarter.java
package person.kinman.cogame.client;

import person.kinman.cogame.client.network.GameClient;
import person.kinman.cogame.client.ui.GameWindow;
import person.kinman.cogame.share.game.GameInterface;
import person.kinman.cogame.share.game.LocalGameManager;
import person.kinman.cogame.share.game.NetworkGameManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameStarter {
    public static void main(String[] args) {
        // 创建模式选择对话框
        JFrame dialogFrame = new JFrame("选择游戏模式");
        JPanel panel = new JPanel();
        JButton localModeButton = new JButton("本地对战");
        JButton networkModeButton = new JButton("联网对战");
        
        panel.add(localModeButton);
        panel.add(networkModeButton);
        dialogFrame.add(panel);
        dialogFrame.pack();
        dialogFrame.setLocationRelativeTo(null);
        dialogFrame.setVisible(true);
        
        // 本地模式
        localModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialogFrame.dispose();
                // 创建本地游戏管理器
                GameInterface gameManager = new LocalGameManager();
                // 创建游戏窗口
                GameWindow window = new GameWindow(gameManager);
                window.setVisible(true);
                // 初始化游戏
                gameManager.initGame();
            }
        });
        
        // 联网模式
        networkModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialogFrame.dispose();
                // 创建服务器连接对话框...
                String serverAddress = JOptionPane.showInputDialog("输入服务器地址:");
                int port = Integer.parseInt(JOptionPane.showInputDialog("输入端口号:"));
                // 创建游戏窗口
                GameWindow window = new GameWindow(gameManager);
                // 创建网络客户端
                GameClient client = new GameClient(serverAddress, port);
                // 创建网络游戏管理器
                GameInterface gameManager = new NetworkGameManager(client);

                window.setVisible(true);
                
                // 连接服务器
                client.connect();
            }
        });
    }
}