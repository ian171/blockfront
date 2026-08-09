# BlockFront HUD 系统使用指南

## 概述

BlockFront 现在包含一个战地风格的精美 HUD 系统，通过 UDP 协议实时接收服务器战场数据，显示玩家状态、队伍信息、占点进度、击杀反馈等。

---

## 🎨 HUD 界面布局

```
┌─────────────────────────────────────────────────────────────┐
│                    [时间 模式 分数]                           │  ← 顶部信息栏
│                                                              │
│  ┌─────────┐         [A] [B] [C]                   ┌──────┐ │
│  │ 蓝队    │        占点进度条                       │ 击杀 │ │
│  │ 16/32   │                                        │ 反馈 │ │
│  │ 票数:100│                                        │ 列表 │ │
│  ├─────────┤                                        └──────┘ │
│  │ 红队    │                                                 │
│  │ 14/32   │                                                 │
│  │ 票数:95 │                  游戏区域                        │
│  └─────────┘                                                 │
│                                                              │
│                                                              │
│  ┌──────────────┐                         ┌──────────────┐  │
│  │ 生命值        │                         │ AK-47        │  │
│  │ ████████░░   │                         │   30 / 120   │  │ ← 底部状态栏
│  │ 100 / 100    │                         │              │  │
│  └──────────────┘                         └──────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### HUD 组件

1. **顶部信息栏** - 游戏模式、剩余时间、个人分数
2. **队伍信息（左上）** - 我方/敌方人数、票数
3. **占点进度（顶部中央）** - A/B/C 点占领状态和进度
4. **击杀反馈（右上）** - 最近的击杀信息
5. **血条（左下）** - 生命值、护甲值
6. **弹药（右下）** - 当前武器、弹药数量
7. **状态消息（中央）** - 重要游戏事件提示

---

## 🚀 快速开始

### 1. 启动 UDP 测试服务器

```bash
# 编译测试服务器
javac UdpHudServerExample.java

# 运行测试服务器（默认端口 25566）
java UdpHudServerExample

