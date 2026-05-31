package com.izimi.undulamod.config;

public class UndulaConfig {

    public static final float BASE_RADIUS = 2.0f;
    public static final float BASE_DAMAGE_BONUS = 0.10f;
    public static final float BASE_UNDULA_DAMAGE = 1.5f;

    public static final int COMMON_COOLDOWN = 10;
    public static final int CHAIN_DELAY = 5;
    public static final int WHIRLPOOL_DURATION = 30;
    public static final int WHIRLPOOL_PARTICLE_INTERVAL = 5;
    public static final int WAVE_LIFETIME = 20;
    public static final int TRANSFER_LIFETIME = 20;
    public static final int WHIRLPOOL_LIFETIME = 30;
    public static final int STACK_LIFETIME = 99999;
    public static final float TILT_ANGLE_RAD = (float) Math.toRadians(15);
    public static final double PACKET_RANGE_SQ = 64 * 64;
    public static final float PULL_STRENGTH = 0.05f;
    public static final float PULL_CAP = 0.08f;
    public static final float PULL_VERTICAL_FACTOR = 0.2f;
    public static final float PULL_MIN_VERTICAL = -0.02f;
    public static final double PULL_MIN_DIST = 0.5;
    public static final double PARTICLE_MERGE_DIST = 2.0;
    public static final double PARTICLE_CULL_DIST = 64.0;
    public static final int MAX_TRANSIENT_PARTICLES = 20;

    public static final float[] LETHALITY_CRIT_RATE = {0f, 0.15f, 0.25f, 0.40f, 0.75f, 0.90f};
    private static final float[] PENETRATE_LAYER_BONUS = {0f, 1.00f, 1.50f, 2.00f};
    private static final float[] SHATTER_RADIUS_MULT = {0f, 0.10f, 0.15f, 0.30f};
    private static final float[] SHATTER_BASE_RADIUS_BONUS = {0f, 0.30f, 0.40f, 0.50f};

    private static final int[] SHATTER_STACKS_REDUCE = {0, 1, 2, 3};
    private static final int[] PENETRATE_STACKS_BONUS = {0, 1, 2, 3};

    private static final float[][] RADIUS_CACHE;
    private static final float[][][] DAMAGE_CACHE;

    static {
        RADIUS_CACHE = new float[17][4];
        for (int s = 0; s <= 16; s++)
            for (int sh = 0; sh <= 3; sh++) {
                float r = BASE_RADIUS;
                if (sh > 0) {
                    r *= (1.0f + SHATTER_BASE_RADIUS_BONUS[sh]);
                    r *= (1.0f + s * SHATTER_RADIUS_MULT[sh]);
                }
                RADIUS_CACHE[s][sh] = r;
            }

        DAMAGE_CACHE = new float[17][4][4];
        for (int s = 0; s <= 16; s++)
            for (int sh = 0; sh <= 3; sh++)
                for (int p = 0; p <= 3; p++) {
                    if (sh > 0) {
                        DAMAGE_CACHE[s][sh][p] = BASE_UNDULA_DAMAGE;
                    } else if (p > 0) {
                        DAMAGE_CACHE[s][sh][p] = BASE_UNDULA_DAMAGE * (1.0f + s * PENETRATE_LAYER_BONUS[p]);
                    } else {
                        DAMAGE_CACHE[s][sh][p] = BASE_UNDULA_DAMAGE * (1.0f + s * BASE_DAMAGE_BONUS);
                    }
                }
    }

    public static int getMaxStacks(int shatterLevel, int penetrateLevel) {
        int base = 5;
        int bonus = PENETRATE_STACKS_BONUS[Math.min(penetrateLevel, 3)];
        int reduce = SHATTER_STACKS_REDUCE[Math.min(shatterLevel, 3)];
        return Math.max(1, base + bonus - reduce);
    }

    public static float getUndulaRadius(int stacks, int shatterLevel) {
        return RADIUS_CACHE[Math.min(stacks, 16)][Math.min(shatterLevel, 3)];
    }

    public static float getUndulaDamage(int stacks, int shatterLevel, int penetrateLevel) {
        return DAMAGE_CACHE[Math.min(stacks, 16)][Math.min(shatterLevel, 3)][Math.min(penetrateLevel, 3)];
    }
}