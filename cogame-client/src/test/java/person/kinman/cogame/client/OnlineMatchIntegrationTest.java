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
        Thread.sleep(250); // 确保房主已先建立房间

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

    @Test
    public void testTurnOrderResolutionBothChooseFirst() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        CoGameWebSocketServer server = new CoGameWebSocketServer(port);
        server.setReuseAddr(true);
        server.start();
        Thread.sleep(300);

        String serverUrl = "ws://127.0.0.1:" + port;
        String roomId = "turn-order-first-" + System.currentTimeMillis();

        AtomicReference<String> p1Notice = new AtomicReference<>();
        AtomicReference<String> p2Notice = new AtomicReference<>();

        // Host wants FIRST
        OnlineController host = new OnlineController(serverUrl, roomId, "HostP1", 6, null, person.kinman.cogame.core.model.TurnOrderPreference.FIRST);
        host.setOnNotification(p1Notice::set);

        Thread.sleep(200);

        // Guest also wants FIRST
        OnlineController guest = new OnlineController(serverUrl, roomId, "GuestP1", 6, null, person.kinman.cogame.core.model.TurnOrderPreference.FIRST);
        guest.setOnNotification(p2Notice::set);

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (host.isGameStarted() && guest.isGameStarted()) {
                break;
            }
            Thread.sleep(50);
        }
        Assertions.assertTrue(host.isGameStarted() && guest.isGameStarted(), "双方同选先手应成功开局");

        // Exactly one player is 1, the other is 2
        Assertions.assertTrue((host.getMyPlayerId() == 1 && guest.getMyPlayerId() == 2)
                           || (host.getMyPlayerId() == 2 && guest.getMyPlayerId() == 1));

        // Resolution message should indicate both selected FIRST
        String notice = p1Notice.get() != null ? p1Notice.get() : p2Notice.get();
        Assertions.assertNotNull(notice);
        Assertions.assertTrue(notice.contains("执先") || notice.contains("P1"));

        host.close();
        guest.close();
        server.stop();
    }

    @Test
    public void testTurnOrderResolutionComplementary() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        CoGameWebSocketServer server = new CoGameWebSocketServer(port);
        server.setReuseAddr(true);
        server.start();
        Thread.sleep(300);

        String serverUrl = "ws://127.0.0.1:" + port;
        String roomId = "turn-order-comp-" + System.currentTimeMillis();

        // Host wants SECOND
        OnlineController host = new OnlineController(serverUrl, roomId, "HostP2", 6, null, person.kinman.cogame.core.model.TurnOrderPreference.SECOND);

        Thread.sleep(200);

        // Guest wants FIRST
        OnlineController guest = new OnlineController(serverUrl, roomId, "GuestP1", 6, null, person.kinman.cogame.core.model.TurnOrderPreference.FIRST);

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (host.isGameStarted() && guest.isGameStarted()) {
                break;
            }
            Thread.sleep(50);
        }
        Assertions.assertTrue(host.isGameStarted() && guest.isGameStarted(), "互补分先意愿应成功开局");

        // Guest must be 1 (先手), Host must be 2 (后手)
        Assertions.assertEquals(1, guest.getMyPlayerId(), "选择执先的Guest应当获得P1先手");
        Assertions.assertEquals(2, host.getMyPlayerId(), "选择执后的Host应当获得P2后手");

        host.close();
        guest.close();
        server.stop();
    }

    @Test
    public void testInGameRematchWithTurnOrderArbitration() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        CoGameWebSocketServer server = new CoGameWebSocketServer(port);
        server.setReuseAddr(true);
        server.start();
        Thread.sleep(300);

        String serverUrl = "ws://127.0.0.1:" + port;
        String roomId = "in-game-rematch-" + System.currentTimeMillis();

        // 局前进入房间 (无强制分先，默认 RANDOM)
        OnlineController playerA = new OnlineController(serverUrl, roomId, "PlayerA");
        Thread.sleep(150);
        OnlineController playerB = new OnlineController(serverUrl, roomId, "PlayerB");

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (playerA.isGameStarted() && playerB.isGameStarted()) break;
            Thread.sleep(50);
        }
        Assertions.assertTrue(playerA.isGameStarted() && playerB.isGameStarted(), "初始局应成功开局");

        // 模拟第1局走几步
        OnlineController p1 = (playerA.getMyPlayerId() == 1) ? playerA : playerB;
        p1.handleUserAction(GameAction.changeDirMove(Direction.DOWN));
        Thread.sleep(100);

        // 现在进行【局内分先发起新一局 (新一局自然要求分先)】：
        // PlayerA 申请执后 (SECOND)，PlayerB 申请执先 (FIRST)
        AtomicReference<String> newGameNoticeA = new AtomicReference<>();
        AtomicReference<String> newGameNoticeB = new AtomicReference<>();
        playerA.setOnNotification(newGameNoticeA::set);
        playerB.setOnNotification(newGameNoticeB::set);

        playerA.resetGameWithPreference(person.kinman.cogame.core.model.TurnOrderPreference.SECOND);
        Thread.sleep(100);
        playerB.resetGameWithPreference(person.kinman.cogame.core.model.TurnOrderPreference.FIRST);

        // 等待新一局开始并完成状态同步
        long rematchDeadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < rematchDeadline) {
            if (playerB.getGameState().getP1().getR() == 0 && newGameNoticeB.get() != null && newGameNoticeB.get().contains("新一局开战")) {
                break;
            }
            Thread.sleep(50);
        }

        // 验证分先结果：PlayerB 应当为 P1 (先手)，PlayerA 应当为 P2 (后手)
        Assertions.assertEquals(1, playerB.getMyPlayerId(), "局内选执先的 PlayerB 应当成为新局 P1");
        Assertions.assertEquals(2, playerA.getMyPlayerId(), "局内选执后的 PlayerA 应当成为新局 P2");

        // 验证棋盘已成功重置
        Assertions.assertEquals(0, playerB.getGameState().getP1().getR(), "新一局 P1 应当在原点 (0,0)");
        Assertions.assertEquals(0, playerB.getGameState().getP1().getC(), "新一局 P1 应当在原点 (0,0)");

        // 再次测试：双方在局内均申请执先 (FIRST)
        newGameNoticeA.set(null);
        newGameNoticeB.set(null);
        playerA.resetGameWithPreference(person.kinman.cogame.core.model.TurnOrderPreference.FIRST);
        Thread.sleep(100);
        playerB.resetGameWithPreference(person.kinman.cogame.core.model.TurnOrderPreference.FIRST);

        long rematch2Deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < rematch2Deadline) {
            if (newGameNoticeA.get() != null && newGameNoticeA.get().contains("新一局开战")
                    && newGameNoticeB.get() != null && newGameNoticeB.get().contains("新一局开战")) {
                break;
            }
            Thread.sleep(50);
        }

        // 系统掷骰裁决：双方一人为1一人为2
        Assertions.assertTrue((playerA.getMyPlayerId() == 1 && playerB.getMyPlayerId() == 2)
                           || (playerA.getMyPlayerId() == 2 && playerB.getMyPlayerId() == 1),
                "双方均选先手时，系统掷骰裁定应当有一人获得先手一人获得后手");

        playerA.close();
        playerB.close();
        server.stop();
    }
}
