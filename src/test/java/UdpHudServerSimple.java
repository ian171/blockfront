import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * UDP HUD 服务端示例（简化版）
 * 固定端口 25566
 * 仅发送占点数据、状态消息和游戏状态
 *
 * 使用方法:
 * javac UdpHudServerSimple.java
 * java UdpHudServerSimple
 */
public class UdpHudServerSimple {
    private static final int PORT = 25566;
    private static final Random random = new Random();

    // 数据包类型
    private static final byte CAPTURE_POINT = 0x12;
    private static final byte STATUS_MESSAGE = 0x14;
    private static final byte GAME_STATE = 0x15;

    // 模拟数据
    private static float[] captureProgress = {0.0f, 0.5f, 1.0f};
    private static int gameTime = 600;  // 10分钟
    private static int playerScore = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("BlockFront UDP HUD Server (Simplified)");
        System.out.println("========================================");
        System.out.println("Port: " + PORT);
        System.out.println("Sending capture point data...");
        System.out.println("\n按 Ctrl+C 停止服务器\n");

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            socket.setBroadcast(true);

            while (true) {
                // 更新模拟数据
                updateSimulatedData();

                // 广播数据到所有客户端
                sendCapturePoints(socket, broadcastAddress);
                sendGameState(socket, broadcastAddress);

                // 随机发送状态消息
                if (random.nextFloat() < 0.01f) {
                    sendStatusMessage(socket, broadcastAddress);
                }

                Thread.sleep(100);  // 10Hz 更新率
            }
        } catch (Exception e) {
            System.err.println("服务器错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新模拟数据
     */
    private static void updateSimulatedData() {
        // 占点进度变化
        for (int i = 0; i < captureProgress.length; i++) {
            captureProgress[i] += (random.nextFloat() - 0.5f) * 0.02f;
            captureProgress[i] = Math.max(0, Math.min(1, captureProgress[i]));
        }

        // 时间递减
        if (random.nextInt(10) == 0) {
            gameTime = Math.max(0, gameTime - 1);
        }

        // 分数增加
        if (random.nextFloat() < 0.1f) {
            playerScore += random.nextInt(50) + 10;
        }
    }

    /**
     * 发送占点数据
     */
    private static void sendCapturePoints(DatagramSocket socket, InetAddress address) throws IOException {
        String[] points = {"A", "B", "C"};

        for (int i = 0; i < points.length; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(256);
            buffer.put(CAPTURE_POINT);

            byte[] idBytes = points[i].getBytes(StandardCharsets.UTF_8);
            buffer.putShort((short) idBytes.length);
            buffer.put(idBytes);

            byte[] nameBytes = points[i].getBytes(StandardCharsets.UTF_8);
            buffer.putShort((short) nameBytes.length);
            buffer.put(nameBytes);

            buffer.putFloat(captureProgress[i]);

            // 状态（根据进度）
            byte state;
            if (captureProgress[i] < 0.3f) {
                state = 1;  // ENEMY_OWNED
            } else if (captureProgress[i] > 0.7f) {
                state = 0;  // FRIENDLY_OWNED
            } else {
                state = 2;  // NEUTRAL
            }
            buffer.put(state);

            buffer.putInt(random.nextInt(5));  // 占点玩家数

            send(socket, address, buffer);
        }
    }

    /**
     * 发送状态消息
     */
    private static void sendStatusMessage(DatagramSocket socket, InetAddress address) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put(STATUS_MESSAGE);

        String[] messages = {
            "占领了据点 A！",
            "失去了据点 B！",
            "敌方增援到达！",
            "你的小队获得了火力支援！"
        };

        String message = messages[random.nextInt(messages.length)];
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) messageBytes.length);
        buffer.put(messageBytes);

        buffer.put((byte) random.nextInt(4));  // 消息类型
        buffer.putInt(3000);  // 持续时间 3秒

        send(socket, address, buffer);
        System.out.println("[消息] " + message);
    }

    /**
     * 发送游戏状态
     */
    private static void sendGameState(DatagramSocket socket, InetAddress address) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put(GAME_STATE);

        String gameMode = "征服模式";
        byte[] gameModeBytes = gameMode.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) gameModeBytes.length);
        buffer.put(gameModeBytes);

        buffer.putInt(gameTime);
        buffer.putInt(playerScore);

        send(socket, address, buffer);
    }

    /**
     * 发送数据包
     */
    private static void send(DatagramSocket socket, InetAddress address, ByteBuffer buffer) throws IOException {
        byte[] data = new byte[buffer.position()];
        buffer.flip();
        buffer.get(data);

        DatagramPacket packet = new DatagramPacket(data, data.length, address, PORT);
        socket.send(packet);
    }
}
