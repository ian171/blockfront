package cn.epicmc.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DownedStatePayload(boolean active, int remainingTicks, String killerName, int killerEntityId,
                                  double killerX, double killerY, double killerZ, String weaponName) implements CustomPayload {
    public static final Id<DownedStatePayload> ID = new Id<>(Identifier.of("operation", "downed_state"));
    public static final PacketCodec<RegistryByteBuf, DownedStatePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeBoolean(payload.active()); buf.writeVarInt(payload.remainingTicks());
                buf.writeString(payload.killerName()); buf.writeVarInt(payload.killerEntityId());
                buf.writeDouble(payload.killerX()); buf.writeDouble(payload.killerY()); buf.writeDouble(payload.killerZ());
                buf.writeString(payload.weaponName());
            },
            buf -> new DownedStatePayload(buf.readBoolean(), buf.readVarInt(), buf.readString(), buf.readVarInt(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readString())
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
