package com.izimi.undulamod.mechanic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.izimi.undulamod.config.UndulaConfig;
import com.izimi.undulamod.network.UndulaParticlePacket;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class UndulaTrigger {

    private static final int MAX_CHAIN_DEPTH = 64;

    public static void execute(UndulaContext ctx) {
        UndulaScheduler.scheduleImmediate(ctx);
    }

    public static void buildChain(ServerWorld world, LivingEntity source,
                                UndulaContext ctx, Map<UUID, Integer> blacklist,
                                Map<Integer, List<UUID>> orderedBatch,
                                int currentTag) {
        if (currentTag >= MAX_CHAIN_DEPTH) return;

        float radius = UndulaConfig.getUndulaRadius(ctx.stacks(), ctx.shatterLevel());
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
            Box.from(source.getPos()).expand(radius),
            e -> e.isAlive() && !(e instanceof PlayerEntity)
                && !blacklist.containsKey(e.getUuid()));

        targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(source.getPos())));

        int nextTag = currentTag + 1;
        List<LivingEntity> successList = new ArrayList<>();

        for (LivingEntity target : targets) {
            if (world.random.nextFloat() >= ctx.critRate()) continue;
            blacklist.put(target.getUuid(), nextTag);
            orderedBatch.computeIfAbsent(nextTag, k -> new ArrayList<>()).add(target.getUuid());
            successList.add(target);
        }

        for (int i = successList.size() - 1; i >= 0; i--) {
            buildChain(world, successList.get(i), ctx, blacklist, orderedBatch, nextTag);
        }
    }

    public static void pullEntitiesContinuous(ServerWorld world, Vec3d pos, float radius) {
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class,
                Box.from(pos).expand(radius), e -> e.isAlive() && !(e instanceof PlayerEntity))) {
            double dist = e.getPos().distanceTo(pos);
            if (dist > radius || dist < UndulaConfig.PULL_MIN_DIST) continue;
            Vec3d dir = pos.subtract(e.getPos()).normalize();
            double s = (1.0 - dist / radius) * 0.5;
            e.addVelocity(dir.x * s, Math.max(dir.y * s * 0.3, -0.05), dir.z * s);
            e.velocityModified = true;
        }
    }

    public static void spreadStacks(ServerWorld world, LivingEntity source, int remaining,
                                     int maxLimit, int sourceShatter, int sourcePenetrate) {
        if (remaining <= 1) return;
        Vec3d pos = source.getPos();
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
            Box.from(pos).expand(UndulaConfig.getUndulaRadius(remaining, sourceShatter) + 1),
            e -> e != source && e.isAlive() && !(e instanceof PlayerEntity));
        if (targets.isEmpty()) return;

        targets.sort((a, b) -> Double.compare(a.squaredDistanceTo(pos), b.squaredDistanceTo(pos)));
        LivingEntity target = targets.get(0);

        long currentTick = world.getServer().getTicks();
        UndulaData d = UndulaDataStorage.get(target);
        if (currentTick - d.lastTransferTick() < 2) return;

        int targetMax = d.isEmpty() ? maxLimit : Math.min(d.maxStacks(), maxLimit);
        int finalStacks = d.stacks() + remaining - 1;

        UndulaDataStorage.set(target, new UndulaData(finalStacks, targetMax, sourceShatter, sourcePenetrate, currentTick, d.critRate()));
        sendParticle(world, target.getPos().add(0, target.getHeight() + 0.5, 0),
            UndulaConfig.getUndulaRadius(finalStacks, sourceShatter), (byte) 1, target.getId());
        UndulaHandler.sendStack(target, world);
    }

    public static void sendParticleDirect(ServerWorld world, Vec3d pos, float radius, byte type, int eid) {
        sendParticle(world, pos, radius, type, eid);
    }

    private static void sendParticle(ServerWorld world, Vec3d pos, float radius, byte type, int eid) {
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(p, new UndulaParticlePacket(radius, type, eid, pos));
    }
}