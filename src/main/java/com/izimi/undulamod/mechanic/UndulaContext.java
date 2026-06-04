package com.izimi.undulamod.mechanic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public record UndulaContext(
    ServerWorld world,
    LivingEntity center,
    int stacks,
    int shatterLevel,
    int penetrateLevel,
    UUID attackerUuid,
    int chainDepth,
    float critRate,
    float precomputedDamage,
    boolean overflow,
    Vec3d deathPos
) {
    public UndulaContext(ServerWorld world, LivingEntity center, int stacks,
                        int shatterLevel, int penetrateLevel, UUID attackerUuid,
                        int chainDepth, float critRate, float precomputedDamage,
                        boolean overflow) {
        this(world, center, stacks, shatterLevel, penetrateLevel, attackerUuid,
             chainDepth, critRate, precomputedDamage, overflow, null);
    }

    public UndulaContext withDeathPos(Vec3d pos) {
        return new UndulaContext(world, center, stacks, shatterLevel, penetrateLevel,
                                attackerUuid, chainDepth, critRate, precomputedDamage,
                                overflow, pos);
    }

    public Vec3d getEffectPos() {
        if (deathPos != null) return deathPos;
        return center.getPos().add(0, center.getHeight() * 0.25, 0);
    }
}