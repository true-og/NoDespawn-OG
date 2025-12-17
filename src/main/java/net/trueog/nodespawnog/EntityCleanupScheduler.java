package net.trueog.nodespawnog;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class EntityCleanupScheduler extends BukkitRunnable {

    private static final int DEFAULT_INTERVAL_TICKS = 30 * 60 * 20;
    private static final int STEP_TICKS = 20;
    private static final int TICKS_PER_SECOND = 20;
    private static final int WARN_5M_TICKS = 5 * 60 * TICKS_PER_SECOND; // 6000
    private static final int WARN_2M_TICKS = 2 * 60 * TICKS_PER_SECOND; // 2400
    private static final int WARN_1M_TICKS = 1 * 60 * TICKS_PER_SECOND; // 1200
    private static final int WARN_30S_TICKS = 30 * TICKS_PER_SECOND; // 600
    private static final int WARN_10S_TICKS = 10 * TICKS_PER_SECOND; // 200
    private static final int WARN_3S_TICKS = 3 * TICKS_PER_SECOND; // 60
    private static final int WARN_2S_TICKS = 2 * TICKS_PER_SECOND; // 40
    private static final int WARN_1S_TICKS = 1 * TICKS_PER_SECOND; // 20
    private final int intervalTicks;
    private int ticksRemaining;
    private final Set<Integer> warningTicks = new HashSet<>();
    private final NamespacedKey keyDeath = new NamespacedKey(NoDespawnOG.getPlugin(), "nodespawn_death");

    public EntityCleanupScheduler() {

        int raw = Math.max(STEP_TICKS, readIntervalTicks());
        raw = alignToStepCeiling(raw);

        this.intervalTicks = raw;
        this.ticksRemaining = intervalTicks;

        initWarningTicks();

    }

    private int readIntervalTicks() {

        final String raw = NoDespawnOG.getPlugin().getConfig().getString("entity-cleanup-interval", "30m");

        return parseDurationToTicks(raw, DEFAULT_INTERVAL_TICKS);

    }

    // Parses time units "30m", "10s", "1h", "6000" into ticks.
    private static int parseDurationToTicks(String raw, int fallbackTicks) {

        if (raw == null) {

            return fallbackTicks;

        }

        final String s = StringUtils.trim(raw);
        if (StringUtils.isEmpty(s)) {

            return fallbackTicks;

        }

        try {

            final String unit = StringUtils.lowerCase(s.replaceAll("[\\d\\s]", ""), Locale.ROOT);
            final String digits = s.replaceAll("[^0-9]", "");
            if (StringUtils.isEmpty(digits)) {

                return fallbackTicks;

            }

            final long value = Long.parseLong(digits);

            final long ticks = switch (unit) {

                case "s" -> value * 20L;
                case "m" -> value * 20L * 60L;
                case "h" -> value * 20L * 60L * 60L;
                default -> value;

            };

            if (ticks <= 0L) {

                return fallbackTicks;

            }

            if (ticks > Integer.MAX_VALUE) {

                return Integer.MAX_VALUE;

            }

            return (int) ticks;

        } catch (Exception error) {

            return fallbackTicks;

        }

    }

    private static int alignToStepCeiling(int ticks) {

        final int q = (ticks + STEP_TICKS - 1) / STEP_TICKS;
        final long aligned = (long) q * (long) STEP_TICKS;

        return aligned > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) aligned;

    }

    private static int alignToStepNearest(int ticks) {

        final int q = (ticks + (STEP_TICKS / 2)) / STEP_TICKS;

        return q * STEP_TICKS;

    }

    // Dynamic warnings.
    private void initWarningTicks() {

        warningTicks.clear();

        addWarning(WARN_5M_TICKS);
        addWarning(WARN_2M_TICKS);
        addWarning(WARN_1M_TICKS);
        addWarning(WARN_30S_TICKS);
        addWarning(WARN_10S_TICKS);
        addWarning(WARN_3S_TICKS);
        addWarning(WARN_2S_TICKS);
        addWarning(WARN_1S_TICKS);

        // Scaling: if interval is under 5 minutes, add relative warnings at 50% and 25%
        if (intervalTicks >= WARN_5M_TICKS) {

            return;

        }

        // 50%
        addRelativeWarning(intervalTicks / 2);

        // 25%
        addRelativeWarning(intervalTicks / 4);

    }

    private void addWarning(int ticks) {

        // After reset, first checked value is (intervalTicks - STEP_TICKS).
        if (ticks < STEP_TICKS) {

            return;

        }

        if (ticks > intervalTicks - STEP_TICKS) {

            return;

        }

        warningTicks.add(ticks);

    }

    private void addRelativeWarning(int rawTicks) {

        final int t = alignToStepNearest(rawTicks);
        if (t <= WARN_10S_TICKS) {

            return;

        }

        addWarning(t);

    }

    @Override
    public void run() {

        ticksRemaining -= STEP_TICKS;
        if (ticksRemaining > 0 && warningTicks.contains(ticksRemaining)) {

            // 3... 2... ...1 Countdown.
            if (ticksRemaining == WARN_3S_TICKS || ticksRemaining == WARN_2S_TICKS || ticksRemaining == WARN_1S_TICKS) {

                final long sec = ticksRemaining / 20L;

                NoDespawnOG.broadcastCleanupMessage(NoDespawnOG.getPrefix() + "&6Entity cleanup in &e" + sec + "&6...");

                return;

            }

            // Dynamic regular entity cleanup broadcast.
            NoDespawnOG.broadcastCleanupMessage(
                    NoDespawnOG.getPrefix() + "&6Entity cleanup in " + formatDurationTicks(ticksRemaining) + "&6.");

            return;

        }

        if (ticksRemaining > 0) {

            return;

        }

        final int removed = clearNonDeathPileDroppedItemsInLoadedChunks();

        // Success broadcast.
        NoDespawnOG.broadcastCleanupMessage(
                NoDespawnOG.getPrefix() + "&aEntity cleanup complete. Cleared &e" + removed + "&a entities.");

        ticksRemaining = intervalTicks;

    }

    // Parses ticks into a human-readable time string.
    private static String formatDurationTicks(int ticks) {

        final long totalSeconds = ticks / 20L;
        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;
        if (minutes <= 0L) {

            return "&e" + seconds + " &6" + (seconds == 1L ? "second" : "seconds");

        }

        if (seconds == 0L) {

            return "&e" + minutes + " &6" + (minutes == 1L ? "minute" : "minutes");

        }

        return "&e" + minutes + " &6" + (minutes == 1L ? "minute" : "minutes") + " &e" + seconds + " &6"
                + (seconds == 1L ? "second" : "seconds");

    }

    int clearNonDeathPileDroppedItemsInLoadedChunks() {

        int removed = 0;
        final List<World> worlds = Bukkit.getWorlds();
        for (World world : worlds) {

            for (Chunk chunk : world.getLoadedChunks()) {

                for (Entity entity : chunk.getEntities()) {

                    if (!(entity instanceof Item item) || !item.isValid()) {

                        continue;

                    }

                    final Byte deathFlag = item.getPersistentDataContainer().get(keyDeath, PersistentDataType.BYTE);

                    if (deathFlag == null || deathFlag.byteValue() != (byte) 1) {

                        item.remove();

                        removed++;

                    }

                }

            }

        }

        return removed;

    }

    int getIntervalTicks() {

        return intervalTicks;

    }

    int getTicksRemaining() {

        return Math.max(0, ticksRemaining);

    }

    long getSecondsRemaining() {

        // Round up so /cleanupin doesn't show "0s".
        return (getTicksRemaining() + 19L) / 20L;

    }

}