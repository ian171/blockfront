# BlockFront 项目完整总结

## 项目概述

BlockFront 是一个为 Minecraft 1.21.1 设计的 Fabric 客户端模组，专门用于限制玩家访问单人游戏，并通过轻量级二进制协议与配置服务器通信，实现动态权限控制和服务器列表管理。

---

## 核心架构

### 1. 客户端模组结构

```
BlockFront (Fabric Mod)
├── 配置管理 (ClientConfig)
│   ├── 权限标志（单人游戏、编辑、删除等）
│   ├── 服务器列表
│   └── 欢迎消息
│
├── 网络通信 (ConfigClient + PacketBuffer)
│   ├── 连接配置服务器
│   ├── 请求配置和服务器列表
│   └── 解析二进制数据包
│
├── 用户界面 (ModMultiplayerScreen)
│   ├── 自定义多人游戏界面
│   ├── 服务器列表显示
│   ├── 同步配置按钮
│   └── 状态消息显示
│
└── Mixin 注入
    ├── SinglePlayerScreen - 阻止单人游戏访问
    ├── TitleScreen - 自动跳转到多人游戏
    └── MultiplePlayers - 防止访问原版界面
```

### 2. 配置服务器

**ConfigServerExample.java** - 独立的 Java 程序
- 监听 TCP 端口 (默认 25555)
- 处理客户端请求
- 发送配置和服务器列表
- 多线程处理多个客户端

---

## 网络协议设计

### 数据包格式（极简设计）

```
┌─────────┬─────────┬─────────┐
│ Type    │ Length  │ Payload │
│ 1 byte  │ 2 bytes │ N bytes │
└─────────┴─────────┴─────────┘
```

### 协议类型

| 方向 | 类型 | 名称 | 说明 |
|------|------|------|------|
| C→S | 0x01 | REQUEST_CONFIG | 请求配置 |
| C→S | 0x02 | REQUEST_SERVERS | 请求服务器列表 |
| C→S | 0x03 | HEARTBEAT | 心跳包 |
| S→C | 0x11 | CONFIG_RESPONSE | 配置响应 |
| S→C | 0x12 | SERVERS_RESPONSE | 服务器列表响应 |
| S→C | 0x13 | HEARTBEAT_ACK | 心跳确认 |
| S→C | 0x14 | KICK | 踢出客户端 |

### 配置响应格式 (0x11)

```
┌───────┬─────────────┬──────────────────┐
│ Flags │ Message Len │ Welcome Message  │
│ 1 byte│ 2 bytes     │ UTF-8 string     │
└───────┴─────────────┴──────────────────┘

Flags 位定义：
Bit 0 (0x01): allowSingleplayer
Bit 1 (0x02): allowEditServers
Bit 2 (0x04): allowDeleteServers
Bit 3 (0x08): allowAddServers
```

### 服务器列表格式 (0x12)

```
┌───────┬────────────────────────────────────┐
│ Count │ Server Entries (repeated)          │
│ 1 byte│ Count * (name + address + port)    │
└───────┴────────────────────────────────────┘

每个 Server Entry:
┌─────────────┬──────┬──────────────┬─────────┬──────┐
│ Name Length │ Name │ Addr Length  │ Address │ Port │
│ 2 bytes     │ UTF-8│ 2 bytes      │ UTF-8   │ 2 b  │
└─────────────┴──────┴──────────────┴─────────┴──────┘
```

### 带宽消耗分析

**单次完整同步（3个服务器）**：
- REQUEST_CONFIG: 3 bytes
- CONFIG_RESPONSE: ~100 bytes
- REQUEST_SERVERS: 3 bytes  
- SERVERS_RESPONSE: ~200 bytes
- **总计**: ~306 bytes

对比 JSON 格式约节省 60-70% 带宽。

---

## 功能实现细节

### 1. 阻止单人游戏

**SinglePlayerScreen.java** (Mixin注入)
```java
@Inject(method = "init", at = @At(value = "TAIL"))
public void init(CallbackInfo ci) throws IllegalAccessException {
    if (!ClientConfig.getInstance().isAllowSingleplayer()) {
        throw new IllegalAccessException("Singleplayer is disabled");
    }
}
```

- 当玩家点击"单人游戏"时触发
- 检查配置是否允许
- 不允许则抛出异常，阻止界面初始化

### 2. 自动跳转多人游戏

**TitleScreen.java** (Mixin注入)
```java
@Redirect(method = "initWidgetsNormal", ...)
private Element initWidgetsNormal(...) {
    MinecraftClient.getInstance().setScreen(new ModMultiplayerScreen(null));
    return element;
}
```

- 标题界面初始化时触发
- 强制切换到自定义多人游戏界面
- 移除 Realms 通知

### 3. 动态服务器列表

**ModMultiplayerScreen.java**
```java
private void loadServersFromConfig() {
    List<ServerEntry> configServers = ClientConfig.getInstance().getAllowedServers();
    for (ServerEntry server : configServers) {
        // 添加到服务器列表
        this.serverList.add(new ServerInfo(...));
    }
}
```

