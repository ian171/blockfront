# BlockFront 配置系统使用指南

## 概述

BlockFront 模组现在支持通过配置服务器动态控制客户端行为。这允许服务器管理员集中管理客户端权限和可用服务器列表。

## 特性

✅ **远程配置管理** - 服务器端统一控制客户端行为  
✅ **轻量级协议** - 使用简单的二进制格式，节省带宽  
✅ **灵活的权限控制** - 精细控制客户端功能  
✅ **动态服务器列表** - 集中管理允许连接的服务器  
✅ **自定义欢迎消息** - 向玩家显示自定义信息  

## 快速开始

### 1. 启动配置服务器

```bash
# 编译示例配置服务器
javac ConfigServerExample.java

# 启动服务器（默认端口 25555）
java ConfigServerExample

# 或使用自定义端口
java ConfigServerExample 8080
```

### 2. 配置 Minecraft 客户端

启动 Minecraft 时添加 JVM 参数：

```bash
java -Dblockfront.config.host=127.0.0.1 \
     -Dblockfront.config.port=25555 \
     -jar minecraft.jar
```

**在启动器中设置**:
- **Prism Launcher / MultiMC**: 编辑实例 → 设置 → Java → JVM 参数
- **官方启动器**: 启动配置 → JVM 参数

### 3. 游戏内效果

客户端启动后会自动连接配置服务器，同步以下内容：
- 权限设置（单人游戏、编辑服务器等）
- 允许连接的服务器列表
- 欢迎消息

## 配置选项

### 权限标志

在 `ConfigServerExample.java` 中修改：

```java
// 配置项
private static boolean allowSingleplayer = false;    // 允许单人游戏
private static boolean allowEditServers = false;     // 允许编辑服务器
private static boolean allowDeleteServers = false;   // 允许删除服务器
private static boolean allowAddServers = false;      // 允许添加服务器
private static String welcomeMessage = "欢迎!";      // 欢迎消息
```

### 服务器列表

添加允许连接的服务器：

```java
servers.add(new ServerEntry("服务器名称", "mc.example.com", 25565));
servers.add(new ServerEntry("小游戏服", "minigame.example.com", 25566));
```

## 工作原理

```
┌─────────────────┐
│ Minecraft 客户端 │
│  + BlockFront   │
└────────┬────────┘
         │
         │ 1. 启动时连接
         │ 2. 请求配置 (0x01)
         │ 3. 请求服务器列表 (0x02)
         ↓
┌─────────────────┐
│   配置服务器      │
│ (ConfigServer)  │
└────────┬────────┘
         │
         │ 4. 返回配置 (0x11)
         │ 5. 返回服务器列表 (0x12)
         ↓
┌─────────────────┐
│   客户端应用配置  │
│  - 禁用单人游戏  │
│  - 加载服务器    │
│  - 显示欢迎信息  │
└─────────────────┘
```

## 网络协议

协议详细说明请参见 `PROTOCOL.md`。

### 简要说明

**数据包格式**: `[类型:1字节][长度:2字节][数据:N字节]`

**主要数据包**:
- `0x01` - 请求配置
- `0x02` - 请求服务器列表
- `0x11` - 配置响应
- `0x12` - 服务器列表响应

## 部署场景

### 场景 1: 网吧/电竞馆

```java
// 完全限制单机，只允许连接到指定服务器
allowSingleplayer = false;
allowEditServers = false;
allowDeleteServers = false;
allowAddServers = false;

servers.add(new ServerEntry("官方服务器", "official.server.com", 25565));
```

### 场景 2: 教育环境

```java
// 允许单人游戏，但控制多人服务器列表
allowSingleplayer = true;
allowEditServers = false;
allowDeleteServers = false;
allowAddServers = false;

servers.add(new ServerEntry("教学服务器", "edu.minecraft.com", 25565));
```

### 场景 3: 小游戏服务器

