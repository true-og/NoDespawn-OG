package net.trueog.nodespawnog;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

// init main.
public class NoDespawnOG extends JavaPlugin {

    // Declare plugin instance.
    private static NoDespawnOG plugin;

    // This gets run when the plugin starts.
    @Override
    public void onEnable() {

        // Set plugin instance.
        plugin = this;

        // Register events so that they get run by the server.
        getServer().getPluginManager().registerEvents(new EventListener(), (Plugin) this);

        // Create config file if it doesn't exist.
        saveDefaultConfig();

        // Log valid plugin launch to console.
        getLogger().info("NoDespawn 2.2 Loaded.");

    }

    // This gets run when the plugin shuts down.
    @Override
    public void onDisable() {

        // Log valid plugin shut-down to console.
        getLogger().info("NoDespawn 2.2 Shut Down without errors.");

    }

    // Accessor constructor so that the main class (this) can be referenced from
    // other classes.
    public static NoDespawnOG getPlugin() {

        // Pass instance of main.
        return plugin;

    }

}
