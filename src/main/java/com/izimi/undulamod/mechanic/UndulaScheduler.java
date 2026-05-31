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
        ServerWorld world; Vec3d pos; float radius; int remainingTicks; int entityId;
        PullTask(ServerWorld w, Vec3d p, float r, int t, int e) {
            world = w; pos = p; radius = r; remainingTicks = t; entityId = e;
        }
    }

    private static class MergedDamage {
        final LivingEntity target;
        float totalDamage = 0;
        UUID attackerUuid;
        LivingEntity center;
        ServerWorld world;
        Vec3d particlePos;
        float particleRadius;
        float particlePitch;

        MergedDamage(LivingEntity t) { this.target = t; }

        void add(float dmg, UUID uuid, LivingEntity c, ServerWorld w) {
            totalDamage += dmg;
            attackerUuid = uuid;
            center = c;
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
            target.damage(world.getDamageSources().indirectMagic(center, attacker), totalDamage);

            if (particlePos != null) {
                UndulaTrigger.sendParticleDirect(world, particlePos, particleRadius, (byte) 0, -1);
                world.playSound(null, particlePos.getX(), particlePos.getY(), particlePos.getZ(),
                    SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.8f, particlePitch);
            }
        }
    }

    public static boolean isOnCooldown(LivingEntity e) {
        return cooldowns.containsKey(e.getUuid());
    }

    public static void setCooldown(LivingEntity e) {
        cooldowns.putIfAbsent(e.getUuid(), UndulaConfig.COMMON_COOLDOWN);
    }

    public static void scheduleImmediate(UndulaContext ctx) {
        triggersThisTick.add(new PendingTrigger(nextUndulaId++, ctx));
    }

    public static void scheduleDelayed(UndulaContext ctx, int delay) {
        int at = ctx.world().getServer().getTicks() + delay;
        delayedTriggers.computeIfAbsent(at, k -> new ArrayList<>()).add(new PendingTrigger(nextUndulaId++, ctx));
    }

    public static void schedulePull(ServerWorld world, Vec3d pos, float radius, int duration, int entityId) {
        pullTasks.add(new PullTask(world, pos, radius, duration, entityId));
    }

    public static void tick(ServerWorld world) {
        List<PendingTrigger> toProcess = new ArrayList<>(triggersThisTick);
        triggersThisTick.clear();

        List<PendingTrigger> expired = delayedTriggers.remove(world.getServer().getTicks());
        if (expired != null) toProcess.addAll(expired);

        Map<UUID, MergedDamage> damageMap = new LinkedHashMap<>();

        for (PendingTrigger t : toProcess) {
            UndulaContext ctx = t.ctx();
            LivingEntity center = ctx.center();
            if (!center.isAlive()) continue;

            // ====== 结算线：冷却 ======
            setCooldown(center);

            // ====== 结算线：层数 ======
            UndulaData data = UndulaDataStorage.get(center);
            int max = UndulaConfig.getMaxStacks(ctx.shatterLevel(), ctx.penetrateLevel());
            if (ctx.overflow()) {
                UndulaDataStorage.set(center, new UndulaData(0, max,
                    ctx.shatterLevel(), ctx.penetrateLevel(), data.lastTransferTick(), ctx.critRate()));
                if (ctx.shatterLevel() > 0) {
                    schedulePull(world, center.getPos(),
                        UndulaConfig.getUndulaRadius(ctx.stacks(), ctx.shatterLevel()),
                        UndulaConfig.WHIRLPOOL_DURATION, center.getId());
                }
            } else {
                int ns = Math.min(data.stacks(), max) + 1;
                UndulaDataStorage.set(center, new UndulaData(ns, max,
                    ctx.shatterLevel(), ctx.penetrateLevel(), data.lastTransferTick(), ctx.critRate()));
            }
            UndulaHandler.sendStack(center, world);

            // ====== 逻辑线：传染树 + 延迟调度 ======
            if (ctx.chainDepth() == 0) {
                Map<UUID, Integer> blacklist = new HashMap<>();
                Map<Integer, List<UUID>> orderedBatch = new LinkedHashMap<>();
                blacklist.put(center.getUuid(), 0);
                UndulaTrigger.buildChain(world, center, ctx, blacklist, orderedBatch, 0);

                int tick = UndulaConfig.CHAIN_DELAY;
                for (Map.Entry<Integer, List<UUID>> entry : orderedBatch.entrySet()) {
                    for (UUID uuid : entry.getValue()) {
                        LivingEntity target = (LivingEntity) world.getEntity(uuid);
                        if (target == null || !target.isAlive()) continue;

                        UndulaData td = UndulaDataStorage.get(target);
                        int tMax = UndulaConfig.getMaxStacks(ctx.shatterLevel(), ctx.penetrateLevel());
                        int cur = Math.min(td.stacks(), tMax);
                        boolean overflow = cur + 1 > tMax;
                        int displayStacks = overflow ? cur : cur + 1;
                        float dmg = UndulaConfig.getUndulaDamage(displayStacks, ctx.shatterLevel(), ctx.penetrateLevel());

                        UndulaContext delayedCtx = new UndulaContext(world, target, displayStacks,
                            ctx.shatterLevel(), ctx.penetrateLevel(), ctx.attackerUuid(),
                            entry.getKey(), ctx.critRate(), dmg, overflow);
                        scheduleDelayed(delayedCtx, tick);
                    }
                    tick += UndulaConfig.CHAIN_DELAY;
                }
            }

            // ====== 伤害收集 ======
            float radius = UndulaConfig.getUndulaRadius(ctx.stacks(), ctx.shatterLevel());
            double rsq = radius * radius;
            Vec3d pos = center.getPos().add(0, center.getHeight() * 0.25, 0);
            List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                Box.from(center.getPos()).expand(radius),
                e -> e.isAlive() && !(e instanceof PlayerEntity) && e.squaredDistanceTo(center.getPos()) <= rsq
            );

            for (LivingEntity target : targets) {
                if (!target.isAlive()) continue;
                MergedDamage md = damageMap.computeIfAbsent(target.getUuid(), k -> new MergedDamage(target));
                md.add(ctx.precomputedDamage(), ctx.attackerUuid(), center, world);
                md.setParticle(pos, radius, 1.0f + (ctx.stacks() * 0.1f));
            }
        }

        // ====== 统一伤害 + 粒子 + 音效 ======
        for (MergedDamage md : damageMap.values()) {
            md.apply();
        }

        // ====== 拉拽 ======
        Iterator<PullTask> pi = pullTasks.iterator();
        while (pi.hasNext()) {
            PullTask task = pi.next();
            if (task.world == world) {
                UndulaTrigger.pullEntitiesContinuous(task.world, task.pos, task.radius);
                if (task.remainingTicks % UndulaConfig.WHIRLPOOL_PARTICLE_INTERVAL == 0)
                    UndulaTrigger.sendParticleDirect(world, task.pos, task.radius, (byte) 2, task.entityId);
                task.remainingTicks--;
                if (task.remainingTicks <= 0) pi.remove();
            }
        }

        // ====== 冷却递减 ======
        Iterator<Map.Entry<UUID, Integer>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int v = entry.getValue() - 1;
            if (v <= -2) it.remove();
            else entry.setValue(v);
        }
    }
}