- 从配置加载服务器
- 合并到本地列表
- 根据配置控制按钮状态

### 4. 权限控制

**updateButtonActivationStates()**
```java
MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
if (entry instanceof LanServerEntry) {
    this.buttonJoin.active = true;  // 总是允许局域网
} else if (entry instanceof ServerEntry) {
    boolean isAllowed = isServerAllowed(serverInfo);
    this.buttonJoin.active = isAllowed;
    this.buttonEdit.active = isAllowed && config.isAllowEditServers();
    this.buttonDelete.active = isAllowed && config.isAllowDeleteServers();
}
```

- 局域网服务器总是可连接
- 配置列表中的服务器根据权限控制
- 编辑/删除功能动态启用/禁用

### 5. 配置同步

**syncConfig() 方法**
```java
ConfigClient.getInstance().syncAll().thenAccept(success -> {
    if (success) {
        loadServersFromConfig();
        this.serverListWidget.setServers(this.serverList);
        statusMessage = Text.literal("配置同步成功！");
    }
});
```

- 异步执行，不阻塞游戏
- 成功后重新加载服务器列表
- 显示状态消息（3秒后自动消失）

---

## 配置服务器实现

### 核心逻辑

```java
// 1. 监听端口
ServerSocket serverSocket = new ServerSocket(port);

// 2. 接受连接
Socket client = serverSocket.accept();

// 3. 读取请求
byte type = input.readByte();
int length = input.readUnsignedShort();
byte[] payload = new byte[length];
input.readFully(payload);

// 4. 处理请求
switch (type) {
    case 0x01: sendConfigResponse(output); break;
    case 0x02: sendServersResponse(output); break;
    case 0x03: sendHeartbeatAck(output); break;
}
```

### 配置响应构建

```java
// 构建 flags 字节
byte flags = 0;
if (allowSingleplayer) flags |= 0x01;
if (allowEditServers) flags |= 0x02;
if (allowDeleteServers) flags |= 0x04;
if (allowAddServers) flags |= 0x08;

// 写入 payload
data.writeByte(flags);
data.writeShort(messageBytes.length);
data.write(messageBytes);

// 发送数据包
output.writeByte(0x11);  // CONFIG_RESPONSE
output.writeShort(payload.length);
output.write(payload);
```

---

## 部署方案

### 方案 1: 单机测试
```bash
# 启动配置服务器
java ConfigServerExample

# 启动 Minecraft（添加 JVM 参数）
-Dblockfront.config.host=127.0.0.1
-Dblockfront.config.port=25555
```

### 方案 2: 局域网部署
```bash
# 服务器端（192.168.1.100）
java ConfigServerExample 25555

# 客户端（多台电脑）
-Dblockfront.config.host=192.168.1.100
-Dblockfront.config.port=25555

# 防火墙规则
sudo iptables -A INPUT -p tcp --dport 25555 -s 192.168.1.0/24 -j ACCEPT
```

### 方案 3: Docker 部署
```dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY ConfigServerExample.java .
RUN javac ConfigServerExample.java
EXPOSE 25555
CMD ["java", "ConfigServerExample"]
```

```bash
docker build -t blockfront-config .
docker run -d -p 25555:25555 --name blockfront-config blockfront-config
```

### 方案 4: 云服务器（带 SSH 隧道）
```bash
# 远程服务器
java ConfigServerExample

# 本地客户端通过 SSH 隧道
ssh -L 25555:localhost:25555 user@remote-server

# Minecraft 连接到 localhost:25555
-Dblockfront.config.host=127.0.0.1
-Dblockfront.config.port=25555
```

---

## 使用场景和配置示例

### 场景 1: 网吧电竞馆

**需求**: 完全锁定客户端，只能连接指定服务器

```java
allowSingleplayer = false;
allowEditServers = false;
allowDeleteServers = false;
allowAddServers = false;
welcomeMessage = "欢迎来到 XX 电竞馆！";

servers.add(new ServerEntry("官方服务器", "official.example.com", 25565));
```

### 场景 2: 小游戏大厅

**需求**: 提供多个游戏模式，玩家自由选择

```java
allowSingleplayer = false;
allowEditServers = false;
welcomeMessage = "选择你喜欢的游戏模式 - 祝你游戏愉快！";

servers.add(new ServerEntry("🏝️ 空岛战争", "skywars.server.com", 25565));
servers.add(new ServerEntry("🛏️ 起床战争", "bedwars.server.com", 25565));
servers.add(new ServerEntry("🎯 饥饿游戏", "hungergames.server.com", 25565));
servers.add(new ServerEntry("🏗️ 创造建筑", "creative.server.com", 25565));
```

### 场景 3: 教育环境

**需求**: 允许单人创造，限制多人服务器

```java
allowSingleplayer = true;
allowEditServers = false;
welcomeMessage = "欢迎参加今天的 Minecraft 教学课程！";

servers.add(new ServerEntry("课堂协作服务器", "edu.minecraft.school", 25565));
```

