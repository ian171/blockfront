package cn.epicmc.client;

import cn.epicmc.BlockFront;
import cn.epicmc.client.network.ConfigClient;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

public class BlockFrontClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (MinecraftClient.getInstance().isDemo()){
			MinecraftClient.getInstance().setScreen(new SelectWorldScreen(null));
		}

		// 初始化配置客户端
		BlockFront.LOGGER.info("Initializing BlockFront Client");

		// 从环境变量或系统属性读取配置服务器地址
		String configHost = System.getProperty("blockfront.config.host", "127.0.0.1");
		int configPort = Integer.parseInt(System.getProperty("blockfront.config.port", "25555"));

		ConfigClient.getInstance().setConfigServer(configHost, configPort);

		// 异步同步配置（不阻塞游戏启动）
		ConfigClient.getInstance().syncAll().thenAccept(success -> {
			if (success) {
				BlockFront.LOGGER.info("Config synced successfully");
			} else {
				BlockFront.LOGGER.warn("Failed to sync config, using default settings");
			}
		});

		// 初始化 UDP HUD 客户端（固定端口 25566）
		cn.epicmc.client.hud.network.UdpHudClient.getInstance().start();

		BlockFront.LOGGER.info("UDP HUD client initialized on port 25566");
	}
}