package com.izimi.undulamod.mechanic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.izimi.undulamod.config.UndulaConfig;
import com.izimi.undulamod.network.UndulaParticlePacket;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class UndulaHandler {

    private static final Set<UUID> hitThisTick = new HashSet<>();

    public static void onJanusHit(PlayerEntity player, LivingEntity target,
                                    ItemStack stack, float critRate,
                                    int shatterLevel, int penetrateLevel, ServerWorld world) {
        if (!target.isAlive()) return;
        if (world.random.nextFloat() >= critRate) return;
        if (hitThisTick.contains(target.getUuid())) return;
        hitThisTick.add(target.getUuid());

        UndulaData d = UndulaDataStorage.get(target);
        int max = UndulaConfig.getMaxStacks(shatterLevel, penetrateLevel);
        int raw = d.stacks(), cur = Math.min(raw, max);
        boolean overflow = cur >= max;
        int displayStacks = overflow ? raw : cur + 1;
        float dmg = UndulaConfig.getUndulaDamage(displayStacks, shatterLevel, penetrateLevel);

        Vec3d pos = target.getPos().add(0, target.getHeight() * 0.5, 0);
        UndulaContext ctx = new UndulaContext(world, pos, target.getUuid(),
            displayStacks, shatterLevel, penetrateLevel, player.getUuid(),
            0, critRate, dmg, overflow);

        UndulaScheduler.scheduleImmediate(ctx);
        sendStack(target, world);
    }

    public static void onDeath(LivingEntity e, ServerWorld world) {
        UndulaData d = UndulaDataStorage.get(e);
        if (d.maxStacks() <= 0) return;

        int stacks = d.stacks() > 0 ? d.stacks() : 1;
        float dmg = UndulaConfig.getUndulaDamage(stacks, d.shatterLevel(), d.penetrateLevel());
        float critRate = d.critRate() > 0 ? d.critRate() : 0.10f;
        int max = UndulaConfig.getMaxStacks(d.shatterLevel(), d.penetrateLevel());
        Vec3d pos = e.getPos().add(0, e.getHeight() * 0.5, 0);

        // 层数转移
        UndulaTrigger.spreadStacks(world, e, Math.max(1, stacks / 2),
            max, d.shatterLevel(), d.penetrateLevel());

        // 拉拽
        if (d.shatterLevel() > 0) {
            UndulaScheduler.schedulePull(world, pos,
                UndulaConfig.getUndulaRadius(stacks, d.shatterLevel()),
                UndulaConfig.WHIRLPOOL_DURATION);
        }

        // 构造ctx，扔给调度器
        UndulaContext ctx = new UndulaContext(world, pos, e.getUuid(),
            stacks, d.shatterLevel(), d.penetrateLevel(), null,
            0, critRate, dmg, false);

        UndulaScheduler.scheduleImmediate(ctx);
        UndulaDataStorage.remove(e);
    }

    public static void tick(ServerWorld world) {
        hitThisTick.clear();
        UndulaDataStorage.cleanup(world);
        UndulaScheduler.tick(world);
    }

    public static void sendStack(LivingEntity e, ServerWorld world) {
        int s = UndulaDataStorage.get(e).stacks();
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList())
            if (p.squaredDistanceTo(e.getPos()) < UndulaConfig.PACKET_RANGE_SQ)
                ServerPlayNetworking.send(p, new UndulaParticlePacket(s, (byte) 3, e.getId(), Vec3d.ZERO));
    }

    public static void sendStack(UUID uuid, ServerWorld world) {
        int s = UndulaDataStorage.get(uuid).stacks();
        LivingEntity e = (LivingEntity) world.getEntity(uuid);
        if (e == null) return;
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList())
            if (p.squaredDistanceTo(e.getPos()) < UndulaConfig.PACKET_RANGE_SQ)
                ServerPlayNetworking.send(p, new UndulaParticlePacket(s, (byte) 3, e.getId(), Vec3d.ZERO));
    }
}