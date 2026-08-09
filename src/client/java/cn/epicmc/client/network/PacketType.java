package cn.epicmc.client.network;

/**
 * 数据包类型定义
 * 使用单字节表示，节省带宽
 */
public class PacketType {
    // 客户端 -> 服务器
    public static final byte REQUEST_CONFIG = 0x01;      // 请求配置
    public static final byte REQUEST_SERVERS = 0x02;     // 请求服务器列表
    public static final byte HEARTBEAT = 0x03;           // 心跳包

    // 服务器 -> 客户端
    public static final byte CONFIG_RESPONSE = 0x11;     // 配置响应
    public static final byte SERVERS_RESPONSE = 0x12;    // 服务器列表响应
    public static final byte HEARTBEAT_ACK = 0x13;       // 心跳确认
    public static final byte KICK = 0x14;                // 踢出客户端
}
