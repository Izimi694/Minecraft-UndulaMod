package com.izimi.undulamod.mechanic;

import com.izimi.undulamod.UndulaMod;
import com.izimi.undulamod.config.UndulaConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class WhirlpoolLogic {

    public static void spawn(ServerWorld world, Vec3d pos, int stacks, int shatterLevel) {
        float radius = UndulaConfig.getUndulaRadius(stacks, shatterLevel);

        UndulaMod.LOGGER.info("[Whirlpool] Spawning at ({},{},{}), radius={}, stacks={}, shatterLevel={}",
            String.format("%.1f", pos.getX()),
            String.format("%.1f", pos.getY()),
            String.format("%.1f", pos.getZ()),
            String.format("%.1f", radius), stacks, shatterLevel);

        int targetsPulled = applyWhirlpoolEffect(world, pos, radius);
        spawnWhirlpoolParticles(world, pos, radius);

        UndulaMod.LOGGER.info("[Whirlpool] Pulled {} entities", targetsPulled);
    }

    private static int applyWhirlpoolEffect(ServerWorld world, Vec3d pos, float radius) {
        Box box = Box.from(pos).expand(radius);
        List<LivingEntity> entities = world.getEntitiesByClass(
            LivingEntity.class, box, e -> e.isAlive()
        );

        for (LivingEntity entity : entities) {
            Vec3d entityPos = entity.getPos();
            double dist = entityPos.distanceTo(pos);

            if (dist > radius || dist < 0.1) continue;

            Vec3d pullDir = pos.subtract(entityPos).normalize();
            double strength = (1.0 - dist / radius) * 0.3;
            entity.addVelocity(pullDir.multiply(strength));
            entity.velocityModified = true;
        }

        return entities.size();
    }

    private static void spawnWhirlpoolParticles(ServerWorld world, Vec3d pos, float radius) {
        int particleCount = (int)(radius * 20);

        for (int i = 0; i < particleCount; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double r = world.random.nextDouble() * radius;
            double x = pos.getX() + Math.cos(angle) * r;
            double z = pos.getZ() + Math.sin(angle) * r;

            world.spawnParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                x, pos.getY() + world.random.nextDouble() * 2, z,
                1, 0, 0, 0, 0.05);
        }
    }
}