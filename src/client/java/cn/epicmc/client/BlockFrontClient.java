package cn.epicmc.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

public class BlockFrontClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (MinecraftClient.getInstance().isDemo()){
			MinecraftClient.getInstance().setScreen(new SelectWorldScreen(null));
		}
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}
}