// server/Main.java
package person.kinman.cogame.server;


import person.kinman.cogame.server.network.GameServer;

public class Main {
    public static void main(String[] args) {
        int port = 8888;
        GameServer server = new GameServer(port);
        server.start();
    }
}