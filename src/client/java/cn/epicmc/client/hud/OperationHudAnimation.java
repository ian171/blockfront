package cn.epicmc.client.hud;

/** Small reusable time-based easing helpers for HUD state transitions. */
public final class OperationHudAnimation {
    private float displayed;
    private float target;
    private boolean initialized;

    public float update(float value, float responsiveness) {
        target = value;
        if (!initialized) {
            displayed = value;
            initialized = true;
        }
        displayed += (target - displayed) * Math.clamp(responsiveness, 0.0f, 1.0f);
        return displayed;
    }

    public static float easeOutCubic(float value) {
        float inverse = 1.0f - Math.clamp(value, 0.0f, 1.0f);
        return 1.0f - inverse * inverse * inverse;
    }
}
