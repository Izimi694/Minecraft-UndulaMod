package com.izimi.undulamod.client;

import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ClientUndulaParticle {

    public enum Type { WAVE, TRANSFER, WHIRLPOOL }

    private static final int MAX_PARTICLES = 120;
    private static final double[] COS = new double[MAX_PARTICLES];
    private static final double[] SIN = new double[MAX_PARTICLES];
    static {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            double a = i * Math.PI * 2 / MAX_PARTICLES;
            COS[i] = Math.cos(a);
            SIN[i] = Math.sin(a);
        }
    }

    private final Type type;
    private final int entityId;
    private final Vec3d fallbackPos;
    private final float maxRadius;
    private final float tiltAngle;
    private final int lifetime;
    private int age;

    public ClientUndulaParticle(Type type, int entityId, Vec3d fallbackPos, float maxRadius, float tiltAngle, int lifetime) {
        this.type = type;
        this.entityId = entityId;
        this.fallbackPos = fallbackPos;
        this.maxRadius = maxRadius;
        this.tiltAngle = tiltAngle;
        this.lifetime = lifetime;
        this.age = 0;
    }

    public boolean tick(World world) {
        if (age >= lifetime) return true;
        float p = (float) age / lifetime;
        Vec3d pos = getPos(world);
        switch (type) {
            case WAVE -> wave(world, pos, p);
            case TRANSFER -> transfer(world, pos, p);
            case WHIRLPOOL -> whirlpool(world, pos, p);
        }
        age += 2;
        return false;
    }

    private Vec3d getPos(World world) {
        if (entityId >= 0) {
            Entity e = world.getEntityById(entityId);
            if (e != null && e.isAlive()) return fallbackPos;
        }
        return fallbackPos;
    }

    public Vec3d getFallbackPos() { return fallbackPos; }

    private void wave(World w, Vec3d pos, float p) {
        float r = maxRadius * p;
        int n = (int)(r * 8);
        if (n <= 0 || n > MAX_PARTICLES) return;
        float ct = (float) Math.cos(tiltAngle), st = (float) Math.sin(tiltAngle);
        int step = MAX_PARTICLES / n;
        for (int i = 0; i < n; i++) {
            int idx = (i * step) % MAX_PARTICLES;
            double c = COS[idx], s = SIN[idx];
            double front = Math.max(0, c);
            double rr = r * (0.7 + front * 0.7);
            double x = c * rr, y = s * st * rr, z = s * ct * rr;
            w.addParticle(ParticleTypes.BUBBLE,
                pos.getX() + x, pos.getY() + y + 0.5, pos.getZ() + z,
                c * (0.03 + front * 0.05) + s * 0.015,
                s * 0.01,
                s * (0.03 + front * 0.05) - c * 0.015);
        }
    }

    private void transfer(World w, Vec3d pos, float p) {
        float r = maxRadius * p;
        int n = (int)(r * 6 + 4);
        if (n <= 0 || n > MAX_PARTICLES) return;
        int step = MAX_PARTICLES / n;
        for (int i = 0; i < n; i++) {
            int idx = (i * step + w.random.nextInt(step)) % MAX_PARTICLES;
            double c = COS[idx], s = SIN[idx];
            double rr = r * (0.7 + w.random.nextDouble() * 0.5);
            double x = c * rr, z = s * rr;
            w.addParticle(ParticleTypes.RAIN,
                pos.getX() + x, pos.getY() + 0.5, pos.getZ() + z,
                c * (0.04 + w.random.nextDouble() * 0.06) + s * w.random.nextDouble() * 0.02,
                0.03 + w.random.nextDouble() * 0.04,
                s * (0.04 + w.random.nextDouble() * 0.06) - c * w.random.nextDouble() * 0.02);
        }
    }

    private void whirlpool(World w, Vec3d pos, float p) {
        float sr = maxRadius * (1 - p);
        double phase = w.getTime() * 0.05;

        int ring = (int)(sr * 10);
        if (ring > 0 && ring <= MAX_PARTICLES) {
            int step = MAX_PARTICLES / ring;
            for (int i = 0; i < ring; i++) {
                int idx = (i * step + (int)(phase * MAX_PARTICLES / Math.PI / 2)) % MAX_PARTICLES;
                double c = COS[idx], s = SIN[idx];
                double x = c * sr, z = s * sr;
                w.addParticle(ParticleTypes.BUBBLE,
                    pos.getX() + x, pos.getY() + 1.5 - p, pos.getZ() + z,
                    s * 0.10 - c * 0.08, -0.04, -c * 0.10 - s * 0.08);
            }
        }

        int inner = (int)(sr * 6);
        if (inner > 0 && inner <= MAX_PARTICLES) {
            int step = MAX_PARTICLES / inner;
            for (int i = 0; i < inner; i++) {
                int idx = (i * step + (int)(phase * 2 * MAX_PARTICLES / Math.PI / 2)) % MAX_PARTICLES;
                double c = COS[idx], s = SIN[idx];
                double x = c * sr * 0.5, z = s * sr * 0.5;
                w.addParticle(ParticleTypes.BUBBLE,
                    pos.getX() + x, pos.getY() + 1.0 - p, pos.getZ() + z,
                    s * 0.12 - c * 0.12, -0.05, -c * 0.12 - s * 0.12);
            }
        }

        for (int i = 0; i < (int)(p * 5); i++)
            w.addParticle(ParticleTypes.BUBBLE_COLUMN_UP,
                pos.getX() + (w.random.nextDouble() - 0.5) * 0.3,
                pos.getY() + w.random.nextDouble() * 0.3,
                pos.getZ() + (w.random.nextDouble() - 0.5) * 0.3, 0, -0.10, 0);
    }
}