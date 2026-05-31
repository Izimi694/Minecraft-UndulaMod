package com.izimi.undulamod.mechanic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record UndulaData(int stacks, int maxStacks, int shatterLevel, int penetrateLevel,
                          long lastTransferTick, float critRate) {

    public static final Codec<UndulaData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("stacks").forGetter(UndulaData::stacks),
            Codec.INT.fieldOf("max_stacks").forGetter(UndulaData::maxStacks),
            Codec.INT.fieldOf("shatter").forGetter(UndulaData::shatterLevel),
            Codec.INT.fieldOf("penetrate").forGetter(UndulaData::penetrateLevel),
            Codec.LONG.fieldOf("last_transfer").forGetter(UndulaData::lastTransferTick),
            Codec.FLOAT.fieldOf("crit_rate").forGetter(UndulaData::critRate)
        ).apply(instance, UndulaData::new)
    );

    public static final PacketCodec<RegistryByteBuf, UndulaData> PACKET_CODEC =
        PacketCodec.tuple(
            PacketCodecs.INTEGER, UndulaData::stacks,
            PacketCodecs.INTEGER, UndulaData::maxStacks,
            PacketCodecs.INTEGER, UndulaData::shatterLevel,
            PacketCodecs.INTEGER, UndulaData::penetrateLevel,
            PacketCodecs.VAR_LONG, UndulaData::lastTransferTick,
            PacketCodecs.FLOAT, UndulaData::critRate,
            UndulaData::new
        );

    public static UndulaData empty() {
        return new UndulaData(0, 5, 0, 0, 0, 0.10f);
    }

    public boolean isFull() { return stacks >= maxStacks; }
    public boolean isEmpty() { return stacks <= 0; }
}