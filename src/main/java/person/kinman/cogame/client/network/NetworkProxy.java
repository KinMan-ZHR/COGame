package person.kinman.cogame.client.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import person.kinman.cogame.client.contract.INetworkProxy;
import person.kinman.cogame.client.event.ConnectionStatusEvent;
import person.kinman.cogame.client.event.ServerMessageEvent;
import person.kinman.cogame.client.eventBus.GlobalEventBus;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 网络代理类
 */
public class NetworkProxy implements INetworkProxy {
    private static final Logger log = LoggerFactory.getLogger(NetworkProxy.class);
    private volatile Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;
    // 定义锁对象
    private final Object lock = new Object();

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
            synchronized (lock){
                try {
                    // 建立Socket连接
                    clientSocket = new Socket(ip, port);
                    // 获取输入输出流
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                    out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                    // 启动消息监听线程
                    startMessageListener();

                    // 可以发送一个连接成功的确认消息给服务器
                    sendMessage("客户端已连接");
                    // 通知UI连接成功
                    onConnectionStatusChanged("已连接到服务器: " + ip + ":" + port);

                } catch (IOException e) {
                    // 通知UI连接失败
                    onConnectionStatusChanged("连接失败: " + e.getMessage());
                    // 清理资源
                    closeResources();
                }
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
                onConnectionStatusChanged("已与服务器断开连接");
                
            } catch (IOException e) {
                // 发生异常，连接已断开
                onConnectionStatusChanged("与服务器断开连接: " + e.getMessage());
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
            onConnectionStatusChanged("未连接到服务器，无法发送消息");
        }
    }

    /**
     * 断开与服务器的连接
     */
    @Override
    public void disconnect() {
        synchronized (lock) { // 加锁保证关闭操作的原子性
            // 仅在连接存在时执行关闭
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    // 1. 先关闭输入输出流（避免读写操作干扰）
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                    // 2. 关闭Socket（触发四次挥手）
                    clientSocket.close();
                    // 3. 关闭成功后通知状态
                    onConnectionStatusChanged("已与服务器断开连接");
                } catch (IOException e) {
                    // 关闭失败时通知异常，但不立即释放资源（可能需要重试）
                    onConnectionStatusChanged("断开连接失败: " + e.getMessage());
                    return; // 关闭失败时不执行后续的资源置空，保留状态以便重试
                }
            }
            // 4. 只有关闭成功或连接已不存在时，才释放资源
            closeResources();
        }
    }

    /**
     * 关闭所有网络资源
     */
    private void closeResources() {
        try {
            // 二次确认关闭（防止遗漏）
            if (in != null) {
                in.close();
                in = null;
            }
            if (out != null) {
                out.close();
                out = null;
            }
            if (clientSocket != null) {
                clientSocket.close(); // 即使之前关闭过，再次关闭也不会报错
                clientSocket = null;
            }
        } catch (IOException e) {
            log.error("释放资源时发生异常", e); // 仅日志记录，不影响状态
        }

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
        synchronized (lock) { // 用锁保证三个条件判断的原子性
            return clientSocket != null
                    && !clientSocket.isClosed()
                    && clientSocket.isConnected();
        }
    }

    private void onMessageReceived(String message) {
        GlobalEventBus.getBusinessBus().post(new ServerMessageEvent(message));
    }

    private void onConnectionStatusChanged(String message) {
        GlobalEventBus.getBusinessBus().post(new ConnectionStatusEvent(isConnected(), message));
    }
}
