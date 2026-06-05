package com.izimi.undulamod.mechanic;

import com.izimi.undulamod.config.UndulaConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class UndulaScheduler {

    private static final List<PendingTrigger> triggersThisTick = new ArrayList<>();
    private static final Map<Integer, List<PendingTrigger>> delayedTriggers = new HashMap<>();
    private static final List<PullTask> pullTasks = new ArrayList<>();
    private static final Map<UUID, Integer> cooldowns = new HashMap<>();
    private static int nextUndulaId = 1;

    public record PendingTrigger(int undulaId, UndulaContext ctx) {}

    private static class PullTask {
        ServerWorld world; Vec3d pos; float radius; int remainingTicks;
        PullTask(ServerWorld w, Vec3d p, float r, int t) {
            world = w; pos = p; radius = r; remainingTicks = t;
        }
    }

    private static class MergedDamage {
        final LivingEntity target;
        float totalDamage = 0;
        UUID attackerUuid;
        ServerWorld world;
        Vec3d particlePos;
        float particleRadius;
        float particlePitch;

        MergedDamage(LivingEntity t) { this.target = t; }

        void add(float dmg, UUID uuid, ServerWorld w) {
            totalDamage += dmg;
            attackerUuid = uuid;
            world = w;
        }

        void setParticle(Vec3d pos, float radius, float pitch) {
            this.particlePos = pos;
            this.particleRadius = radius;
            this.particlePitch = pitch;
        }

        void apply() {
            if (!target.isAlive()) return;
            PlayerEntity attacker = attackerUuid != null ? world.getPlayerByUuid(attackerUuid) : null;
            target.damage(world.getDamageSources().indirectMagic(target, attacker), totalDamage);

            if (particlePos != null) {
                UndulaTrigger.sendParticleDirect(world, particlePos, particleRadius, (byte) 0, -1);
                world.playSound(null, particlePos.getX(), particlePos.getY(), particlePos.getZ(),
                    SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.8f, particlePitch);
            }
        }
    }

    public static boolean isOnCooldown(UUID uuid) {
        return cooldowns.containsKey(uuid);
    }

    public static void setCooldown(UUID uuid) {
        cooldowns.putIfAbsent(uuid, UndulaConfig.COMMON_COOLDOWN);
    }

    public static void scheduleImmediate(UndulaContext ctx) {
        triggersThisTick.add(new PendingTrigger(nextUndulaId++, ctx));
    }

    public static void scheduleDelayed(UndulaContext ctx, int delay) {
        int at = ctx.world().getServer().getTicks() + delay;
        delayedTriggers.computeIfAbsent(at, k -> new ArrayList<>()).add(new PendingTrigger(nextUndulaId++, ctx));
    }

    public static void schedulePull(ServerWorld world, Vec3d pos, float radius, int duration) {
        pullTasks.add(new PullTask(world, pos, radius, duration));
    }

    public static void tick(ServerWorld world) {
        List<PendingTrigger> toProcess = new ArrayList<>(triggersThisTick);
        triggersThisTick.clear();

        List<PendingTrigger> expired = delayedTriggers.remove(world.getServer().getTicks());
        if (expired != null) toProcess.addAll(expired);

        Map<UUID, MergedDamage> damageMap = new LinkedHashMap<>();

        for (PendingTrigger t : toProcess) {
            UndulaContext ctx = t.ctx();
            Vec3d pos = ctx.position();

            LivingEntity center = (LivingEntity) world.getEntity(ctx.targetUuid());
            boolean alive = center != null && center.isAlive();

            if (alive) {
                setCooldown(ctx.targetUuid());

                UndulaData data = UndulaDataStorage.get(ctx.targetUuid());
                int max = UndulaConfig.getMaxStacks(ctx.shatterLevel(), ctx.penetrateLevel());
                if (ctx.overflow()) {
                    UndulaDataStorage.set(ctx.targetUuid(), new UndulaData(0, max,
                        ctx.shatterLevel(), ctx.penetrateLevel(), data.lastTransferTick(), ctx.critRate()));
                    if (ctx.shatterLevel() > 0) {
                        schedulePull(world, pos,
                            UndulaConfig.getUndulaRadius(ctx.stacks(), ctx.shatterLevel()),
                            UndulaConfig.WHIRLPOOL_DURATION);
                    }
                } else {
                    int ns = Math.min(data.stacks(), max) + 1;
                    UndulaDataStorage.set(ctx.targetUuid(), new UndulaData(ns, max,
                        ctx.shatterLevel(), ctx.penetrateLevel(), data.lastTransferTick(), ctx.critRate()));
                }
                UndulaHandler.sendStack(ctx.targetUuid(), world);
            }

            if (ctx.chainDepth() == 0) {
                Map<UUID, Integer> blacklist = new HashMap<>();
                Map<Integer, List<UUID>> orderedBatch = new LinkedHashMap<>();
                blacklist.put(ctx.targetUuid(), 0);
                UndulaTrigger.buildChain(world, pos, ctx, blacklist, orderedBatch, 0);

                int tick = UndulaConfig.CHAIN_DELAY;
                for (Map.Entry<Integer, List<UUID>> entry : orderedBatch.entrySet()) {
                    for (UUID uuid : entry.getValue()) {
                        LivingEntity target = (LivingEntity) world.getEntity(uuid);
                        if (target == null || !target.isAlive()) continue;

                        UndulaData td = UndulaDataStorage.get(uuid);
                        int tMax = UndulaConfig.getMaxStacks(ctx.shatterLevel(), ctx.penetrateLevel());
                        int cur = Math.min(td.stacks(), tMax);
                        boolean overflow = cur + 1 > tMax;
                        int displayStacks = overflow ? cur : cur + 1;
                        float dmg = UndulaConfig.getUndulaDamage(displayStacks, ctx.shatterLevel(), ctx.penetrateLevel());
                        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);

                        UndulaContext delayedCtx = new UndulaContext(world, targetPos, uuid,
                            displayStacks, ctx.shatterLevel(), ctx.penetrateLevel(), ctx.attackerUuid(),
                            entry.getKey(), ctx.critRate(), dmg, overflow);
                        scheduleDelayed(delayedCtx, tick);
                    }
                    tick += UndulaConfig.CHAIN_DELAY;
                }
            }

            float radius = UndulaConfig.getUndulaRadius(ctx.stacks(), ctx.shatterLevel());
            double rsq = radius * radius;

            List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                Box.from(pos).expand(radius),
                e -> e.isAlive() && !(e instanceof PlayerEntity) && e.squaredDistanceTo(pos) <= rsq
            );

            for (LivingEntity target : targets) {
                MergedDamage md = damageMap.computeIfAbsent(target.getUuid(), k -> new MergedDamage(target));
                md.add(ctx.precomputedDamage(), ctx.attackerUuid(), world);
                md.setParticle(pos, radius, 1.0f + (ctx.stacks() * 0.1f));
            }
        }

        for (MergedDamage md : damageMap.values()) {
            md.apply();
        }

        Iterator<PullTask> pi = pullTasks.iterator();
        while (pi.hasNext()) {
            PullTask task = pi.next();
            if (task.world == world) {
                UndulaTrigger.pullEntitiesContinuous(task.world, task.pos, task.radius);
                if (task.remainingTicks % UndulaConfig.WHIRLPOOL_PARTICLE_INTERVAL == 0)
                    UndulaTrigger.sendParticleDirect(world, task.pos, task.radius, (byte) 2, -1);
                task.remainingTicks--;
                if (task.remainingTicks <= 0) pi.remove();
            }
        }

        Iterator<Map.Entry<UUID, Integer>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int v = entry.getValue() - 1;
            if (v <= -2) it.remove();
            else entry.setValue(v);
        }
    }
}