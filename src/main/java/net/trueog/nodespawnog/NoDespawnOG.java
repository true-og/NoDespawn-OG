package net.trueog.nodespawnog;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.trueog.utilitiesog.UtilitiesOG;

// Init main.
public class NoDespawnOG extends JavaPlugin {

    // Declare fields.
    private static NoDespawnOG plugin;
    private EntityCleanupScheduler cleanupScheduler;
    private EventListener eventListener;
    public static List<World> serverWorlds;

    // This gets run when the plugin starts.
    @Override
    public void onEnable() {

        // Set plugin instance.
        plugin = this;

        // Create config file if it doesn't exist.
        saveDefaultConfig();

        // Start the entity cleanup scheduler.
        cleanupScheduler = new EntityCleanupScheduler();
        cleanupScheduler.runTaskTimer(this, 20L, 20L);

        // Register events so that they get run by the server and its functions are
        // callable by this class.
        eventListener = new EventListener();
        getServer().getPluginManager().registerEvents(eventListener, this);

        // Scan chunks that were loaded before the plugin got enabled for entities to
        // track.
        Bukkit.getScheduler().runTask(this, eventListener::scanLoadedChunks);

        // Initialize the /clearentities command.
        if (getCommand("clearentities") != null) {

            getCommand("clearentities").setExecutor(new ClearEntitiesCommand(cleanupScheduler));

        }

        // Initialize the /cleanupin command.
        if (getCommand("cleanupin") != null) {

            getCommand("cleanupin").setExecutor(new CleanupInCommand(cleanupScheduler));

        }

        // Log valid plugin launch to console.
        getLogger().info("NoDespawn-OG 4.0 Loaded.");

    }

    // This gets run when the plugin shuts down.
    @Override
    public void onDisable() {

        if (cleanupScheduler != null) {

            cleanupScheduler.cancel();
            cleanupScheduler = null;

        }

        eventListener = null;

        // Log valid plugin shut-down to console.
        UtilitiesOG.logToConsole(getPrefix(), "Shut down without errors.");

    }

    // Getter for the plugin instance running on the server.
    public static NoDespawnOG getPlugin() {

        return plugin;

    }

    public static String getPrefix() {

        return "&7[&eNoDespawn&f-&4OG&7] &r";

    }

    public static List<World> getServerWorlds() {

        serverWorlds = Bukkit.getWorlds();

        return serverWorlds;

    }

    public static void broadcastCleanupMessage(String message) {

        Bukkit.getOnlinePlayers().forEach((Player player) -> UtilitiesOG.trueogMessage(player, message));

    }

}