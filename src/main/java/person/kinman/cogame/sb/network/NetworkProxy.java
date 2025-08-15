package person.kinman.cogame.sb.network;

import person.kinman.cogame.sb.contract.INetworkProxy;
import person.kinman.cogame.client.ui.page.event.GlobalEventBus;
import person.kinman.cogame.sb.event.ConnectionStatusEvent;
import person.kinman.cogame.sb.event.ServerMessageEvent;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 网络代理类
 */
public class NetworkProxy implements INetworkProxy {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;

    public NetworkProxy() {}

    /**
     * 连接到服务器
     * @param ip 服务器IP地址
     * @param port 服务器端口号
     */
    @Override
    public void connect(String ip, int port) {
        // 先断开可能存在的连接
        disconnect();
        
        // 在新线程中执行连接操作，避免阻塞UI
        new Thread(() -> {
            try {
                // 建立Socket连接
                clientSocket = new Socket(ip, port);
                // 获取输入输出流
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                
                // 通知UI连接成功
                onConnectionStatusChanged(true, "已连接到服务器: " + ip + ":" + port);
                
                // 启动消息监听线程
                startMessageListener();
                
                // 可以发送一个连接成功的确认消息给服务器
                sendMessage("客户端已连接");
                
            } catch (IOException e) {
                // 通知UI连接失败
                onConnectionStatusChanged(false, "连接失败: " + e.getMessage());
                // 清理资源
                closeResources();
            }
        }).start();
    }

    /**
     * 启动消息监听线程，持续接收服务器发送的消息
     */
    private void startMessageListener() {
        listenerThread = new Thread(() -> {
            try {
                String message;
                // 循环读取服务器消息，直到连接关闭
                while ((message = in.readLine()) != null) {
                    onMessageReceived(message);
                }
                
                // 如果跳出循环，说明连接已关闭
                onConnectionStatusChanged(false, "已与服务器断开连接");
                
            } catch (IOException e) {
                // 发生异常，连接已断开
                onConnectionStatusChanged(false, "与服务器断开连接: " + e.getMessage());
            } finally {
                closeResources();
            }
        });
        
        listenerThread.start();
    }

    /**
     * 发送消息到服务器
     * @param message 要发送的消息
     */
    @Override
    public void sendMessage(String message) {
        // 检查连接是否有效
        if (out != null && clientSocket != null && !clientSocket.isClosed()) {
            new Thread(() -> out.println(message)).start();
        } else {
            onConnectionStatusChanged(false, "未连接到服务器，无法发送消息");
        }
    }

    /**
     * 断开与服务器的连接
     */
    @Override
    public void disconnect() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
                onConnectionStatusChanged(false, "已与服务器断开连接");
            } catch (IOException e) {
                onConnectionStatusChanged(false, "断开连接失败: " + e.getMessage());
            }
        }
        closeResources();
    }

    /**
     * 关闭所有网络资源
     */
    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        in = null;
        out = null;
        clientSocket = null;
        
        // 停止监听线程
        if (listenerThread != null && listenerThread.isAlive()) {
            listenerThread.interrupt();
            listenerThread = null;
        }
    }

    /**
     * 检查是否已连接到服务器
     */
    @Override
    public boolean isConnected() {
        return clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected();
    }

    private void onMessageReceived(String message) {
        GlobalEventBus.getInstance().post(new ServerMessageEvent(message));
    }

    private void onConnectionStatusChanged(boolean isConnected, String message) {
        GlobalEventBus.getInstance().post(new ConnectionStatusEvent(isConnected, message));
    }
}
