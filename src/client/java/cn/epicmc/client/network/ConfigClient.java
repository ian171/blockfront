package cn.epicmc.client.network;

import cn.epicmc.BlockFront;
import cn.epicmc.client.config.ClientConfig;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 配置服务器客户端
 * 负责与配置服务器通信，获取配置和服务器列表
 */
public class ConfigClient {
    private static ConfigClient instance;

    private String configServerHost = "127.0.0.1";  // 配置服务器地址
    private int configServerPort = 25555;            // 配置服务器端口
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private boolean connected = false;

    private ConfigClient() {}

    public static ConfigClient getInstance() {
        if (instance == null) {
            instance = new ConfigClient();
        }
        return instance;
    }

    /**
     * 设置配置服务器地址
     */
    public void setConfigServer(String host, int port) {
        this.configServerHost = host;
        this.configServerPort = port;
    }

    /**
     * 连接到配置服务器
     */
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BlockFront.LOGGER.info("Connecting to config server: {}:{}", configServerHost, configServerPort);
                socket = new Socket(configServerHost, configServerPort);
                socket.setSoTimeout(5000);  // 5秒超时
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
                connected = true;
                BlockFront.LOGGER.info("Connected to config server");
                return true;
            } catch (IOException e) {
                BlockFront.LOGGER.error("Failed to connect to config server", e);
                connected = false;
                return false;
            }
        });
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            connected = false;
            BlockFront.LOGGER.info("Disconnected from config server");
        } catch (IOException e) {
            BlockFront.LOGGER.error("Error disconnecting from config server", e);
        }
    }

    /**
     * 请求配置
     */
    public CompletableFuture<Boolean> requestConfig() {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) {
                BlockFront.LOGGER.warn("Not connected to config server");
                return false;
            }

            try {
                // 发送请求配置包
                PacketBuffer packet = new PacketBuffer(PacketType.REQUEST_CONFIG);
                byte[] data = packet.build();
                output.write(data);
                output.flush();

                // 读取响应
                byte[] responseData = readPacket();
                if (responseData == null) {
                    return false;
                }

                PacketBuffer.Reader reader = new PacketBuffer.Reader(responseData);
                if (reader.getPacketType() != PacketType.CONFIG_RESPONSE) {
                    BlockFront.LOGGER.warn("Unexpected packet type: {}", reader.getPacketType());
                    return false;
                }

                // 解析配置
                parseConfig(reader);
                BlockFront.LOGGER.info("Config received and applied");
                return true;

            } catch (IOException e) {
                BlockFront.LOGGER.error("Error requesting config", e);
                return false;
            }
        });
    }

    /**
     * 请求服务器列表
     */
    public CompletableFuture<Boolean> requestServers() {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) {
                BlockFront.LOGGER.warn("Not connected to config server");
                return false;
            }

            try {
                // 发送请求服务器列表包
                PacketBuffer packet = new PacketBuffer(PacketType.REQUEST_SERVERS);
                byte[] data = packet.build();
                output.write(data);
                output.flush();

                // 读取响应
                byte[] responseData = readPacket();
                if (responseData == null) {
                    return false;
                }

                PacketBuffer.Reader reader = new PacketBuffer.Reader(responseData);
                if (reader.getPacketType() != PacketType.SERVERS_RESPONSE) {
                    BlockFront.LOGGER.warn("Unexpected packet type: {}", reader.getPacketType());
                    return false;
                }

                // 解析服务器列表
                parseServers(reader);
                BlockFront.LOGGER.info("Server list received and applied");
                return true;

            } catch (IOException e) {
                BlockFront.LOGGER.error("Error requesting servers", e);
                return false;
            }
        });
    }

    /**
     * 读取一个完整的数据包
     */
    private byte[] readPacket() throws IOException {
        // 先读取包头（3字节：type + length）
        byte type = input.readByte();
        int length = input.readUnsignedShort();

        // 读取数据部分
        byte[] payload = new byte[length];
        input.readFully(payload);

        // 重新组装完整包
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(type);
        dos.writeShort(length);
        dos.write(payload);
        dos.flush();

        return baos.toByteArray();
    }

    /**
     * 解析配置数据
     * 格式: [1 byte: flags] [string: welcome message]
     * flags: bit0=allowSingleplayer, bit1=allowEdit, bit2=allowDelete, bit3=allowAdd
     */
    private void parseConfig(PacketBuffer.Reader reader) throws IOException {
        ClientConfig config = ClientConfig.getInstance();

        byte flags = reader.readByte();
        config.setAllowSingleplayer((flags & 0x01) != 0);
        config.setAllowEditServers((flags & 0x02) != 0);
        config.setAllowDeleteServers((flags & 0x04) != 0);
        config.setAllowAddServers((flags & 0x08) != 0);

        String welcomeMessage = reader.readString();
        config.setWelcomeMessage(welcomeMessage);

        BlockFront.LOGGER.info("Config applied: singleplayer={}, edit={}, delete={}, add={}",
            config.isAllowSingleplayer(),
            config.isAllowEditServers(),
            config.isAllowDeleteServers(),
            config.isAllowAddServers());
    }

    /**
     * 解析服务器列表
     * 格式: [1 byte: count] [entries...]
     * entry: [string: name] [string: address] [2 bytes: port]
     */
    private void parseServers(PacketBuffer.Reader reader) throws IOException {
        ClientConfig config = ClientConfig.getInstance();
        config.clearServers();

        int count = reader.readByte() & 0xFF;
        List<ClientConfig.ServerEntry> servers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String name = reader.readString();
            String address = reader.readString();
            int port = reader.readShort() & 0xFFFF;

            servers.add(new ClientConfig.ServerEntry(name, address, port));
            BlockFront.LOGGER.info("Server added: {} -> {}:{}", name, address, port);
        }

        config.setAllowedServers(servers);
    }

    /**
     * 同步获取配置和服务器列表
     */
    public CompletableFuture<Boolean> syncAll() {
        return connect()
            .thenCompose(success -> {
                if (!success) {
                    return CompletableFuture.completedFuture(false);
                }
                return requestConfig();
            })
            .thenCompose(success -> {
                if (!success) {
                    return CompletableFuture.completedFuture(false);
                }
                return requestServers();
            })
            .whenComplete((success, throwable) -> {
                if (!success || throwable != null) {
                    BlockFront.LOGGER.error("Failed to sync config");
                }
                disconnect();
            });
    }

    public boolean isConnected() {
        return connected;
    }
}
