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
        if (world.random.nextFloat() >= critRate) return;
        if (hitThisTick.contains(target.getUuid())) return;
        hitThisTick.add(target.getUuid());

        UndulaData d = UndulaDataStorage.get(target);
        int max = UndulaConfig.getMaxStacks(shatterLevel, penetrateLevel);
        int raw = d.stacks(), cur = Math.min(raw, max);
        boolean overflow = cur >= max;
        int displayStacks = overflow ? raw : cur + 1;
        float dmg = UndulaConfig.getUndulaDamage(displayStacks, shatterLevel, penetrateLevel);

        UndulaTrigger.execute(new UndulaContext(world, target, displayStacks,
            shatterLevel, penetrateLevel, player.getUuid(), 0, critRate, dmg, overflow));

        sendStack(target, world);
    }

    public static void onDeath(LivingEntity e, ServerWorld world) {
        UndulaData d = UndulaDataStorage.get(e);
        if (d.isEmpty()) return;
        if (d.stacks() > 0) {
            UndulaTrigger.spreadStacks(world, e, d.stacks(), d.maxStacks(),
                d.shatterLevel(), d.penetrateLevel());
        }
        UndulaDataStorage.remove(e);
    }

    public static void tick(ServerWorld world) {
        hitThisTick.clear();
        UndulaScheduler.tick(world);
    }

    public static void sendStack(LivingEntity e, ServerWorld world) {
        int s = UndulaDataStorage.get(e).stacks();
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList())
            if (p.squaredDistanceTo(e.getPos()) < UndulaConfig.PACKET_RANGE_SQ)
                ServerPlayNetworking.send(p, new UndulaParticlePacket(s, (byte) 3, e.getId(), Vec3d.ZERO));
    }
}