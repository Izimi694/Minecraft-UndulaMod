package com.izimi.undulamod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record UndulaParticlePacket(
    float radius,
    byte particleType,
    int entityId,
    Vec3d pos
) implements CustomPayload {

    public static final CustomPayload.Id<UndulaParticlePacket> ID =
        new CustomPayload.Id<>(Identifier.of("undulamod", "undula_particle"));

    public static final PacketCodec<RegistryByteBuf, UndulaParticlePacket> CODEC =
        PacketCodec.tuple(
            PacketCodecs.FLOAT, UndulaParticlePacket::radius,
            PacketCodecs.BYTE, UndulaParticlePacket::particleType,
            PacketCodecs.INTEGER, UndulaParticlePacket::entityId,
            PacketCodecs.VECTOR3F.xmap(
                v -> new Vec3d(v.x, v.y, v.z),
                v -> new org.joml.Vector3f((float) v.x, (float) v.y, (float) v.z)
            ), UndulaParticlePacket::pos,
            UndulaParticlePacket::new
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}