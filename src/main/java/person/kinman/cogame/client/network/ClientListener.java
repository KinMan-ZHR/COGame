package person.kinman.cogame.client.network;

public interface ClientListener {
    void onConnected(int playerId);
    void onGameStateUpdated();
    void onPlayerDisconnected(int playerId);
    void onError(String message);
}