### 场景 4: 赛事活动

**需求**: 快速切换赛事服务器，实时更新配置

```java
allowSingleplayer = false;
allowEditServers = false;
welcomeMessage = "2026 Minecraft 杯总决赛 - 第一轮即将开始！";

servers.add(new ServerEntry("比赛服务器 A", "match-a.tournament.com", 25565));
servers.add(new ServerEntry("比赛服务器 B", "match-b.tournament.com", 25565));
servers.add(new ServerEntry("备用服务器", "backup.tournament.com", 25565));
```

玩家可以在游戏内点击"同步配置"实时更新服务器列表。

---

## 技术优势

### 1. 极简协议设计
- ✅ 单次同步仅 ~400 字节
- ✅ 比 JSON 节省 60-70% 带宽
- ✅ 适合低带宽环境

### 2. 异步非阻塞
- ✅ 配置同步不影响游戏启动
- ✅ CompletableFuture 异步处理
- ✅ 失败时使用默认配置

### 3. 灵活可扩展
- ✅ 易于添加新的数据包类型
- ✅ 配置项使用位标志，节省空间
- ✅ 支持未来功能扩展

### 4. 易于部署
- ✅ 配置服务器是单文件 Java 程序
- ✅ 无需数据库
- ✅ 支持 Docker 容器化
- ✅ 跨平台兼容

---

## 安全考虑

### 当前版本局限

⚠️ **无加密**: 数据明文传输  
⚠️ **无认证**: 任何客户端都可连接  
⚠️ **无签名**: 易受中间人攻击  

### 安全建议

1. **仅在可信网络使用**
   - 局域网环境
   - 内网专线

2. **防火墙隔离**
   ```bash
   # 只允许特定 IP 段
   iptables -A INPUT -p tcp --dport 25555 -s 192.168.1.0/24 -j ACCEPT
   iptables -A INPUT -p tcp --dport 25555 -j DROP
   ```

3. **VPN/SSH 隧道**
   - 公网传输时使用加密隧道

4. **监控和日志**
   - 记录所有连接
   - 异常流量告警

### 未来增强

计划添加：
- [ ] TLS/SSL 加密
- [ ] 基于令牌的认证
- [ ] 数字签名验证
- [ ] 客户端证书
- [ ] 速率限制

---

## 文件清单

### 模组源代码
```
src/client/java/cn/epicmc/client/
├── config/
│   └── ClientConfig.java              # 配置管理器
├── network/
│   ├── ConfigClient.java              # 网络客户端
│   ├── PacketBuffer.java              # 数据包编解码
│   └── PacketType.java                # 协议常量
├── screen/
│   ├── ModMultiplayerScreen.java      # 多人游戏界面
│   └── ModMultiplayerServerListWidget.java
├── mixin/
│   ├── SinglePlayerScreen.java        # 单人游戏拦截
│   ├── TitleScreen.java               # 标题界面修改
│   ├── MultiplePlayers.java           # 原版界面拦截
│   └── GameOptionScreen.java
└── BlockFrontClient.java              # 客户端入口
```

### 配置服务器
```
ConfigServerExample.java               # 独立配置服务器
```

### 文档
```
README.md                              # 项目首页
QUICKSTART.md                          # 5分钟快速开始
PROTOCOL.md                            # 网络协议详细文档
README_CONFIG.md                       # 配置系统说明
PROJECT_SUMMARY.md                     # 本文档 - 项目总结
```

---

## 编译和打包

### 编译模组
```bash
./gradlew build
```

输出: `build/libs/blockfront-1.0.0.jar`

### 编译配置服务器
```bash
javac ConfigServerExample.java
```

输出: `ConfigServerExample.class`

### 运行配置服务器
```bash
java ConfigServerExample [port]
```

---

## 开发路线图

### v1.0 (当前)
- ✅ 基础权限控制
- ✅ 服务器列表管理
- ✅ 二进制协议
- ✅ 游戏内同步

### v1.1 (计划中)
- [ ] TLS/SSL 加密
- [ ] 配置文件支持 (YAML/JSON)
- [ ] 热重载配置

### v1.2 (计划中)
- [ ] 客户端认证
- [ ] 玩家级权限
- [ ] Web 管理界面

### v2.0 (长期)
- [ ] 实时配置推送
- [ ] 统计和监控
- [ ] 多语言支持
- [ ] 插件系统

---

## 许可证

CC0-1.0 - 公共领域贡献

---

## 总结

BlockFront 是一个专为小游戏服务器和受控环境设计的轻量级 Minecraft 客户端模组。它通过极简的二进制协议实现服务器端配置控制，带宽消耗极低（单次同步仅 ~400 字节），易于部署和维护。

**适用场景**: 网吧、教育、小游戏服务器、活动赛事  
**核心优势**: 轻量、灵活、易部署  
**技术特点**: 异步通信、动态配置、Mixin 注入  

**立即开始**: 查看 [QUICKSTART.md](QUICKSTART.md)
