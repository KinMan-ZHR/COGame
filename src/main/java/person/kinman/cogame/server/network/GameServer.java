// server/network/GameServer.java
package person.kinman.cogame.server.network;



import person.kinman.cogame.common.GameMessage;
import person.kinman.cogame.share.game.GameLogic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private final int port;
    private final GameLogic gameLogic;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private boolean running = false;

    public GameServer(int port) {
        this.port = port;
        this.gameLogic = new GameLogic();
    }

    public void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("服务器启动，监听端口: " + port);
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, clients.size() + 1);
                clients.add(clientHandler);
                threadPool.submit(clientHandler);
                System.out.println("新客户端连接: " + clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        threadPool.shutdown();
        clients.forEach(ClientHandler::close);
        System.out.println("服务器已关闭");
    }

    private void broadcast(GameMessage message) {
        clients.forEach(client -> client.sendMessage(message));
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private boolean connected = true;
        private final int playerId;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                // 发送欢迎消息
                sendMessage(new GameMessage(GameMessage.Type.CONNECT, playerId, null));
                
                // 通知其他客户端
                broadcast(new GameMessage(GameMessage.Type.CONNECT, playerId, "Player " + playerId + " joined"));
                
                // 处理客户端消息
                while (connected) {
                    GameMessage message = (GameMessage) in.readObject();
                    handleMessage(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    disconnect();
                }
            }
        }

        private void handleMessage(GameMessage message) {
            switch (message.getType()) {
                case MOVE:
                    gameLogic.movePlayer(playerId, (int) message.getData());
                    broadcast(new GameMessage(GameMessage.Type.GAME_STATE, playerId, gameLogic));
                    break;
                case DISCONNECT:
                    disconnect();
                    break;
            }
        }

        public void sendMessage(GameMessage message) {
            try {
                if (out != null) {
                    out.writeObject(message);
                    out.flush();
                }
            } catch (IOException e) {
                disconnect();
            }
        }

        public void close() {
            connected = false;
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void disconnect() {
            connected = false;
            clients.remove(this);
            close();
            broadcast(new GameMessage(GameMessage.Type.DISCONNECT, playerId, "Player " + playerId + " left"));
        }
    }
}