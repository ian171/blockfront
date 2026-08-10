package cn.epicmc.client.deployment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;

/** Locally interpolates the view while the server keeps the spectator at a valid location. */
public final class DeploymentCameraController {
    private static ArmorStandEntity camera;
    private static Vec3d from;
    private static Vec3d to;
    private static long startedAt;
    private static long durationMillis;
    private static boolean active;
    private static Vec3d lastPlayerPosition = Vec3d.ZERO;

    private DeploymentCameraController() { }

    public static void rememberPlayerPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && !active) lastPlayerPosition = client.player.getPos();
    }

    public static void ascendTo(Vec3d destination, int ticks) {
        if (active) return;
        start(lastPlayerPosition, destination, ticks);
    }

    public static void descendTo(Vec3d destination, int ticks) {
        Vec3d origin = camera == null ? lastPlayerPosition : camera.getPos();
        start(origin, destination, ticks);
    }

    private static void start(Vec3d origin, Vec3d destination, int ticks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (camera == null) {
            camera = new ArmorStandEntity(client.world, origin.x, origin.y, origin.z);
            camera.setInvisible(true);
            camera.setNoGravity(true);
        }
        from = origin;
        to = destination;
        startedAt = System.currentTimeMillis();
        durationMillis = Math.max(1, ticks) * 50L;
        active = true;
        client.setCameraEntity(camera);
    }

    public static void tick() {
        rememberPlayerPosition();
        if (!active || camera == null) return;
        float progress = Math.min(1.0f, (System.currentTimeMillis() - startedAt) / (float) durationMillis);
        float eased = 1.0f - (float) Math.pow(1.0f - progress, 3);
        camera.setPosition(from.lerp(to, eased));
        if (progress >= 1.0f && DeploymentManager.descending()) close();
    }

    public static void close() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.setCameraEntity(client.player);
        camera = null;
        active = false;
    }
}
