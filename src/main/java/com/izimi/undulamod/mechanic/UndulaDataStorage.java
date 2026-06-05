package com.izimi.undulamod.mechanic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UndulaDataStorage {

    private static final Map<UUID, UndulaData> dataMap = new ConcurrentHashMap<>();
    private static long lastCleanupTick = 0;
    private static final int CLEANUP_INTERVAL = 100;

    public static UndulaData get(UUID uuid) {
        return dataMap.getOrDefault(uuid, UndulaData.empty());
    }

    public static UndulaData get(LivingEntity entity) {
        return get(entity.getUuid());
    }

    public static void set(UUID uuid, UndulaData data) {
        dataMap.put(uuid, data);
    }

    public static void set(LivingEntity entity, UndulaData data) {
        set(entity.getUuid(), data);
    }

    public static void remove(UUID uuid) {
        dataMap.remove(uuid);
    }

    public static void remove(LivingEntity entity) {
        remove(entity.getUuid());
    }

    public static void cleanup(ServerWorld world) {
        long currentTick = world.getServer().getTicks();
        if (currentTick - lastCleanupTick < CLEANUP_INTERVAL) return;
        lastCleanupTick = currentTick;

        dataMap.keySet().removeIf(uuid -> {
            LivingEntity entity = (LivingEntity) world.getEntity(uuid);
            return entity == null || !entity.isAlive();
        });
    }
}