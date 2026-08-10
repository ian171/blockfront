package cn.epicmc.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DeploymentStatePayload(String phase, boolean reset, int durationTicks,
                                     double anchorX, double anchorY, double anchorZ,
                                     String targetId, String targetName,
                                     double targetX, double targetY, double targetZ) implements CustomPayload {
    public static final Id<DeploymentStatePayload> ID = new Id<>(Identifier.of("operation", "deployment_state"));
    public static final PacketCodec<RegistryByteBuf, DeploymentStatePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.phase); buf.writeBoolean(payload.reset); buf.writeVarInt(payload.durationTicks);
                buf.writeDouble(payload.anchorX); buf.writeDouble(payload.anchorY); buf.writeDouble(payload.anchorZ);
                buf.writeString(payload.targetId); buf.writeString(payload.targetName);
                buf.writeDouble(payload.targetX); buf.writeDouble(payload.targetY); buf.writeDouble(payload.targetZ);
            },
            buf -> new DeploymentStatePayload(buf.readString(), buf.readBoolean(), buf.readVarInt(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readString(), buf.readString(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble())
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
