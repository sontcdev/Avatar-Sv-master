package avatar.service;

/**
 * Pure crop-care formulas adapted from ductd26100/avatar-remake FarmService.
 * This class does not change the live farm economy or mutate persisted farms.
 * Source provenance: https://github.com/ductd26100/avatar-remake/blob/main/src/main/java/avatar/service/FarmService.java
 *
 * Donor behavior is evidence of one remake, not verification of original Teamobi rules.
 */
public final class FarmGrowthRules {
    private FarmGrowthRules() { }

    public static final int STEP_MINUTES = 10;
    public static final int MAX_HEALTH = 100;
    public static final int MIN_SICK_HEALTH = 40;
    public static final int CARE_REGEN_PER_STEP = 5;
    public static final int PENALTY_PER_CONDITION = 1;
    public static final int RANDOM_CONDITION_PERCENT = 10;

    public static int waterIntervalMinutes(int growthMinutes) {
        if (growthMinutes >= 1440) return 300;
        if (growthMinutes >= 720) return 120;
        if (growthMinutes >= 360) return 90;
        return 60;
    }

    public static int pestIntervalMinutes(int growthMinutes) {
        if (growthMinutes >= 1440) return 180;
        if (growthMinutes >= 720) return 90;
        if (growthMinutes >= 360) return 60;
        return 30;
    }

    /** A tick uses the conditions at the start of the tick (as in donor code). */
    public static int nextHealth(int currentHealth, boolean arid, boolean worm, boolean grass) {
        if (currentHealth < 0 || currentHealth > 100) {
            throw new IllegalArgumentException("Health outside 0..100");
        }
        if (!arid && !worm && !grass) return Math.min(MAX_HEALTH, currentHealth + CARE_REGEN_PER_STEP);
        int penalties = (arid ? 1 : 0) + (worm ? 1 : 0) + (grass ? 1 : 0);
        // Donor clamps to 40 after each penalty.
        int result = currentHealth;
        for (int i = 0; i < penalties; i++) result = Math.max(result - PENALTY_PER_CONDITION, MIN_SICK_HEALTH);
        return result;
    }

    /** Donor seed of 1 true / 9 false, i.e. 10% for each independent attempt. */
    public static boolean conditionTriggered(int rollZeroToNinetyNine) {
        if (rollZeroToNinetyNine < 0 || rollZeroToNinetyNine >= 100)
            throw new IllegalArgumentException("Roll outside 0..99");
        return rollZeroToNinetyNine < RANDOM_CONDITION_PERCENT;
    }

    /** Only COMPLETE ten-minute ticks are processed; never simulate past maturity. */
    public static long completeTicks(long plantedEpochMs, long lastUpdatedEpochMs,
                                     long nowEpochMs, int growthMinutes) {
        if (growthMinutes <= 0 || lastUpdatedEpochMs < plantedEpochMs)
            throw new IllegalArgumentException("Invalid crop timing");
        long durationMs = Math.multiplyExact((long) growthMinutes, 60_000L);
        long deadline = Math.addExact(plantedEpochMs, durationMs);
        long until = Math.min(nowEpochMs, deadline);
        if (until <= lastUpdatedEpochMs) return 0;
        return (until - lastUpdatedEpochMs) / (STEP_MINUTES * 60_000L);
    }
}
