# BlockFront 网络协议文档

## 概述

BlockFront 使用简单的二进制协议进行客户端-服务器通信。该协议设计目标是：
- **最小化带宽使用**：使用字节级传输，避免JSON等文本格式
- **简单易实现**：结构清晰，易于在不同语言中实现
- **可扩展性**：支持未来添加新的数据包类型

## 数据包格式

所有数据包使用统一的格式：

```
+--------+--------+--------+------------------+
| Type   | Length          | Payload          |
| 1 byte | 2 bytes (短整型) | n bytes          |
+--------+--------+--------+------------------+
```

- **Type (1 byte)**: 数据包类型标识符
- **Length (2 bytes)**: Payload 长度（大端序，无符号短整型，0-65535）
- **Payload (n bytes)**: 实际数据内容

## 数据包类型

### 客户端 → 服务器

| Type | 名称 | 描述 |
|------|------|------|
| `0x01` | REQUEST_CONFIG | 请求服务器配置 |
| `0x02` | REQUEST_SERVERS | 请求服务器列表 |
| `0x03` | HEARTBEAT | 心跳包（保活） |

### 服务器 → 客户端

| Type | 名称 | 描述 |
|------|------|------|
| `0x11` | CONFIG_RESPONSE | 配置响应 |
| `0x12` | SERVERS_RESPONSE | 服务器列表响应 |
| `0x13` | HEARTBEAT_ACK | 心跳确认 |
| `0x14` | KICK | 踢出客户端 |

## 数据包详细说明

### 0x01 REQUEST_CONFIG (请求配置)

**客户端 → 服务器**

请求服务器发送配置信息。

**Payload**: 无（length = 0）

---

### 0x11 CONFIG_RESPONSE (配置响应)

**服务器 → 客户端**

响应客户端的配置请求，包含客户端行为配置。

**Payload 格式**:

```
+--------+--------+--------+------------------+
| Flags  | MsgLen          | Welcome Message  |
| 1 byte | 2 bytes         | n bytes (UTF-8)  |
+--------+--------+--------+------------------+
```

**Flags 位定义**:
- Bit 0 (0x01): `allowSingleplayer` - 是否允许单人游戏
- Bit 1 (0x02): `allowEditServers` - 是否允许编辑服务器
- Bit 2 (0x04): `allowDeleteServers` - 是否允许删除服务器
- Bit 3 (0x08): `allowAddServers` - 是否允许添加服务器
- Bit 4-7: 保留（未来使用）

**示例**:
```
Flags = 0x02  // 只允许编辑服务器
Welcome Message = "欢迎来到服务器！"
```

---

### 0x02 REQUEST_SERVERS (请求服务器列表)

**客户端 → 服务器**

请求服务器发送允许连接的服务器列表。

**Payload**: 无（length = 0）

---

### 0x12 SERVERS_RESPONSE (服务器列表响应)

**服务器 → 客户端**

响应客户端的服务器列表请求。

**Payload 格式**:

```
+--------+------------------+
| Count  | Server Entries   |
| 1 byte | n * entry_size   |
+--------+------------------+
```

**Server Entry 格式** (每个服务器):

```
+--------+--------+----------------+--------+--------+------------------+--------+--------+
| NameLen         | Name (UTF-8)   | AddrLen         | Address (UTF-8)  | Port            |
| 2 bytes         | n bytes        | 2 bytes         | m bytes          | 2 bytes         |
+--------+--------+----------------+--------+--------+------------------+--------+--------+
```

- **Count (1 byte)**: 服务器数量（0-255）
- **NameLen (2 bytes)**: 服务器名称长度
- **Name**: 服务器名称（UTF-8编码）
- **AddrLen (2 bytes)**: 服务器地址长度
- **Address**: 服务器地址（UTF-8编码，不含端口）
- **Port (2 bytes)**: 服务器端口（大端序，无符号）

**示例**:
```
Count = 2

Server 1:
  Name = "主服务器"
  Address = "mc.example.com"
  Port = 25565

Server 2:
  Name = "小游戏服"
  Address = "minigame.example.com"
  Port = 25566
```

