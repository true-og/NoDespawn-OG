package net.trueog.nodespawnog;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

// init main.
public class Main extends JavaPlugin {
    
	// config.yml file to object initialization.
    public FileConfiguration config = getConfig();

    // Hook into standard plugin enable function from bukkit.
    @Override
    public void onEnable() {

        // Create config file if it doesn't exist.
        saveDefaultConfig();

        // Reload configuration options using bukkit function.
        reloadConfig();

        // Announce successful plugin load in console.
        getLogger().info("NoDespawn-OG Loaded!");

    }

}