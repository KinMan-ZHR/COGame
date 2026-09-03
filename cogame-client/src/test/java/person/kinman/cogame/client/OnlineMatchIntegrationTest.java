package person.kinman.cogame.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import person.kinman.cogame.client.controller.OnlineController;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.net.WsMessage;
import person.kinman.cogame.server.net.CoGameWebSocketServer;

import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class OnlineMatchIntegrationTest {

    @Test
    public void testTwoPlayersConnectAndPlay() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        CoGameWebSocketServer server = new CoGameWebSocketServer(port);
        server.setReuseAddr(true);
        server.start();

        Thread.sleep(300);

        String serverUrl = "ws://127.0.0.1:" + port;
        String roomId = "test-integration-" + System.currentTimeMillis();

        CountDownLatch moveLatch = new CountDownLatch(1);

        OnlineController p1 = new OnlineController(serverUrl, roomId, "Alice");
        OnlineController p2 = new OnlineController(serverUrl, roomId, "Bob");

        try {
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                if (p1.isGameStarted() && p2.isGameStarted()) {
                    break;
                }
                Thread.sleep(50);
            }
            Assertions.assertTrue(p1.isGameStarted() && p2.isGameStarted(), "双方未能在5秒内成功匹配开局");

            Assertions.assertTrue((p1.getMyPlayerId() == 1 && p2.getMyPlayerId() == 2)
                               || (p1.getMyPlayerId() == 2 && p2.getMyPlayerId() == 1));

            OnlineController firstPlayer = (p1.getMyPlayerId() == 1) ? p1 : p2;
            OnlineController secondPlayer = (p1.getMyPlayerId() == 1) ? p2 : p1;

            secondPlayer.setOnStateChanged(state -> {
                if (state.getP1().getR() == 1 && state.getP1().getC() == 0) {
                    moveLatch.countDown();
                }
            });

            firstPlayer.handleUserAction(GameAction.changeDirMove(Direction.DOWN));

            boolean synced = moveLatch.await(5, TimeUnit.SECONDS);
            Assertions.assertTrue(synced, "先手行动未能实时同步给后手");
        } finally {
            p1.close();
            p2.close();
            server.stop();
        }
    }

    @Test
    public void testUniqueNicknameLogin() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        CoGameWebSocketServer server = new CoGameWebSocketServer(port);
        server.setReuseAddr(true);
        server.start();
        Thread.sleep(300);

        String serverUrl = "ws://127.0.0.1:" + port;
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<String> res1 = new AtomicReference<>();
        AtomicReference<String> res2 = new AtomicReference<>();

        // Client 1: login Alice
        WebSocketClient c1 = new WebSocketClient(new URI(serverUrl)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                send(WsMessage.login("Alice").toJson());
            }

            @Override
            public void onMessage(String message) {
                WsMessage msg = WsMessage.fromJson(message);
                if (msg != null) {
                    res1.set(msg.getType());
                    latch1.countDown();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {}

            @Override
            public void onError(Exception ex) {}
        };
        c1.connect();
        Assertions.assertTrue(latch1.await(3, TimeUnit.SECONDS));
        Assertions.assertEquals(WsMessage.TYPE_LOGIN_SUCCESS, res1.get());

        // Client 2: duplicate login Alice -> must fail
        WebSocketClient c2 = new WebSocketClient(new URI(serverUrl)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                send(WsMessage.login("Alice").toJson());
            }

            @Override
            public void onMessage(String message) {
                WsMessage msg = WsMessage.fromJson(message);
                if (msg != null) {
                    res2.set(msg.getType());
                    latch2.countDown();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {}

            @Override
            public void onError(Exception ex) {}
        };
        c2.connect();
        Assertions.assertTrue(latch2.await(3, TimeUnit.SECONDS));
        Assertions.assertEquals(WsMessage.TYPE_LOGIN_FAIL, res2.get(), "重复昵称应当被拒绝");

        c1.close();
        c2.close();
        server.stop();
    }

    @Test
    public void testPasswordProtectedRoom() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        CoGameWebSocketServer server = new CoGameWebSocketServer(port);
        server.setReuseAddr(true);
        server.start();
        Thread.sleep(300);

        String serverUrl = "ws://127.0.0.1:" + port;
        String roomId = "secret-room-" + System.currentTimeMillis();

        // P1 creates room with password "pass123"
        OnlineController p1 = new OnlineController(serverUrl, roomId, "P1Host", 8, "pass123");

        // P2 tries to join with wrong password "wrong"
        CountDownLatch errorLatch = new CountDownLatch(1);
        OnlineController p2Wrong = new OnlineController(serverUrl, roomId, "P2Wrong", 8, "wrong");
        p2Wrong.setOnNotification(msg -> {
            if (msg.contains("密码错误")) {
                errorLatch.countDown();
            }
        });
        Assertions.assertTrue(errorLatch.await(3, TimeUnit.SECONDS), "错误密码应当收到密码错误通知");
        p2Wrong.close();

        // P2 joins with correct password "pass123"
        OnlineController p2Correct = new OnlineController(serverUrl, roomId, "P2Real", 8, "pass123");

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (p1.isGameStarted() && p2Correct.isGameStarted()) {
                break;
            }
            Thread.sleep(50);
        }
        Assertions.assertTrue(p1.isGameStarted() && p2Correct.isGameStarted(), "正确密码应当成功开局");

        p1.close();
        p2Correct.close();
        server.stop();
    }
}
