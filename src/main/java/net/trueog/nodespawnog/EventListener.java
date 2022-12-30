package net.trueog.nodespawnog;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import de.tr7zw.changeme.nbtapi.NBTEntity;

// Class to watch for in-game events.
public class EventListener implements Listener {
	
	// Enable the conversion of text from config.yml to objects.
    public FileConfiguration config = Bukkit.getPluginManager().getPlugin("NoDespawn-OG").getConfig();
    
    // If despawn time is set to this, it never changes.
    public short minimumValue = -32768;
    
    // Determine what the despawn time should be using the config and some math.
    private short despawnTime() {
    	
    	// Get the user-set despawn time value.
    	int valueOfDespawnTime = config.getInt("time-to-despawn");

    	// Make sure the value the user declared is within an acceptable range.
	    if (valueOfDespawnTime < 0 || valueOfDespawnTime > 38767) {
	    	
	    	// Return default despawn time of 5 minutes.
	        return 6000;

	    }
	    // If the value the user entered is acceptable, do this.
	    else {

	    	// Convert valid in to short for data type compliance.
	        return (short) (-1 * (valueOfDespawnTime) - 6000);

	    }
	    
    }

    // Override entityDeathEvent with the highest possible priority.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityDeathEvent(EntityDeathEvent event) {

        // If a player did not die, do this.
        if (! event.getEntity().getType().equals(org.bukkit.entity.EntityType.PLAYER)) {

        	System.out.println("Entity died that was NOT a player.");
        	
        	// Do nothing.
            return;

        }
        // If a player did die, but their inventory was empty, do this.
        else if(event.getDrops().isEmpty()) {
        	
        	Bukkit.getPluginManager().getPlugin("NoDespawn-OG").getLogger().info("Player died with an empty inventory.");
        	
        	// Do nothing
            return;

        }
        
        if(config.getBoolean("disable-despawns") == true) {
        	
        	Bukkit.getPluginManager().getPlugin("NoDespawn-OG").getLogger().info("disable-despawns boolean in config is true.");
        	
            // For each intended drop item, do this.
            event.getDrops().forEach(element -> {
            	
            	// Get what the item was.
                Item droppedItem = event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), element);
                
            	Bukkit.getPluginManager().getPlugin("NoDespawn-OG").getLogger().info("Dropped item:" + droppedItem);
                
                // Create a new item drop of the same thing using NBT.
                NBTEntity newDropNBT = new NBTEntity(droppedItem);

                // Set spawned items upon death to stay spawned in infinitely
                newDropNBT.setShort("Age", despawnTime());

            });
        	
        }

        // Delete the original entities to prevent duplication.
        event.getDrops().clear();

    }

}