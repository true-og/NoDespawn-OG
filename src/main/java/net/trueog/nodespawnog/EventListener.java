package net.trueog.nodespawnog;

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
	public FileConfiguration config = NoDespawnOG.getPlugin().getConfig();

	// If despawn time is set to this, it never changes.
	public short minimumValue = -32768;

	// Get the user-set death despawn time value.
	private short valueOfDespawnTime() {

		// Declare an empty short object for holding the death despawn time from the config file after input validation.
		short valueOfDespawnTime = 0;

		// Read config for death despawn in such a way as to validate that it would be a valid 'short'.
		try {

			// TOOD: Fix any valueOfDespawnTime causing an instant despawn.
			// Get short from config file with error handling.
			valueOfDespawnTime = Short.valueOf(config.getString("time-to-death-despawns"));

			// Set short to valid user-set death despawn time.
			// This will only run if the short the user specified in the config was in range.
			return valueOfDespawnTime;

		}
		// If the value the user set in the config is out of range, do this.
		catch(NumberFormatException error) {

			// TODO: DEV LOG, REMOVE!
			NoDespawnOG.getPlugin().getLogger().info("Exception caught! " + error.getMessage() + "Time to despawns:" + config.getInt("time-to-death-despawns"));

			// Return default value of 6000 ticks (5 minutes) to replace invalid user-set value in the config.
			return Short.valueOf("6000");

		}

	}

	// Override entityDeathEvent with the highest possible priority.
	@EventHandler(priority = EventPriority.HIGHEST)
	public void entityDeathEvent(EntityDeathEvent PlayerDeathEvent) {

		// If a player died, do this.
		if (PlayerDeathEvent.getEntity().getType().equals(org.bukkit.entity.EntityType.PLAYER)) {

			// If the player's inventory was not empty, do this.
			if(! PlayerDeathEvent.getDrops().isEmpty()) {

				// If disable death despawns option is enabled in config file, do this.
				if(config.getBoolean("disable-death-despawns") == true) {

					// For each intended drop item, do this.
					PlayerDeathEvent.getDrops().forEach(element -> {

						// Get what the item was.
						Item droppedItem = PlayerDeathEvent.getEntity().getWorld().dropItem(PlayerDeathEvent.getEntity().getLocation(), element);

						// Create a new item drop of the same thing using NBT.
						NBTEntity newDropNBT = new NBTEntity(droppedItem);

						// Set items upon death to stay spawned in infinitely
						newDropNBT.setShort("Age", minimumValue);

					});

					// Delete the original entities to prevent duplication.
					PlayerDeathEvent.getDrops().clear();

				}
				// If disable death despawns is disabled in the config file, do this.
				else {

					// For each intended drop item, do this.
					PlayerDeathEvent.getDrops().forEach(element -> {

						// Get what the item was.
						Item droppedItem = PlayerDeathEvent.getEntity().getWorld().dropItem(PlayerDeathEvent.getEntity().getLocation(), element);

						// Create a new item drop of the same thing using NBT.
						NBTEntity newDropNBT = new NBTEntity(droppedItem);

						// Set items upon death to stay spawned in for the calculated amount of time.
						newDropNBT.setShort("Age", valueOfDespawnTime());

					});

					// TODO: DEV LOG, REMOVE!
					NoDespawnOG.getPlugin().getLogger().info("Items will despawn in: " + valueOfDespawnTime());

					// Delete the original entities to prevent duplication.
					PlayerDeathEvent.getDrops().clear();

				}

			}

		}

	}

}