---

### 0x03 HEARTBEAT (心跳包)

**客户端 → 服务器**

用于保持连接活跃，定期发送。

**Payload**: 无（length = 0）

---

### 0x13 HEARTBEAT_ACK (心跳确认)

**服务器 → 客户端**

响应客户端的心跳包。

**Payload**: 无（length = 0）

---

### 0x14 KICK (踢出客户端)

**服务器 → 客户端**

服务器通知客户端断开连接。

**Payload 格式**:

```
+--------+--------+------------------+
| ReasonLen        | Reason (UTF-8)   |
| 2 bytes          | n bytes          |
+--------+--------+------------------+
```

- **ReasonLen (2 bytes)**: 原因文本长度
- **Reason**: 踢出原因（UTF-8编码）

---

## 通信流程

### 1. 客户端启动时同步配置

```
Client                          Server
  |                               |
  |-- TCP Connect --------------->|
  |                               |
  |-- 0x01 REQUEST_CONFIG ------->|
  |                               |
  |<-- 0x11 CONFIG_RESPONSE ------|
  |                               |
  |-- 0x02 REQUEST_SERVERS ------>|
  |                               |
  |<-- 0x12 SERVERS_RESPONSE -----|
  |                               |
  |-- TCP Disconnect ------------->|
```

### 2. 保活连接（可选）

如果需要实时更新配置：

```
Client                          Server
  |                               |
  |-- TCP Connect (持久连接) ----->|
  |                               |
  |-- 0x01 REQUEST_CONFIG ------->|
  |<-- 0x11 CONFIG_RESPONSE ------|
  |                               |
  |-- 0x02 REQUEST_SERVERS ------>|
  |<-- 0x12 SERVERS_RESPONSE -----|
  |                               |
  |   ... 每30秒 ...               |
  |                               |
  |-- 0x03 HEARTBEAT ------------->|
  |<-- 0x13 HEARTBEAT_ACK --------|
  |                               |
  |   ... 配置更新时 ...           |
  |                               |
  |<-- 0x11 CONFIG_RESPONSE ------|
  |                               |
```

## 字节序

所有多字节整数使用 **大端序 (Big Endian)**，即网络字节序。

## 字符编码

所有字符串使用 **UTF-8** 编码。

## 错误处理

- 如果客户端收到未知的数据包类型，应忽略该包并记录警告
- 如果数据包格式错误，客户端应断开连接
- 连接超时时间建议设置为 5 秒
- 如果配置服务器不可用，客户端应使用默认配置（禁止所有操作）

## 安全考虑

⚠️ **警告**: 此协议未包含任何加密或身份验证机制！

建议的安全措施：
1. **仅在可信网络中使用**（如局域网）
2. **使用防火墙限制访问**（仅允许特定IP）
3. **如需公网部署，使用VPN或SSH隧道**
4. **未来版本可考虑添加**：
   - 客户端认证（用户名/密码或令牌）
   - 数据加密（TLS/SSL）
   - 签名验证（防止中间人攻击）

## 配置服务器实现示例

参见项目根目录的 `ConfigServerExample.java` 文件。

### 启动配置服务器：

```bash
# 编译
javac ConfigServerExample.java

# 运行（默认端口 25555）
java ConfigServerExample

# 或指定端口
java ConfigServerExample 8080
```

### 客户端配置：

在启动 Minecraft 时设置 JVM 参数：

```bash
-Dblockfront.config.host=127.0.0.1
-Dblockfront.config.port=25555
```

## 扩展协议

如需添加新功能，分配新的数据包类型：

- `0x04`-`0x0F`: 保留给客户端→服务器
- `0x15`-`0x1F`: 保留给服务器→客户端
- `0x20`+: 可用于自定义扩展

## 版本历史

- **v1.0** (2026-08): 初始版本
  - 基础配置同步
  - 服务器列表管理
  - 心跳保活机制
