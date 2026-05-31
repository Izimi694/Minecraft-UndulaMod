package com.izimi.undulamod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.izimi.undulamod.enchantment.ModEnchantments;
import com.izimi.undulamod.item.ModItems;
import com.izimi.undulamod.mechanic.UndulaHandler;
import com.izimi.undulamod.network.UndulaParticlePacket;
import com.izimi.undulamod.trade.ModTrades;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.world.ServerWorld;

public class UndulaMod implements ModInitializer {
    public static final String MOD_ID = "undulamod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 注册网络包
        PayloadTypeRegistry.playS2C().register(UndulaParticlePacket.ID, UndulaParticlePacket.CODEC);

        // 注册附魔
        ModEnchantments.initialize();

        // 注册物品
        ModItems.initialize();

        ModTrades.initialize(); 

        // 实体死亡事件：亡语传播
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                UndulaHandler.onDeath(entity, serverWorld);
            }
        });

        // 服务端 tick
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world instanceof ServerWorld serverWorld) {
                UndulaHandler.tick(serverWorld);
            }
        });

        LOGGER.info("Janus Undae initialized!");
    }
}