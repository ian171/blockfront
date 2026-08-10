package cn.epicmc.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DeployRequestPayload(String targetId) implements CustomPayload {
    public static final Id<DeployRequestPayload> ID = new Id<>(Identifier.of("operation", "deploy_request"));
    public static final PacketCodec<RegistryByteBuf, DeployRequestPayload> CODEC = PacketCodec.of(
            (payload, buf) -> buf.writeString(payload.targetId), buf -> new DeployRequestPayload(buf.readString()));
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
