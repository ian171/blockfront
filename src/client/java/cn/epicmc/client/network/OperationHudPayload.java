package cn.epicmc.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Wire-compatible copy of Shanghai 1937 Operation's HUD payload. */
public record OperationHudPayload(
        String gameState,
        int tickets,
        int wave,
        int maxWaves,
        String pointId,
        String pointName,
        float progress,
        int captureState,
        int playersInPoint
) implements CustomPayload {
    public static final Id<OperationHudPayload> ID = new Id<>(Identifier.of("operation", "hud_state"));
    public static final PacketCodec<RegistryByteBuf, OperationHudPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.gameState);
                buf.writeInt(payload.tickets);
                buf.writeInt(payload.wave);
                buf.writeInt(payload.maxWaves);
                buf.writeString(payload.pointId);
                buf.writeString(payload.pointName);
                buf.writeFloat(payload.progress);
                buf.writeInt(payload.captureState);
                buf.writeInt(payload.playersInPoint);
            },
            buf -> new OperationHudPayload(
                    buf.readString(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readString(), buf.readString(), buf.readFloat(), buf.readInt(), buf.readInt())
    );
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
