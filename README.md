# BlockFront

一个轻量级的 Minecraft Fabric 客户端模组，用于小游戏服务器和受控环境。通过极简的二进制协议实现服务器端配置控制。

## ✨ 特性

- 🔒 **服务器端权限控制** - 禁用单人游戏、限制服务器编辑
- 🌐 **动态服务器列表** - 从配置服务器加载允许的服务器
- ⚡ **极简协议** - 单次同步仅需 ~400 字节
- 🎮 **游戏内同步** - 支持手动刷新配置
- 💬 **自定义消息** - 显示服务器欢迎信息
- 🔧 **易于部署** - 配置服务器是单文件 Java 程序

## 🚀 快速开始

### 1. 启动配置服务器

```bash
javac ConfigServerExample.java
java ConfigServerExample
```

### 2. 配置客户端

添加 JVM 参数到 Minecraft 启动器：
```
-Dblockfront.config.host=127.0.0.1
-Dblockfront.config.port=25555
```

### 3. 启动游戏

客户端将自动连接配置服务器并应用设置。

详细指南请查看 [QUICKSTART.md](QUICKSTART.md)

## 📖 文档

- [快速开始指南](QUICKSTART.md) - 5分钟上手
- [网络协议文档](PROTOCOL.md) - 协议详细说明
- [配置系统文档](README_CONFIG.md) - 高级配置

## 🎯 使用场景

- **网吧/电竞馆** - 防止玩家创建单人世界
- **小游戏服务器** - 提供多个游戏模式选择
- **教育环境** - 控制学生访问权限
- **活动赛事** - 快速部署统一客户端配置

## 🔧 技术栈

- Minecraft 1.21.1
- Fabric Loader >= 0.19.3
- Java 21+
- 自定义二进制协议

## 📦 构建

```bash
./gradlew build
```

输出文件：`build/libs/blockfront-1.0.0.jar`

## 🛡️ 安全提示

⚠️ 当前版本**不包含加密和认证**！建议：
- 仅在可信局域网使用
- 使用防火墙限制访问
- 公网部署时使用 VPN/SSH 隧道

## 📝 许可证

本项目采用 CC0-1.0 许可证 - 详见 [LICENSE](LICENSE)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

