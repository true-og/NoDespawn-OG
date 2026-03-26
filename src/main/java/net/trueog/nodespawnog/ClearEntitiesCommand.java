package net.trueog.nodespawnog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.trueog.utilitiesog.UtilitiesOG;

public class ClearEntitiesCommand implements CommandExecutor {

    EntityCleanupScheduler entityCleanupScheduler;

    public ClearEntitiesCommand(EntityCleanupScheduler cleanerUpper) {

        entityCleanupScheduler = cleanerUpper;

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args)
    {

        if (!sender.hasPermission("nodespawnog.clearentities") && !sender.isOp()) {

            if (sender instanceof Player player) {

                UtilitiesOG.trueogMessage(player, "&cERROR: You do not have permission to use that command.");

            }

            return true;

        }

        final int removed = entityCleanupScheduler.clearNonDeathPileDroppedEntitiesInLoadedChunks();
        if (sender instanceof Player player) {

            UtilitiesOG.trueogMessage(player,
                    NoDespawnOG.getPrefix() + "&aEntity cleanup complete. Cleared &e" + removed + "&a entities.");

        } else {

            UtilitiesOG.logToConsole(NoDespawnOG.getPrefix(),
                    "&aEntity cleanup complete. Cleared &e" + removed + "&a entities.");

        }

        return true;

    }

}