# 或使用自定义端口
java UdpHudServerExample 12345
```

### 2. 配置客户端

在 Minecraft 启动器的 JVM 参数中添加：

```
-Dblockfront.hud.host=127.0.0.1
-Dblockfront.hud.port=25566
```

### 3. 启动游戏

进入游戏后，HUD 会自动连接到服务器并显示战场数据。

---

## 📡 UDP 协议说明

### 数据包格式

所有 UDP 数据包使用以下格式：

```
[Type: 1 byte] [Data: variable length]
```

### 数据包类型

| Type | 名称 | 方向 | 说明 |
|------|------|------|------|
| `0x01` | HANDSHAKE | C→S | 客户端握手 |
| `0x02` | KEEP_ALIVE | C→S | 保活包 |
| `0x10` | PLAYER_DATA | S→C | 玩家数据（血量、弹药） |
| `0x11` | TEAM_DATA | S→C | 队伍数据 |
| `0x12` | CAPTURE_POINT | S→C | 占点数据 |
| `0x13` | KILL_FEED | S→C | 击杀反馈 |
| `0x14` | STATUS_MESSAGE | S→C | 状态消息 |
| `0x15` | GAME_STATE | S→C | 游戏状态 |

### 详细格式

#### PLAYER_DATA (0x10)
```
[type:1] [health:4] [maxHealth:4] [armor:4] [ammo:4] [ammoReserve:4] 
[weaponNameLen:2] [weaponName:N]
```

#### TEAM_DATA (0x11)
```
[type:1] [isFriendly:1] [nameLen:2] [name:N] [playerCount:4] 
[maxPlayers:4] [tickets:4]
```

#### CAPTURE_POINT (0x12)
```
[type:1] [idLen:2] [id:N] [nameLen:2] [name:N] [progress:4] 
[state:1] [capturingPlayers:4]
```

#### KILL_FEED (0x13)
```
[type:1] [killerLen:2] [killer:N] [victimLen:2] [victim:N] 
[weaponLen:2] [weapon:N] [isHeadshot:1] [isFriendly:1]
```

#### STATUS_MESSAGE (0x14)
```
[type:1] [messageLen:2] [message:N] [messageType:1] [duration:4]
```

#### GAME_STATE (0x15)
```
[type:1] [gameModeLen:2] [gameMode:N] [remainingTime:4] [score:4]
```

---

## 🎮 服务器集成示例

### Java 服务器示例

```java
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class GameServer {
    private DatagramSocket socket;
    private InetAddress clientAddress;
    private int clientPort;

    public void sendPlayerData(float health, float maxHealth, int ammo) {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put((byte) 0x10);  // PLAYER_DATA
        buffer.putFloat(health);
        buffer.putFloat(maxHealth);
        buffer.putFloat(0);  // armor
        buffer.putInt(ammo);
        buffer.putInt(120);  // ammoReserve
        
        String weapon = "AK-47";
        byte[] weaponBytes = weapon.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) weaponBytes.length);
        buffer.put(weaponBytes);
        
        sendPacket(buffer);
    }

    public void sendKillFeed(String killer, String victim, String weapon) {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put((byte) 0x13);  // KILL_FEED
        
        writeString(buffer, killer);
        writeString(buffer, victim);
        writeString(buffer, weapon);
        buffer.put((byte) 0);  // isHeadshot
        buffer.put((byte) 1);  // isFriendly
        
        sendPacket(buffer);
    }

    private void writeString(ByteBuffer buffer, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) bytes.length);
        buffer.put(bytes);
    }

    private void sendPacket(ByteBuffer buffer) {
        byte[] data = new byte[buffer.position()];
        buffer.flip();
        buffer.get(data);
        
        try {
            DatagramPacket packet = new DatagramPacket(
                data, data.length, clientAddress, clientPort
            );
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Python 服务器示例

```python
import socket
import struct

class HudServer:
    def __init__(self, port=25566):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(('0.0.0.0', port))
        self.client = None

    def send_player_data(self, health, max_health, ammo):
        if not self.client:
            return
            
        weapon = "AK-47"
        weapon_bytes = weapon.encode('utf-8')
        
        data = struct.pack(
            '!Bfffff II H',
            0x10,  # PLAYER_DATA
            health,
            max_health,
            0.0,  # armor
            ammo,
            120,  # ammoReserve
            len(weapon_bytes)
        ) + weapon_bytes
        
        self.sock.sendto(data, self.client)

    def send_kill_feed(self, killer, victim, weapon):
        if not self.client:
            return
            
        killer_bytes = killer.encode('utf-8')
        victim_bytes = victim.encode('utf-8')
        weapon_bytes = weapon.encode('utf-8')
        
        data = struct.pack('!B', 0x13)  # KILL_FEED
        data += struct.pack('!H', len(killer_bytes)) + killer_bytes
        data += struct.pack('!H', len(victim_bytes)) + victim_bytes
        data += struct.pack('!H', len(weapon_bytes)) + weapon_bytes
        data += struct.pack('!BB', 0, 1)  # isHeadshot, isFriendly
        
        self.sock.sendto(data, self.client)
```

---

## 🎨 HUD 设计特点

### 视觉设计

- **战地风格** - 参考《战地》系列游戏界面
- **现代扁平化** - 简洁的线条和色块
- **高对比度** - 黑色背景 + 明亮图标
- **颜色编码**：
  - 🔵 蓝色 - 我方/友军
  - 🔴 红色 - 敌方
  - 🟢 绿色 - 健康/成功
  - 🟠 橙色 - 警告
  - ⚪ 白色 - 中性信息

### 动画效果

- **血条动画** - 平滑过渡，不突变
- **脉冲警告** - 血量低时红色闪烁
- **弹药警告** - 弹药不足时背景闪烁
- **击杀反馈** - 渐入渐出效果
- **占点进度** - 实时动态更新

### 用户体验

- **非阻塞** - 不遮挡游戏视野
- **信息密度** - 重要信息突出显示
- **实时更新** - 10Hz 更新率，流畅显示
- **自适应** - 根据屏幕尺寸调整布局

---

## 🔧 自定义 HUD

### 修改颜色

编辑 `BattlefieldHudRenderer.java`:

```java
private static final int COLOR_FRIENDLY = 0x4A90E2;    // 我方 - 蓝色
private static final int COLOR_ENEMY = 0xE74C3C;       // 敌方 - 红色
private static final int COLOR_SUCCESS = 0x2ECC71;     // 成功 - 绿色
private static final int COLOR_WARNING = 0xF39C12;     // 警告 - 橙色
```

### 调整布局

修改各组件的位置：

```java
// 血条位置（左下角）
int x = 10;
int y = screenHeight - 80;

// 弹药位置（右下角）
int x = screenWidth - 210;
int y = screenHeight - 80;
```

### 禁用特定组件

在 `render()` 方法中注释掉不需要的组件：

```java
// renderKillFeed(context, screenWidth, screenHeight);  // 禁用击杀反馈
```

---

## 📊 性能优化

### 网络带宽

- **UDP 无连接** - 低延迟，适合实时数据
- **数据包大小** - 平均 50-200 字节/包
- **更新频率** - 建议 10-20Hz
- **总带宽** - 约 5-20 KB/s

### 渲染性能

- **缓存文本渲染** - 避免重复计算
- **批量绘制** - 减少 Draw Call
- **条件渲染** - 仅渲染可见组件
- **LOD** - 根据距离调整细节

---

## 🐛 故障排除

### HUD 不显示

1. 检查服务器是否运行
   ```bash
   netstat -an | grep 25566
   ```

2. 检查客户端日志
   ```
   logs/latest.log
   查找 "UDP HUD client"
   ```

3. 验证 JVM 参数
   ```
   -Dblockfront.hud.host=127.0.0.1
   -Dblockfront.hud.port=25566
   ```

### 数据不更新

1. 确认收到数据包
   - 服务器应显示 "发送数据..."
   - 客户端日志应有 "接收数据包" 消息

2. 检查防火墙
   ```bash
   # Windows
   netsh advfirewall firewall add rule name="BlockFront HUD" dir=in action=allow protocol=UDP localport=25566

   # Linux
   sudo ufw allow 25566/udp
   ```

### 性能问题

1. 降低更新频率
   ```java
   Thread.sleep(100);  // 改为 200 或更高
   ```

2. 减少数据包大小
   - 只发送变化的数据
   - 使用增量更新

---

## 📚 完整示例

查看项目中的示例文件：

- `UdpHudServerExample.java` - 测试服务器
- `HudDataManager.java` - 数据管理
- `BattlefieldHudRenderer.java` - HUD 渲染
- `UdpHudClient.java` - UDP 客户端

---

## 🎯 未来计划

- [ ] 可配置的 HUD 布局
- [ ] 自定义颜色主题
- [ ] 小地图显示
- [ ] 3D 方向指示器
- [ ] 伤害数字弹出
- [ ] 技能冷却显示
- [ ] 队友血条
- [ ] 语音聊天指示器

---

## 📝 许可证

与主项目相同，采用 CC0-1.0 许可证

---

**享受你的战地风格 HUD！** 🎮✨
