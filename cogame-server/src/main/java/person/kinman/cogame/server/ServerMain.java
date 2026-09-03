package person.kinman.cogame.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import person.kinman.cogame.server.net.CoGameWebSocketServer;

/**
 * 服务端主启动入口
 */
public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
    public static final int DEFAULT_PORT = 8088;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("提供的端口号无效，使用默认端口: {}", DEFAULT_PORT);
            }
        }

        CoGameWebSocketServer server = new CoGameWebSocketServer(port);
        server.start();

        logger.info("==============================================");
        logger.info("  COGame 联机对战服务器已启动！");
        logger.info("  监听端口: {}", port);
        logger.info("  协议格式: WebSocket (JSON WsMessage)");
        logger.info("==============================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("正在关闭服务器...");
                server.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.info("服务被中断退出");
        }
    }
}
