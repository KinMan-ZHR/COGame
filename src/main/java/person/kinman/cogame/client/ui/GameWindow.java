package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.network.ClientListener;
import person.kinman.cogame.client.network.GameClient;
import person.kinman.cogame.share.game.GameInterface;

import javax.swing.*;

/**
 * @author KinMan谨漫
 * @date 2025/7/29 17:55
 */
public class GameWindow extends JFrame implements ClientListener {

    public GameInterface gameInterface;
    public GameClient client;

    public GameWindow(GameInterface gameInterface) {
        this.gameInterface = gameInterface;
    }

    public void setClient(GameClient client) {
        this.client = client;
    }

    public void setGameInterface(GameInterface gameInterface){
        this.gameInterface = gameInterface;
    }

    @Override
    public void onConnected(int playerId) {

    }

    @Override
    public void onGameStateUpdated() {

    }

    @Override
    public void onPlayerDisconnected(int playerId) {

    }

    @Override
    public void onError(String message) {

    }
}
