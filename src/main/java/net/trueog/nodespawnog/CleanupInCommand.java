package net.trueog.nodespawnog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.trueog.utilitiesog.UtilitiesOG;

public class CleanupInCommand implements CommandExecutor {

    private final EntityCleanupScheduler entityCleanupScheduler;

    public CleanupInCommand(EntityCleanupScheduler cleanerUpper) {

        entityCleanupScheduler = cleanerUpper;

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args)
    {

        if (!(sender instanceof Player player)) {

            return true;

        }

        if (!sender.hasPermission("nodespawnog.cleanupin") && !sender.isOp()) {

            UtilitiesOG.trueogMessage(player, "&cERROR: You do not have permission to use that command.");

            return true;

        }

        final long seconds = entityCleanupScheduler.getSecondsRemaining();

        UtilitiesOG.trueogMessage(player,
                NoDespawnOG.getPrefix() + "&6Entity cleanup in " + formatTime(seconds) + "&6.");

        return true;

    }

    private static String unit(long v, String singular, String plural) {

        return (v == 1L) ? singular : plural;

    }

    private static String formatTime(long totalSeconds) {

        totalSeconds = Math.max(0L, totalSeconds);

        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;

        if (minutes <= 0L) {

            return "&e" + seconds + " &6" + unit(seconds, "second", "seconds");

        }

        if (seconds <= 0L) {

            return "&e" + minutes + " &6" + unit(minutes, "minute", "minutes");

        }

        return "&e" + minutes + " &6" + unit(minutes, "minute", "minutes") + " &6and &e" + seconds + " &6"
                + unit(seconds, "second", "seconds");

    }

}