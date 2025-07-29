// client/network/GameClient.java
package person.kinman.cogame.client.network;


import person.kinman.cogame.common.GameMessage;
import person.kinman.cogame.share.game.GameLogic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameClient {
    private final String serverHost;
    private final int serverPort;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private boolean connected = false;
    private int playerId;
    private final GameLogic gameLogic;
    private final ClientListener listener;

    public GameClient(String serverHost, int serverPort, GameLogic gameLogic, ClientListener listener) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.gameLogic = gameLogic;
        this.listener = listener;
    }

    public void connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            
            threadPool.submit(this::receiveMessages);
            System.out.println("已连接到服务器: " + serverHost + ":" + serverPort);
        } catch (IOException e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onError("无法连接到服务器");
            }
        }
    }

    public void disconnect() {
        if (connected) {
            try {
                sendMessage(new GameMessage(GameMessage.Type.DISCONNECT, playerId, null));
                connected = false;
                threadPool.shutdown();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(GameMessage message) {
        try {
            if (connected && out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            disconnect();
        }
    }

    private void receiveMessages() {
        try {
            while (connected) {
                GameMessage message = (GameMessage) in.readObject();
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (connected) {
                disconnect();
                if (listener != null) {
                    listener.onError("与服务器断开连接");
                }
            }
        }
    }

    private void handleMessage(GameMessage message) {
        switch (message.getType()) {
            case CONNECT:
                this.playerId = message.getPlayerId();
                if (listener != null) {
                    listener.onConnected(playerId);
                }
                break;
            case GAME_STATE:
                GameLogic newState = (GameLogic) message.getData();
                // 更新本地游戏状态
                updateGameState(newState);
                if (listener != null) {
                    listener.onGameStateUpdated();
                }
                break;
            case DISCONNECT:
                if (listener != null) {
                    listener.onPlayerDisconnected((int) message.getData());
                }
                break;
        }
    }

    private void updateGameState(GameLogic newState) {
        // 同步游戏状态
        this.gameLogic.getPlayers().clear();
        this.gameLogic.getPlayers().addAll(newState.getPlayers());
        this.gameLogic.setCurrentPlayerIndex(newState.getCurrentPlayerIndex());
        this.gameLogic.setGameOver(newState.isGameOver());
        // 同步地图状态
        this.gameLogic.getMap().updateFrom(newState.getMap());
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return connected;
    }
}