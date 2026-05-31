package com.izimi.undulamod.mechanic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
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
    boolean overflow
) {}