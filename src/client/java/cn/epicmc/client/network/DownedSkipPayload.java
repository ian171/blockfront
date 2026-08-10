package cn.epicmc.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DownedSkipPayload() implements CustomPayload {
    public static final Id<DownedSkipPayload> ID = new Id<>(Identifier.of("operation", "downed_skip"));
    public static final PacketCodec<RegistryByteBuf, DownedSkipPayload> CODEC = PacketCodec.unit(new DownedSkipPayload());
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
