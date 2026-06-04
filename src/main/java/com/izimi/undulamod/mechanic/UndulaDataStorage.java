package com.izimi.undulamod.mechanic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UndulaDataStorage {

    private static final Map<UUID, UndulaData> dataMap = new ConcurrentHashMap<>();
    private static long lastCleanupTick = 0;
    private static final int CLEANUP_INTERVAL = 100; // 每5秒清理一次

    public static UndulaData get(LivingEntity entity) {
        return dataMap.getOrDefault(entity.getUuid(), UndulaData.empty());
    }

    public static void set(LivingEntity entity, UndulaData data) {
        dataMap.put(entity.getUuid(), data);
    }

    public static void remove(LivingEntity entity) {
        dataMap.remove(entity.getUuid());
    }

    public static void cleanup(ServerWorld world) {
        long currentTick = world.getServer().getTicks();
        if (currentTick - lastCleanupTick < CLEANUP_INTERVAL) return;
        lastCleanupTick = currentTick;

        dataMap.entrySet().removeIf(entry -> {
            LivingEntity entity = (LivingEntity) world.getEntity(entry.getKey());
            return entity == null || !entity.isAlive();
        });
    }

    public static int size() {
        return dataMap.size();
    }
}