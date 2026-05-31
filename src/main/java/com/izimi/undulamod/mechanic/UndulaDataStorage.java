package com.izimi.undulamod.mechanic;


import net.minecraft.entity.LivingEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UndulaDataStorage {

    private static final Map<UUID, UndulaData> dataMap = new ConcurrentHashMap<>();

    public static UndulaData get(LivingEntity entity) {
        return dataMap.getOrDefault(entity.getUuid(), UndulaData.empty());
    }

    public static void set(LivingEntity entity, UndulaData data) {
        if (data.isEmpty()) dataMap.remove(entity.getUuid());
        else dataMap.put(entity.getUuid(), data);
    }

    public static void remove(LivingEntity entity) {
        dataMap.remove(entity.getUuid());
    }
}