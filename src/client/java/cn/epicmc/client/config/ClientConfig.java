package cn.epicmc.client.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 客户端配置
 * 从服务器同步的配置信息
 */
public class ClientConfig {
    private static ClientConfig instance;

    // 配置项
    private boolean allowSingleplayer = false;          // 是否允许单人游戏
    private boolean allowEditServers = false;           // 是否允许编辑服务器
    private boolean allowDeleteServers = false;         // 是否允许删除服务器
    private boolean allowAddServers = false;            // 是否允许添加服务器
    private String welcomeMessage = "";                 // 欢迎消息
    private List<ServerEntry> allowedServers = new CopyOnWriteArrayList<>();  // 允许的服务器列表

    private ClientConfig() {}

    public static ClientConfig getInstance() {
        if (instance == null) {
            instance = new ClientConfig();
        }
        return instance;
    }

    // Getter and Setter
    public boolean isAllowSingleplayer() {
        return allowSingleplayer;
    }

    public void setAllowSingleplayer(boolean allowSingleplayer) {
        this.allowSingleplayer = allowSingleplayer;
    }

    public boolean isAllowEditServers() {
        return allowEditServers;
    }

    public void setAllowEditServers(boolean allowEditServers) {
        this.allowEditServers = allowEditServers;
    }

    public boolean isAllowDeleteServers() {
        return allowDeleteServers;
    }

    public void setAllowDeleteServers(boolean allowDeleteServers) {
        this.allowDeleteServers = allowDeleteServers;
    }

    public boolean isAllowAddServers() {
        return allowAddServers;
    }

    public void setAllowAddServers(boolean allowAddServers) {
        this.allowAddServers = allowAddServers;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public List<ServerEntry> getAllowedServers() {
        return new ArrayList<>(allowedServers);
    }

    public void setAllowedServers(List<ServerEntry> servers) {
        this.allowedServers.clear();
        this.allowedServers.addAll(servers);
    }

    public void addServer(ServerEntry server) {
        this.allowedServers.add(server);
    }

    public void clearServers() {
        this.allowedServers.clear();
    }

    /**
     * 服务器条目
     */
    public static class ServerEntry {
        private final String name;
        private final String address;
        private final int port;

        public ServerEntry(String name, String address, int port) {
            this.name = name;
            this.address = address;
            this.port = port;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public String getFullAddress() {
            return port == 25565 ? address : address + ":" + port;
        }
    }
}
