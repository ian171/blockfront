# BlockFront 快速开始指南

## 项目介绍

BlockFront 是一个 Minecraft 1.21.1 Fabric 客户端模组，专为小游戏服务器和受控环境设计。它通过轻量级的二进制协议与配置服务器通信，实现动态权限控制和服务器列表管理。

## 核心功能

- ✅ **服务器端配置控制** - 集中管理所有客户端行为
- ✅ **极简二进制协议** - 单次同步仅需 ~400 字节
- ✅ **动态权限系统** - 控制单人游戏、编辑、删除等操作
- ✅ **白名单服务器** - 只允许连接到指定服务器
- ✅ **实时配置同步** - 支持游戏内手动同步配置
- ✅ **自定义欢迎消息** - 显示服务器公告

## 快速开始（5分钟）

### 步骤 1: 编译配置服务器

```bash
cd E:\blockfront
javac ConfigServerExample.java
```

### 步骤 2: 启动配置服务器

```bash
# 使用默认端口 25555
java ConfigServerExample

# 或自定义端口
java ConfigServerExample 8080
```

你将看到：
```
BlockFront Config Server starting on port 25555
Configuration:
  Allow Singleplayer: false
  Allow Edit: false
  Allow Delete: false
  Allow Add: false
  Welcome Message: 欢迎来到游戏服务器!

Servers:
  - 主服务器 -> mc.example.com:25565
  - 小游戏服务器 -> minigame.example.com:25566
  - 测试服务器 -> 127.0.0.1:25565

Waiting for connections...
```

### 步骤 3: 配置 Minecraft 客户端

编辑你的启动器配置，添加 JVM 参数：

```bash
-Dblockfront.config.host=127.0.0.1
-Dblockfront.config.port=25555
```

**各启动器配置位置**:

| 启动器 | 配置路径 |
|--------|----------|
| Prism Launcher | 实例 → 编辑 → 设置 → Java → JVM 参数 |
| MultiMC | 实例设置 → Java → JVM 参数 |
| 官方启动器 | 启动配置 → 更多选项 → JVM 参数 |

### 步骤 4: 启动游戏

启动 Minecraft，模组将自动：
1. 连接到配置服务器
2. 下载配置和服务器列表
3. 应用权限设置
4. 显示欢迎消息

游戏内你会看到：
- 自动跳转到多人游戏界面
- 显示配置的服务器列表
- 可用"同步配置"按钮手动刷新

## 自定义配置

### 修改服务器行为

编辑 `ConfigServerExample.java`：

```java
// 权限控制
private static boolean allowSingleplayer = false;    // 是否允许单人游戏
private static boolean allowEditServers = false;     // 是否允许编辑服务器
private static boolean allowDeleteServers = false;   // 是否允许删除服务器
private static boolean allowAddServers = false;      // 是否允许添加服务器

// 欢迎消息
private static String welcomeMessage = "欢迎来到服务器！";

// 服务器列表
static {
    servers.add(new ServerEntry("服务器名称", "地址", 端口));
    servers.add(new ServerEntry("主服务器", "mc.example.com", 25565));
    servers.add(new ServerEntry("小游戏", "games.example.com", 25566));
}
```

修改后重新编译：
```bash
javac ConfigServerExample.java
java ConfigServerExample
```

### 常见配置场景

#### 场景 1: 网吧/电竞馆
```java
// 完全锁定客户端
allowSingleplayer = false;
allowEditServers = false;
allowDeleteServers = false;
allowAddServers = false;
welcomeMessage = "欢迎来到 XX 电竞馆！";

servers.add(new ServerEntry("官方服务器", "official.server.com", 25565));
```

#### 场景 2: 小游戏大厅
```java
// 提供多个小游戏选择
allowSingleplayer = false;
allowEditServers = false;
welcomeMessage = "选择你喜欢的游戏模式！";

servers.add(new ServerEntry("空岛战争", "skywars.server.com", 25565));
servers.add(new ServerEntry("起床战争", "bedwars.server.com", 25565));
servers.add(new ServerEntry("饥饿游戏", "survival.server.com", 25565));
servers.add(new ServerEntry("创造建筑", "creative.server.com", 25565));
```

#### 场景 3: 教育环境
```java
// 允许单人游戏，限制多人服务器
allowSingleplayer = true;
allowEditServers = false;
welcomeMessage = "欢迎参加今天的 Minecraft 课程！";

servers.add(new ServerEntry("课堂服务器", "edu.minecraft.com", 25565));
```

#### 场景 4: 测试/开发环境
```java
// 允许所有操作
allowSingleplayer = true;
allowEditServers = true;
allowDeleteServers = true;
allowAddServers = true;
welcomeMessage = "开发环境 - 所有功能已启用";

servers.add(new ServerEntry("开发服务器", "localhost", 25565));
servers.add(new ServerEntry("测试服务器", "test.local", 25566));
```

## 网络协议说明

### 数据包格式
```
[类型:1字节][长度:2字节][数据:N字节]
```

### 主要数据包类型
- `0x01` - 客户端请求配置
- `0x02` - 客户端请求服务器列表
- `0x11` - 服务器返回配置
- `0x12` - 服务器返回服务器列表

### 配置同步流程
```
客户端启动 → 连接配置服务器 → 请求配置(0x01) → 接收配置(0x11) 
→ 请求服务器列表(0x02) → 接收列表(0x12) → 断开连接 → 应用配置
```

### 带宽消耗
- 典型配置同步: ~400 字节/次
- 3个服务器列表: ~200 字节
- 非常适合低带宽环境

## 高级使用

### 使用环境变量

