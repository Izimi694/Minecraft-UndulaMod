package com.izimi.undulamod.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class UndulaParticleRenderer {

    private static final List<ClientUndulaParticle> particles = new ArrayList<>();

    public static void add(float radius, float tilt, int life, byte type, int entityId, Vec3d pos) {
        ClientUndulaParticle.Type t = switch (type) {
            case 0 -> ClientUndulaParticle.Type.WAVE;
            case 1 -> ClientUndulaParticle.Type.TRANSFER;
            case 2 -> ClientUndulaParticle.Type.WHIRLPOOL;
            default -> ClientUndulaParticle.Type.WAVE;
        };
        particles.add(new ClientUndulaParticle(t, entityId, pos, radius, tilt, life));
    }

    public static void tick() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.world == null) return;
        particles.removeIf(p -> p.tick(c.world));
    }

}