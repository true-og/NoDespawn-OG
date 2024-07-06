package net.trueog.nodespawnog;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import de.tr7zw.changeme.nbtapi.NBTItem;

// Class to watch for in-game events.
public class EventListener implements Listener {

	// Enable the conversion of text from config.yml to objects.
	public FileConfiguration config = NoDespawnOG.getPlugin().getConfig();

	// If despawn time is set to this, it never changes.
	public short minimumValue = -32768;

	// Determine the despawn time using the number taken from the config file.
	private short calculateDespawnTime() {

		// Get the user-defined despawn time from the config file.
		String timeString = config.getString("time-to-death-despawns");

		// If the config value is empty, do this...
		if (timeString == null || timeString.isEmpty()) {

			// Default to 5 minutes (using ticks).
			return Short.valueOf(String.valueOf(-1 * 6000));

		}

		// Deconstruct and interpret the user-specified time string.
		try {

			// Declare a variable to hold the time.
			long time;

			// Extract the unit (s, m, h).
			String unit = timeString.replaceAll("\\d", "");

			// Extract the numeric value.
			int value = Integer.parseInt(timeString.replaceAll("\\D", ""));

			switch (unit.toLowerCase()) {
			case "s":
				// Convert seconds to ticks.
				time = value * 20L;
				break;
			case "m":
				// Convert minutes to ticks.
				time = value * 20L * 60L;
				break;
			case "h":
				// Convert hours to ticks.
				time = value * 20L * 60L * 60L;
				break;
			default:
				// Default to ticks if no unit is provided.
				time = value;
			}

			// Start the countdown to despawn.
			return Short.valueOf(String.valueOf(-1 * time));

		}
		// If the user-specified time string is in an invalid format, do this...
		catch (NumberFormatException error) {

			// Default to 5 minutes (using ticks).
			return Short.valueOf(String.valueOf(-1 * 6000));

		}

	}

	// Prioritize this event listener over all other plugins.
	@EventHandler(priority = EventPriority.HIGHEST)
	// Runs whenever an entity dies.
	public void entityDeathEvent(EntityDeathEvent event) {

		// Check if the entity is a player.
		if (! (event.getEntity() instanceof Player)) {

			// If the entity is not a player, skip this event.
			return;

		}

		// If the player's inventory was not empty...
		if (! event.getDrops().isEmpty()) {

			// Loop through every item in the death pile.
			event.getDrops().forEach(itemStack -> {

				// Get each item from the dropped entities.
				Item droppedItem = event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), itemStack);
				NBTItem nbtItem = new NBTItem(itemStack); 

				// Set the Age tag based on the value set in the config file.
				nbtItem.setShort("Age", config.getBoolean("disable-death-despawns") ? minimumValue : calculateDespawnTime());

				// Save the modified NBT data.
				PersistentDataContainer pdc = droppedItem.getPersistentDataContainer();
				NamespacedKey key = new NamespacedKey(NoDespawnOG.getPlugin(), "nodespawn_age");
				pdc.set(key, PersistentDataType.INTEGER, (int) nbtItem.getShort("Age"));

				// Apply the modified NBTItem back to the ItemStack and update the dropped Item.
				itemStack = nbtItem.getItem();
				droppedItem.setItemStack(itemStack);

			});

			// Clear the original drops.
			event.getDrops().clear();

		}

	}

	// Runs every time a chunk is loaded.
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {

		// Get the chunk that was loaded.
		Chunk chunk = event.getChunk();
		// Loop through every entity in the chunk.
		for (Entity entity : chunk.getEntities()) {

			// If the entity is an Item, do this...
			if (entity instanceof Item) {

				// Cast the entity to an Item since it is one.
				Item item = (Item) entity;

				// Declare a container to hold item data.
				PersistentDataContainer pdc = item.getPersistentDataContainer();
				NamespacedKey key = new NamespacedKey(NoDespawnOG.getPlugin(), "nodespawn_age");
				// Check for item age.
				if (pdc.has(key, PersistentDataType.INTEGER)) {

					// Derive the item age.
					short savedAge = pdc.get(key, PersistentDataType.INTEGER).shortValue();

					// Get the ItemStack from the Item.
					ItemStack itemStack = item.getItemStack();

					// Apply the saved age using NBTItem.
					NBTItem nbtItem = new NBTItem(itemStack);
					nbtItem.setShort("Age", savedAge);
					itemStack = nbtItem.getItem();

					// Update the Item with the modified ItemStack.
					item.setItemStack(itemStack);

				}

			}

		}

	}

	// Runs every time a world is loaded.
	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {

		// Get the world that was loaded.
		World world = event.getWorld();
		// Loop through all loaded chunks in the world.
		for (Chunk chunk : world.getLoadedChunks()) {

			// Loop through every entity in the chunk.
			for (Entity entity : chunk.getEntities()) {

				// If the entity is an Item, do this...
				if (entity instanceof Item) {

					// Cast the entity to an Item since it is one.
					Item item = (Item) entity;

					// Declare a container to hold item data.
					PersistentDataContainer pdc = item.getPersistentDataContainer();
					NamespacedKey key = new NamespacedKey(NoDespawnOG.getPlugin(), "nodespawn_age");
					// Check for item age.
					if (pdc.has(key, PersistentDataType.INTEGER)) {

						// Derive the item age.
						short savedAge = pdc.get(key, PersistentDataType.INTEGER).shortValue();

						// Get the ItemStack from the Item.
						ItemStack itemStack = item.getItemStack();

						// Apply the saved age using NBTItem.
						NBTItem nbtItem = new NBTItem(itemStack);
						nbtItem.setShort("Age", savedAge);
						itemStack = nbtItem.getItem();

						// Update the Item with the modified ItemStack.
						item.setItemStack(itemStack);

					}

				}

			}

		}

	}

}