package cn.epicmc.client;

import cn.epicmc.BlockFront;
import cn.epicmc.client.network.ConfigClient;
import cn.epicmc.client.downed.DownedManager;
import cn.epicmc.client.network.DownedSkipPayload;
import cn.epicmc.client.network.DownedStatePayload;
import cn.epicmc.client.deployment.DeploymentCameraController;
import cn.epicmc.client.deployment.DeploymentManager;
import cn.epicmc.client.deployment.DeploymentScreen;
import cn.epicmc.client.network.DeployRequestPayload;
import cn.epicmc.client.network.DeploymentStatePayload;
import cn.epicmc.client.hud.HudDataManager;
import cn.epicmc.client.network.OperationHudPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.lwjgl.glfw.GLFW;

public class BlockFrontClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(OperationHudPayload.ID, OperationHudPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DeploymentStatePayload.ID, DeploymentStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DeployRequestPayload.ID, DeployRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DownedStatePayload.ID, DownedStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DownedSkipPayload.ID, DownedSkipPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(OperationHudPayload.ID, (payload, context) ->
                context.client().execute(() -> applyOperationHud(payload)));
        ClientPlayNetworking.registerGlobalReceiver(DownedStatePayload.ID, (payload, context) ->
                context.client().execute(() -> DownedManager.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(DeploymentStatePayload.ID, (payload, context) ->
                context.client().execute(() -> applyDeploymentState(payload)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> { DeploymentCameraController.tick(); DownedManager.tick(); });
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
        BlockFront.LOGGER.info("Operation HUD channel registered");
	}

    private static void applyDeploymentState(DeploymentStatePayload payload) {
        boolean wasActive = DeploymentManager.active();
        DeploymentManager.accept(payload);
        MinecraftClient client = MinecraftClient.getInstance();
        if (payload.reset() || "CLOSED".equals(payload.phase())) {
            DeploymentCameraController.close();
            if (client.currentScreen instanceof DeploymentScreen) client.setScreen(null);
        } else {
            if ("SELECTING".equals(payload.phase()) && !wasActive) {
                DeploymentCameraController.ascendTo(new net.minecraft.util.math.Vec3d(payload.anchorX(), payload.anchorY(), payload.anchorZ()), payload.durationTicks());
            } else if ("DESCENDING".equals(payload.phase())) {
                DeploymentCameraController.descendTo(new net.minecraft.util.math.Vec3d(payload.targetX(), payload.targetY(), payload.targetZ()), payload.durationTicks());
            }
            if (!(client.currentScreen instanceof DeploymentScreen)) client.setScreen(new DeploymentScreen());
        }
    }
    private static void applyOperationHud(OperationHudPayload payload) {
        HudDataManager data = HudDataManager.getInstance();
        HudDataManager.CaptureState[] states = HudDataManager.CaptureState.values();
        int stateIndex = Math.clamp(payload.captureState(), 0, states.length - 1);
        data.updateCapturePoint(payload.pointId(), payload.pointName(), payload.progress(), states[stateIndex], payload.playersInPoint());
        data.setGameMode("上海1937行动 · " + payload.gameState());
        data.setScore(payload.tickets());
        data.setOperationState(payload.tickets(), payload.wave(), payload.maxWaves());
    }
}