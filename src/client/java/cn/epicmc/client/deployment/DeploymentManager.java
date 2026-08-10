package cn.epicmc.client.deployment;

import cn.epicmc.client.network.DeploymentStatePayload;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client deployment state populated solely by the Operation server. */
public final class DeploymentManager {
    private static final Map<String, DeploymentTarget> TARGETS = new LinkedHashMap<>();
    private static String phase = "CLOSED";
    private static long phaseStartedAt;
    private static int transitionTicks;

    private DeploymentManager() { }

    public static void accept(DeploymentStatePayload payload) {
        if (payload.reset() || "CLOSED".equals(payload.phase())) {
            close();
            return;
        }
        phase = payload.phase();
        phaseStartedAt = System.currentTimeMillis();
        transitionTicks = payload.durationTicks();
        if ("SELECTING".equals(phase)) {
            TARGETS.put(payload.targetId(), new DeploymentTarget(payload.targetId(), payload.targetName(),
                    new Vec3d(payload.targetX(), payload.targetY(), payload.targetZ())));
        }
    }

    public static void close() {
        TARGETS.clear();
        phase = "CLOSED";
        phaseStartedAt = 0;
        transitionTicks = 0;
    }

    public static boolean active() { return !"CLOSED".equals(phase); }
    public static boolean selecting() { return "SELECTING".equals(phase); }
    public static boolean descending() { return "DESCENDING".equals(phase); }
    public static String phase() { return phase; }
    public static List<DeploymentTarget> targets() { return new ArrayList<>(TARGETS.values()); }
    public static long elapsedMillis() { return Math.max(0, System.currentTimeMillis() - phaseStartedAt); }
    public static int transitionTicks() { return transitionTicks; }

    public record DeploymentTarget(String id, String name, Vec3d position) { }
}
