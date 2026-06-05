package com.izimi.undulamod.mechanic;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public record UndulaContext(
    ServerWorld world,
    Vec3d position,
    UUID targetUuid,
    int stacks,
    int shatterLevel,
    int penetrateLevel,
    UUID attackerUuid,
    int chainDepth,
    float critRate,
    float precomputedDamage,
    boolean overflow
) {}