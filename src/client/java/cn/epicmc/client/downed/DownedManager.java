package cn.epicmc.client.downed;

import cn.epicmc.client.network.DownedStatePayload;
import cn.epicmc.client.network.DownedSkipPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class DownedManager {
    private static boolean downed;
    private static long endsAt;
    private static String killerName = "";
    private static String weaponName = "";
    private static int killerEntityId = -1;
    private static Vec3d killerPosition = Vec3d.ZERO;
    private static boolean outlined;
    private static long skipStartedAt;
    private static boolean skipSent;
    private static final long SKIP_HOLD_MILLIS = 1500L;

    private DownedManager() { }

    public static void accept(DownedStatePayload payload) {
        downed = payload.active();
        if (!downed) { release(); return; }
        endsAt = System.currentTimeMillis() + payload.remainingTicks() * 50L;
        killerName = payload.killerName();
        weaponName = payload.weaponName();
        killerEntityId = payload.killerEntityId();
        killerPosition = new Vec3d(payload.killerX(), payload.killerY(), payload.killerZ());
        focusKiller();
    }

    public static void tick() {
        if (downed) focusKiller();
    }

    private static void focusKiller() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        Entity killer = killerEntityId < 0 ? null : client.world.getEntityById(killerEntityId);
        if (killer != null) {
            killer.setGlowing(true);
            outlined = true;
            client.setCameraEntity(killer);
        }
    }

    public static void release() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && killerEntityId >= 0) {
            Entity killer = client.world.getEntityById(killerEntityId);
            if (killer != null && outlined) killer.setGlowing(false);
        }
        if (client.player != null) client.setCameraEntity(client.player);
        downed = false;
        outlined = false;
        killerEntityId = -1;
        skipStartedAt = 0;
        skipSent = false;
    }

    public static boolean isDowned() { return downed; }
    public static int remainingSeconds() { return Math.max(0, (int) Math.ceil((endsAt - System.currentTimeMillis()) / 1000.0)); }
    public static String killerName() { return killerName; }
    public static String weaponName() { return weaponName; }
    public static Vec3d killerPosition() { return killerPosition; }
    public static float skipProgress() {
        if (skipSent) return 1.0f;
        return skipStartedAt == 0 ? 0.0f : Math.min(1.0f, (System.currentTimeMillis() - skipStartedAt) / (float) SKIP_HOLD_MILLIS);
    }
}
