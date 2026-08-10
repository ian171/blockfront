package cn.epicmc.client.hud.network;

import cn.epicmc.BlockFront;
import cn.epicmc.client.hud.GameModeType;
import cn.epicmc.client.hud.HudDataManager;
import cn.epicmc.client.hud.HudDataManager.*;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP 网络客户端
 * 接收服务器发送的战场数据包
 * 固定端口: 25566
 */
public class UdpHudClient {
    private static UdpHudClient instance;
    private DatagramSocket socket;
    private Thread receiveThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // 固定配置
    private static final int SERVER_PORT = 25566;

    private final HudDataManager dataManager = HudDataManager.getInstance();

    private UdpHudClient() {}

    public static UdpHudClient getInstance() {
        if (instance == null) {
            instance = new UdpHudClient();
        }
        return instance;
    }

    /**
     * 启动 UDP 客户端
     */
    public void start() {
        if (running.get()) {
            BlockFront.LOGGER.warn("UDP HUD client is already running");
            return;
        }

        try {
            socket = new DatagramSocket(SERVER_PORT);
            socket.setSoTimeout(100);  // 100ms 超时
            running.set(true);

            receiveThread = new Thread(this::receiveLoop, "HUD-UDP-Receiver");
            receiveThread.setDaemon(true);
            receiveThread.start();

            BlockFront.LOGGER.info("UDP HUD client started, listening on port {}", SERVER_PORT);

        } catch (SocketException e) {
            BlockFront.LOGGER.error("Failed to start UDP client on port {}", SERVER_PORT, e);
            running.set(false);
        }
    }

    /**
     * 停止 UDP 客户端
     */
    public void stop() {
        if (!running.get()) {
            return;
        }

        running.set(false);

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        if (receiveThread != null) {
            try {
                receiveThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        BlockFront.LOGGER.info("UDP HUD client stopped");
    }

    /**
     * 接收循环
     */
    private void receiveLoop() {
        byte[] buffer = new byte[4096];

        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // 处理数据包
                processPacket(packet.getData(), packet.getLength());

            } catch (SocketTimeoutException e) {
                // 超时是正常的，继续循环
            } catch (IOException e) {
                if (running.get()) {
                    BlockFront.LOGGER.error("Error receiving packet", e);
                }
            }
        }
    }

    /**
     * 处理数据包
     */
    private void processPacket(byte[] data, int length) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);
            byte packetType = buffer.get();

            switch (packetType) {
                case HudPacketType.CAPTURE_POINT:
                    handleCapturePoint(buffer);
                    break;
                case HudPacketType.STATUS_MESSAGE:
                    handleStatusMessage(buffer);
                    break;
                case HudPacketType.GAME_STATE:
                    handleGameState(buffer);
                    break;
                default:
                    BlockFront.LOGGER.debug("Unknown packet type: 0x{}", Integer.toHexString(packetType & 0xFF));
            }
        } catch (Exception e) {
            BlockFront.LOGGER.error("Error processing packet", e);
        }
    }

    /**
     * 处理占点数据包
     * 格式: [type:1] [idLen:2] [id:N] [nameLen:2] [name:N] [progress:4] [state:1] [capturingPlayers:4]
     */
    private void handleCapturePoint(ByteBuffer buffer) {
        try {
            short idLen = buffer.getShort();
            byte[] idBytes = new byte[idLen];
            buffer.get(idBytes);
            String id = new String(idBytes, StandardCharsets.UTF_8);

            short nameLen = buffer.getShort();
            byte[] nameBytes = new byte[nameLen];
            buffer.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);

            float progress = buffer.getFloat();
            byte stateOrdinal = buffer.get();
            int capturingPlayers = buffer.getInt();

            if (stateOrdinal >= 0 && stateOrdinal < CaptureState.values().length) {
                CaptureState state = CaptureState.values()[stateOrdinal];
                dataManager.updateCapturePoint(id, name, progress, state, capturingPlayers);
            }
        } catch (Exception e) {
            BlockFront.LOGGER.error("Error parsing capture point packet", e);
        }
    }

    /**
     * 处理状态消息数据包
     * 格式: [type:1] [messageLen:2] [message:N] [messageType:1] [duration:4]
     */
    private void handleStatusMessage(ByteBuffer buffer) {
        try {
            short messageLen = buffer.getShort();
            byte[] messageBytes = new byte[messageLen];
            buffer.get(messageBytes);
            String message = new String(messageBytes, StandardCharsets.UTF_8);

            byte typeOrdinal = buffer.get();
            int duration = buffer.getInt();

            if (typeOrdinal >= 0 && typeOrdinal < StatusMessageType.values().length) {
                StatusMessageType type = StatusMessageType.values()[typeOrdinal];
                dataManager.setStatusMessage(message, type, duration);
            }
        } catch (Exception e) {
            BlockFront.LOGGER.error("Error parsing status message packet", e);
        }
    }

    /**
     * 处理游戏状态数据包
     * 格式: [type:1] [gameModeLen:2] [gameMode:N] [remainingTime:4] [score:4]
     */
    private void handleGameState(ByteBuffer buffer) {
        try {
            short gameModeLen = buffer.getShort();
            byte[] gameModeBytes = new byte[gameModeLen];
            buffer.get(gameModeBytes);
            String gameMode = new String(gameModeBytes, StandardCharsets.UTF_8);
            GameModeType modernType = GameModeType.valueOf(gameMode);
            if (gameMode.equals("行动模式")){
                modernType = GameModeType.ACTION;
            }else if (gameMode.equals("夺点模式")){
                modernType = GameModeType.CONTEST;
            }
            int remainingTime = buffer.getInt();
            int score = buffer.getInt();

            dataManager.setGameMode(modernType);
            dataManager.setRemainingTime(remainingTime);
            dataManager.setScore(score);
        } catch (Exception e) {
            BlockFront.LOGGER.error("Error parsing game state packet", e);
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
