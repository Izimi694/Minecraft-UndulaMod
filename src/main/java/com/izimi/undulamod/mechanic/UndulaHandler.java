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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class UndulaHandler {

    private static final Set<UUID> hitThisTick = new HashSet<>();
    private static final Set<UUID> deathProcessed = new HashSet<>();

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

        UndulaTrigger.execute(new UndulaContext(world, target, displayStacks,
            shatterLevel, penetrateLevel, player.getUuid(), 0, critRate, dmg, overflow));

        sendStack(target, world);
    }

    public static void onDeath(LivingEntity e, ServerWorld world) {
        if (deathProcessed.contains(e.getUuid())) return;
        deathProcessed.add(e.getUuid());

        UndulaData d = UndulaDataStorage.get(e);
        boolean wasAffected = d.maxStacks() > 0;

        if (!wasAffected) {
            UndulaDataStorage.remove(e);
            return;
        }

        int actualStacks = d.stacks() > 0 ? d.stacks() : 1;
        int spreadStacks = Math.max(1, actualStacks / 2); // 扩散保留50%层数，至少1层

        // 亡语AOE爆炸
        float dmg = UndulaConfig.getUndulaDamage(actualStacks, d.shatterLevel(), d.penetrateLevel());
        float radius = UndulaConfig.getUndulaRadius(actualStacks, d.shatterLevel());
        Vec3d pos = e.getPos().add(0, e.getHeight() * 0.5, 0);

        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
            Box.from(pos).expand(radius),
            target -> target.isAlive() && target != e && !(target instanceof PlayerEntity));

        for (LivingEntity target : targets) {
            target.damage(world.getDamageSources().magic(), dmg);
        }

        UndulaTrigger.sendParticleDirect(world, pos, radius, (byte) 0, -1);
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
            SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.8f, 1.0f + actualStacks * 0.1f);

        // 层数扩散
        UndulaTrigger.spreadStacks(world, e, spreadStacks, d.maxStacks(),
            d.shatterLevel(), d.penetrateLevel());

        UndulaDataStorage.remove(e);
    }

    public static void tick(ServerWorld world) {
        hitThisTick.clear();
        deathProcessed.clear();
        UndulaDataStorage.cleanup(world);
        UndulaScheduler.tick(world);
    }

    public static void sendStack(LivingEntity e, ServerWorld world) {
        int s = UndulaDataStorage.get(e).stacks();
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList())
            if (p.squaredDistanceTo(e.getPos()) < UndulaConfig.PACKET_RANGE_SQ)
                ServerPlayNetworking.send(p, new UndulaParticlePacket(s, (byte) 3, e.getId(), Vec3d.ZERO));
    }
}