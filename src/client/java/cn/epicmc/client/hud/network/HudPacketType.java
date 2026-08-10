package cn.epicmc.client.hud.network;

/**
 * HUD 数据包类型定义
 * 使用单字节表示，节省带宽
 */
public class HudPacketType {
    // 客户端 → 服务器
    public static final byte HANDSHAKE = 0x01;           // 握手包
    public static final byte KEEP_ALIVE = 0x02;          // 保活包

    // 服务器 → 客户端
    public static final byte PLAYER_DATA = 0x10;         // 玩家数据（血量、弹药等）
    public static final byte TEAM_DATA = 0x11;           // 队伍数据
    public static final byte CAPTURE_POINT = 0x12;       // 占点数据
    public static final byte KILL_FEED = 0x13;           // 击杀反馈
    public static final byte STATUS_MESSAGE = 0x14;      // 状态消息
    public static final byte GAME_STATE = 0x15;          // 游戏状态（时间、模式、分数）
    public static final byte DISCONNECT = 0x16;          // 断开连接

    private HudPacketType() {}
}