```java
// 提供多个小游戏服务器选择
allowSingleplayer = false;
allowEditServers = false;

servers.add(new ServerEntry("空岛战争", "skywars.example.com", 25565));
servers.add(new ServerEntry("起床战争", "bedwars.example.com", 25565));
servers.add(new ServerEntry("饥饿游戏", "hungergames.example.com", 25565));

welcomeMessage = "选择你喜欢的小游戏！";
```

## 高级配置

### 使用环境变量

```bash
# Linux/Mac
export BLOCKFRONT_CONFIG_HOST=config.server.com
export BLOCKFRONT_CONFIG_PORT=25555

# Windows
set BLOCKFRONT_CONFIG_HOST=config.server.com
set BLOCKFRONT_CONFIG_PORT=25555
```

然后在代码中读取：

```java
String configHost = System.getenv().getOrDefault(
    "BLOCKFRONT_CONFIG_HOST",
    System.getProperty("blockfront.config.host", "127.0.0.1")
);
```

### 配置文件支持

可以扩展配置服务器，从配置文件读取设置：

```java
// config.properties
allow.singleplayer=false
allow.edit=false
allow.delete=false
allow.add=false
welcome.message=欢迎来到服务器！

# 服务器列表
servers.1.name=主服务器
servers.1.address=mc.example.com
servers.1.port=25565
```

### Docker 部署

```dockerfile
FROM openjdk:21-slim
COPY ConfigServerExample.java /app/
WORKDIR /app
RUN javac ConfigServerExample.java
EXPOSE 25555
CMD ["java", "ConfigServerExample", "25555"]
```

```bash
docker build -t blockfront-config .
docker run -p 25555:25555 blockfront-config
```

## 故障排除

### 客户端无法连接配置服务器

**问题**: 游戏日志显示 "Failed to connect to config server"

**解决方案**:
1. 检查配置服务器是否运行：`netstat -an | grep 25555`
2. 检查防火墙是否允许连接
3. 验证 JVM 参数是否正确设置
4. 检查 IP 地址和端口是否正确

### 配置未生效

**问题**: 客户端仍然可以访问单人游戏

**解决方案**:
1. 检查配置服务器日志，确认客户端已连接
2. 重启 Minecraft 客户端
3. 查看客户端日志中的 "Config applied" 消息

### 服务器列表为空

**问题**: 多人游戏界面没有显示服务器

**解决方案**:
1. 确认配置服务器的 `servers` 列表不为空
2. 检查网络连接
3. 查看日志中的 "Added server from config" 消息

## 安全注意事项

⚠️ **重要**: 当前协议不包含加密或认证！

**建议**:
- 仅在可信网络（局域网）中使用
- 使用防火墙限制配置服务器访问
- 公网部署时使用 VPN 或 SSH 隧道
- 不要在配置中包含敏感信息

## 性能优化

### 带宽使用

典型的配置同步（3个服务器）：
- 请求配置: ~50 字节
- 配置响应: ~100 字节
- 请求服务器: ~50 字节
- 服务器列表: ~200 字节

**总计**: ~400 字节/客户端启动

### 连接模式

**当前实现**: 启动时连接 → 同步 → 断开  
**优点**: 简单、低开销  
**缺点**: 无法动态更新

**可选实现**: 持久连接 + 心跳  
**优点**: 支持实时更新  
**缺点**: 保持连接占用资源

## 未来功能

计划中的功能：
- [ ] TLS/SSL 加密支持
- [ ] 客户端认证（令牌/密码）
- [ ] 配置文件支持
- [ ] Web 管理界面
- [ ] 实时配置推送
- [ ] 玩家权限系统（不同玩家不同权限）
- [ ] 统计信息收集

## 许可证

与主项目相同，采用 CC0-1.0 许可证。

## 支持

- 协议文档: `PROTOCOL.md`
- 示例代码: `ConfigServerExample.java`
- 项目主页: [GitHub链接]