**Linux/Mac**:
```bash
export BLOCKFRONT_CONFIG_HOST=config.myserver.com
export BLOCKFRONT_CONFIG_PORT=25555
minecraft-launcher
```

**Windows**:
```cmd
set BLOCKFRONT_CONFIG_HOST=config.myserver.com
set BLOCKFRONT_CONFIG_PORT=25555
minecraft-launcher.exe
```

### Docker 部署配置服务器

创建 `Dockerfile`:
```dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY ConfigServerExample.java .
RUN javac ConfigServerExample.java
EXPOSE 25555
CMD ["java", "ConfigServerExample"]
```

构建并运行:
```bash
docker build -t blockfront-config .
docker run -d -p 25555:25555 --name blockfront blockfront-config
```

### 防火墙配置

**允许配置服务器端口**:

```bash
# Linux (iptables)
sudo iptables -A INPUT -p tcp --dport 25555 -j ACCEPT

# Linux (firewalld)
sudo firewall-cmd --permanent --add-port=25555/tcp
sudo firewall-cmd --reload

# Windows PowerShell (管理员)
New-NetFirewallRule -DisplayName "BlockFront Config" -Direction Inbound -Protocol TCP -LocalPort 25555 -Action Allow
```

## 故障排除

### 问题 1: 客户端无法连接配置服务器

**症状**: 游戏日志显示 "Failed to connect to config server"

**解决方案**:
1. 确认配置服务器正在运行
   ```bash
   netstat -an | grep 25555
   ```
2. 检查防火墙设置
3. 验证 JVM 参数是否正确
4. 尝试 ping 配置服务器地址

### 问题 2: 配置未生效

**症状**: 仍然可以访问单人游戏或编辑服务器

**解决方案**:
1. 检查客户端日志：`logs/latest.log`
   - 查找 "Config synced successfully"
   - 查找 "Config applied: singleplayer=false"
2. 确认配置服务器的配置值
3. 重启 Minecraft 客户端
4. 使用游戏内"同步配置"按钮手动刷新

### 问题 3: 服务器列表为空

**症状**: 多人游戏界面没有服务器

**解决方案**:
1. 检查配置服务器日志，确认发送了服务器列表
2. 查看客户端日志中的 "Added server from config" 消息
3. 确认 `servers` 列表不为空
4. 点击"同步配置"按钮

### 问题 4: 编译失败

**症状**: `javac ConfigServerExample.java` 报错

**解决方案**:
1. 确认 Java 版本 >= 17
   ```bash
   java -version
   javac -version
   ```
2. 如果使用 Java 8，需要修改代码中的 switch 表达式

### 问题 5: 模组编译失败

**症状**: `./gradlew build` 失败

**解决方案**:
1. 清理构建缓存
   ```bash
   ./gradlew clean
   ```
2. 删除被锁定的文件
   ```bash
   rm -rf build/
   ```
3. 关闭可能占用 jar 文件的程序（IDE、Java 进程）

## 安全建议

⚠️ **当前版本没有加密和认证功能！**

**生产环境建议**:

1. **仅在局域网使用**
   - 不要将配置服务器暴露到公网
   - 使用内网地址（192.168.x.x, 10.x.x.x）

2. **防火墙保护**
   ```bash
   # 只允许特定网段访问
   sudo iptables -A INPUT -p tcp --dport 25555 -s 192.168.1.0/24 -j ACCEPT
   sudo iptables -A INPUT -p tcp --dport 25555 -j DROP
   ```

3. **VPN/SSH 隧道**
   ```bash
   # 通过 SSH 隧道连接远程配置服务器
   ssh -L 25555:localhost:25555 user@remote-server
   # 然后客户端连接到 localhost:25555
   ```

4. **监控日志**
   - 定期检查配置服务器日志
   - 记录所有连接的客户端 IP

## 开发路线图

计划中的功能：
- [ ] TLS/SSL 加密支持
- [ ] 基于令牌的客户端认证
- [ ] 配置文件支持（YAML/JSON）
- [ ] Web 管理界面
- [ ] 实时配置推送（持久连接）
- [ ] 玩家级权限系统
- [ ] 统计和监控功能
- [ ] 多语言支持

## 技术细节

### 项目结构
```
blockfront/
├── src/
│   ├── client/java/cn/epicmc/client/
│   │   ├── config/
│   │   │   └── ClientConfig.java          # 配置管理
│   │   ├── network/
│   │   │   ├── ConfigClient.java          # 网络客户端
│   │   │   ├── PacketBuffer.java          # 数据包编解码
│   │   │   └── PacketType.java            # 协议定义
│   │   ├── screen/
│   │   │   ├── ModMultiplayerScreen.java  # 多人游戏界面
│   │   │   └── ModMultiplayerServerListWidget.java
│   │   ├── mixin/
│   │   │   ├── SinglePlayerScreen.java    # 单人游戏拦截
│   │   │   ├── TitleScreen.java           # 标题界面修改
│   │   │   └── MultiplePlayers.java       # 多人游戏拦截
│   │   └── BlockFrontClient.java
│   └── main/java/cn/epicmc/
│       └── BlockFront.java
├── ConfigServerExample.java               # 配置服务器示例
├── PROTOCOL.md                            # 协议文档
├── README_CONFIG.md                       # 配置系统文档
└── QUICKSTART.md                          # 本文档
```

### 依赖项
- Minecraft 1.21.1
- Fabric Loader >= 0.19.3
- Fabric API
- Java 21+

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

本项目采用 CC0-1.0 许可证 - 详见 LICENSE 文件

## 联系方式

- 项目主页: [GitHub]
- 协议文档: `PROTOCOL.md`
- 配置文档: `README_CONFIG.md`

---

**享受你的 Minecraft 小游戏服务器！** 🎮
