import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * BlockFront 配置服务器示例
 * 这是一个简单的配置服务器实现，用于向客户端提供配置和服务器列表
 *
 * 使用方法:
 * javac ConfigServerExample.java
 * java ConfigServerExample [port]
 */
public class ConfigServerExample {
    private static final int DEFAULT_PORT = 25555;

    // 配置项
    private static boolean allowSingleplayer = false;
    private static boolean allowEditServers = false;
    private static boolean allowDeleteServers = false;
    private static boolean allowAddServers = false;
    private static String welcomeMessage = "欢迎来到游戏服务器!";

    // 服务器列表
    private static List<ServerEntry> servers = new ArrayList<>();

    static {
        // 添加示例服务器
        servers.add(new ServerEntry("MagicPixel","mc.epicmc.cn",25565));
        servers.add(new ServerEntry("Localhost","127.0.0.1",25565));
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + DEFAULT_PORT);
            }
        }

        System.out.println("BlockFront Config Server starting on port " + port);
        System.out.println("Configuration:");
        System.out.println("  Allow Singleplayer: " + allowSingleplayer);
        System.out.println("  Allow Edit: " + allowEditServers);
        System.out.println("  Allow Delete: " + allowDeleteServers);
        System.out.println("  Allow Add: " + allowAddServers);
        System.out.println("  Welcome Message: " + welcomeMessage);
        System.out.println("\nServers:");
        for (ServerEntry server : servers) {
            System.out.println("  - " + server.name + " -> " + server.address + ":" + server.port);
        }
        System.out.println("\nWaiting for connections...\n");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getInetAddress());

                // 为每个客户端创建新线程
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket client) {
        try (DataInputStream input = new DataInputStream(client.getInputStream());
             DataOutputStream output = new DataOutputStream(client.getOutputStream())) {

            while (!client.isClosed()) {
                // 读取数据包
                byte type = input.readByte();
                int length = input.readUnsignedShort();
                byte[] payload = new byte[length];
                input.readFully(payload);

                System.out.println("Received packet type: 0x" + String.format("%02X", type) + ", length: " + length);

                // 处理请求
                switch (type) {
                    case 0x01: // REQUEST_CONFIG
                        sendConfigResponse(output);
                        break;
                    case 0x02: // REQUEST_SERVERS
                        sendServersResponse(output);
                        break;
                    case 0x03: // HEARTBEAT
                        sendHeartbeatAck(output);
                        break;
                    default:
                        System.out.println("Unknown packet type: 0x" + String.format("%02X", type));
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }

    /**
     * 发送配置响应
     * 格式: [type=0x11] [length] [flags] [welcome_message]
     */
    private static void sendConfigResponse(DataOutputStream output) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(buffer);

        // 构建flags字节
        byte flags = 0;
        if (allowSingleplayer) flags |= 0x01;
        if (allowEditServers) flags |= 0x02;
        if (allowDeleteServers) flags |= 0x04;
        if (allowAddServers) flags |= 0x08;

        data.writeByte(flags);

        // 写入欢迎消息
        byte[] messageBytes = welcomeMessage.getBytes(StandardCharsets.UTF_8);
        data.writeShort(messageBytes.length);
        data.write(messageBytes);

        // 发送数据包
        byte[] payload = buffer.toByteArray();
        output.writeByte(0x11); // CONFIG_RESPONSE
        output.writeShort(payload.length);
        output.write(payload);
        output.flush();

        System.out.println("Sent config response");
    }

    /**
     * 发送服务器列表响应
     * 格式: [type=0x12] [length] [count] [entries...]
     * entry: [name_length] [name] [address_length] [address] [port]
     */
    private static void sendServersResponse(DataOutputStream output) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(buffer);

        // 写入服务器数量
        data.writeByte(servers.size());

        // 写入每个服务器
        for (ServerEntry server : servers) {
            // 写入名称
            byte[] nameBytes = server.name.getBytes(StandardCharsets.UTF_8);
            data.writeShort(nameBytes.length);
            data.write(nameBytes);

            // 写入地址
            byte[] addressBytes = server.address.getBytes(StandardCharsets.UTF_8);
            data.writeShort(addressBytes.length);
            data.write(addressBytes);

            // 写入端口
            data.writeShort(server.port);
        }

        // 发送数据包
        byte[] payload = buffer.toByteArray();
        output.writeByte(0x12); // SERVERS_RESPONSE
        output.writeShort(payload.length);
        output.write(payload);
        output.flush();

        System.out.println("Sent servers response (" + servers.size() + " servers)");
    }

    /**
     * 发送心跳确认
     */
    private static void sendHeartbeatAck(DataOutputStream output) throws IOException {
        output.writeByte(0x13); // HEARTBEAT_ACK
        output.writeShort(0);   // 无payload
        output.flush();
        System.out.println("Sent heartbeat ack");
    }

    static class ServerEntry {
        String name;
        String address;
        int port;

        ServerEntry(String name, String address, int port) {
            this.name = name;
            this.address = address;
            this.port = port;
        }
    }
}
