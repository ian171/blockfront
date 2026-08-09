import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * UDP HUD 测试服务器
 * 模拟游戏服务器发送战场数据
 *
 * 使用方法:
 * javac UdpHudServerExample.java
 * java UdpHudServerExample [port]
 */
public class UdpHudServerExample {
    private static final int DEFAULT_PORT = 25566;
    private static final Random random = new Random();

    // 数据包类型
    private static final byte PLAYER_DATA = 0x10;
    private static final byte TEAM_DATA = 0x11;
    private static final byte CAPTURE_POINT = 0x12;
    private static final byte KILL_FEED = 0x13;
    private static final byte STATUS_MESSAGE = 0x14;
    private static final byte GAME_STATE = 0x15;

    // 模拟数据
    private static float playerHealth = 100.0f;
    private static int friendlyTickets = 100;
    private static int enemyTickets = 100;
    private static float[] captureProgress = {0.0f, 0.5f, 1.0f};
    private static int gameTime = 600;  // 10分钟
    private static int playerScore = 0;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + DEFAULT_PORT);
            }
        }

        System.out.println("========================================");
        System.out.println("BlockFront UDP HUD Test Server");
        System.out.println("========================================");
        System.out.println("Port: " + port);
        System.out.println("Sending test data to connected clients...");
        System.out.println("\n按 Ctrl+C 停止服务器\n");

        try (DatagramSocket socket = new DatagramSocket(port)) {
            InetAddress clientAddress = null;
            int clientPort = 0;

            while (true) {
                // 等待客户端连接（握手包）
                if (clientAddress == null) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    try {
                        socket.setSoTimeout(1000);
                        socket.receive(packet);
                        clientAddress = packet.getAddress();
                        clientPort = packet.getPort();
                        System.out.println("[连接] 客户端: " + clientAddress + ":" + clientPort);
                    } catch (IOException e) {
                        // 超时，继续等待
                        continue;
                    }
                }

                // 发送数据到客户端
                try {
                    sendGameData(socket, clientAddress, clientPort);
                    Thread.sleep(100);  // 10Hz 更新率
                } catch (InterruptedException e) {
                    break;
                } catch (IOException e) {
                    System.err.println("[错误] 发送数据失败: " + e.getMessage());
                    clientAddress = null;  // 重新等待连接
                }
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 发送游戏数据
     */
    private static void sendGameData(DatagramSocket socket, InetAddress address, int port) throws IOException {
        // 更新模拟数据
        updateSimulatedData();

        // 发送各类数据包
        sendPlayerData(socket, address, port);
        sendTeamData(socket, address, port);
        sendCapturePoints(socket, address, port);
        sendGameState(socket, address, port);

        // 随机发送击杀反馈
        if (random.nextFloat() < 0.05f) {  // 5% 概率
            sendKillFeed(socket, address, port);
        }

        // 随机发送状态消息
        if (random.nextFloat() < 0.01f) {  // 1% 概率
            sendStatusMessage(socket, address, port);
        }
    }

    /**
     * 更新模拟数据
     */
    private static void updateSimulatedData() {
        // 血量波动
        playerHealth += (random.nextFloat() - 0.5f) * 5;
        playerHealth = Math.max(20, Math.min(100, playerHealth));

        // 票数减少
        if (random.nextFloat() < 0.05f) {
            friendlyTickets = Math.max(0, friendlyTickets - 1);
            enemyTickets = Math.max(0, enemyTickets - 1);
        }

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
     * 发送玩家数据包
     */
    private static void sendPlayerData(DatagramSocket socket, InetAddress address, int port) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put(PLAYER_DATA);
        buffer.putFloat(playerHealth);  // health
        buffer.putFloat(100.0f);        // maxHealth
        buffer.putFloat(random.nextFloat() * 50);  // armor
        buffer.putInt(random.nextInt(31));  // ammo
        buffer.putInt(random.nextInt(121));  // ammoReserve

        String weaponName = "AK-47";
        byte[] weaponBytes = weaponName.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) weaponBytes.length);
        buffer.put(weaponBytes);

        send(socket, address, port, buffer);
    }

    /**
     * 发送队伍数据包
     */
    private static void sendTeamData(DatagramSocket socket, InetAddress address, int port) throws IOException {
        // 我方队伍
        sendTeam(socket, address, port, true, "蓝队", 16, 32, friendlyTickets);
        // 敌方队伍
        sendTeam(socket, address, port, false, "红队", 14, 32, enemyTickets);
    }

    private static void sendTeam(DatagramSocket socket, InetAddress address, int port,
                                  boolean isFriendly, String name, int playerCount,
                                  int maxPlayers, int tickets) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put(TEAM_DATA);
        buffer.put((byte) (isFriendly ? 1 : 0));

        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) nameBytes.length);
        buffer.put(nameBytes);

        buffer.putInt(playerCount);
        buffer.putInt(maxPlayers);
        buffer.putInt(tickets);

        send(socket, address, port, buffer);
    }

    /**
     * 发送占点数据
     */
    private static void sendCapturePoints(DatagramSocket socket, InetAddress address, int port) throws IOException {
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

            send(socket, address, port, buffer);
        }
    }

    /**
     * 发送击杀反馈
     */
    private static void sendKillFeed(DatagramSocket socket, InetAddress address, int port) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put(KILL_FEED);

        String[] players = {"PlayerA", "PlayerB", "PlayerC", "PlayerD", "敌人1", "敌人2"};
        String[] weapons = {"AK-47", "M4A1", "AWP", "手榴弹", "刀"};

        String killer = players[random.nextInt(players.length)];
        String victim = players[random.nextInt(players.length)];
        String weapon = weapons[random.nextInt(weapons.length)];

        byte[] killerBytes = killer.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) killerBytes.length);
        buffer.put(killerBytes);

        byte[] victimBytes = victim.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) victimBytes.length);
        buffer.put(victimBytes);

        byte[] weaponBytes = weapon.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) weaponBytes.length);
        buffer.put(weaponBytes);

        buffer.put((byte) (random.nextFloat() < 0.2f ? 1 : 0));  // isHeadshot
        buffer.put((byte) (random.nextFloat() < 0.5f ? 1 : 0));  // isFriendly

        send(socket, address, port, buffer);
        System.out.println("[击杀] " + killer + " -> " + victim + " (" + weapon + ")");
    }

    /**
     * 发送状态消息
     */
    private static void sendStatusMessage(DatagramSocket socket, InetAddress address, int port) throws IOException {
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

        send(socket, address, port, buffer);
        System.out.println("[消息] " + message);
    }

    /**
     * 发送游戏状态
     */
    private static void sendGameState(DatagramSocket socket, InetAddress address, int port) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put(GAME_STATE);

        String gameMode = "征服模式";
        byte[] gameModeBytes = gameMode.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) gameModeBytes.length);
        buffer.put(gameModeBytes);

        buffer.putInt(gameTime);
        buffer.putInt(playerScore);

        send(socket, address, port, buffer);
    }

    /**
     * 发送数据包
     */
    private static void send(DatagramSocket socket, InetAddress address, int port, ByteBuffer buffer) throws IOException {
        byte[] data = new byte[buffer.position()];
        buffer.flip();
        buffer.get(data);

        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }
}
