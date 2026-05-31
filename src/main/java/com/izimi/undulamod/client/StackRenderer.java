package com.izimi.undulamod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class StackRenderer {

    private static final Map<Integer, Integer> stacks = new HashMap<>();
    private static final double[][] OFFSETS = new double[5][2];

    static {
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5.0;
            OFFSETS[i][0] = Math.cos(a) * 0.35;
            OFFSETS[i][1] = Math.sin(a) * 0.35;
        }
    }

    public static void update(int entityId, int count) {
        if (count <= 0) stacks.remove(entityId);
        else stacks.put(entityId, count);
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null) return;

        float tickDelta = client.getRenderTickCounter().getTickDelta(false);
        double time = (world.getTime() + tickDelta) * 0.08;

        Iterator<Map.Entry<Integer, Integer>> it = stacks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            int id = entry.getKey();
            int n = entry.getValue();
            if (n <= 0) { it.remove(); continue; }

            Entity e = world.getEntityById(id);
            if (e == null || !e.isAlive()) { it.remove(); continue; }

            Vec3d base = e.getPos().add(0, e.getHeight() + 0.3, 0);

            for (int i = 0; i < n; i++) {
                double angle = time - i * 0.6;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double x = cos * OFFSETS[i % 5][0] - sin * OFFSETS[i % 5][1];
                double z = sin * OFFSETS[i % 5][0] + cos * OFFSETS[i % 5][1];
                world.addParticle(ParticleTypes.BUBBLE,
                    base.getX() + x, base.getY() + i * 0.22 - 0.1, base.getZ() + z,
                    0, 0.01, 0);
            }
        }
    }
}