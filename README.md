# NoDespawn-OG

![Icon](https://git.trueog.net/trueog/NoDespawn-OG/raw/branch/master/assets/nodespawn-og-logo-small.png)

A sophisticated fork of [NoDespawn](https://github.com/rydklein/NoDespawn) made for [TrueOG Network](https://true-og.net/) designed to replace closed-source ClearLag plugins and SaveDeathDrops.

# Target

Purpur 1.19.4

# Authors

BadPylot, NotAlexNoyle.

# Features

- Change the amount of time it takes for dropped items to despawn.

- Chat warnings for when dropped items are soon to despawn.

- Stop player death piles, including their experience orbs, from despawning, or change the amount of time it takes for them to despawn.

- Anti-abuse measures to stop players from using death piles and their experience orbs to create lag machines.

- Clear dropped items and experience orbs with an in-game command (/clearentities), while preserving protected death piles.

- See how much time is left until entity cleanup with an in-game command (/cleanupin).

- Let other plugins exempt their own items and experience orbs from despawning and cleanup.

# Protecting another plugin's entities

Minigame objectives and similar plugin-owned items are ordinary dropped items, so entity cleanup and /clearentities delete them out from under the plugin that spawned them. To exempt one, tag it with the `nodespawn-og:protect` key:

```java
// Do this once, e.g. in a static field.
private static final NamespacedKey PROTECT = Objects.requireNonNull(NamespacedKey.fromString("nodespawn-og:protect"));

// Tag the entity as soon as it exists.
Item item = world.dropItem(location, itemStack);
item.getPersistentDataContainer().set(PROTECT, PersistentDataType.BYTE, (byte) 1);
```

## Tagging rules

- **Tag the entity, not the ItemStack.** NoDespawn-OG reads `Item#getPersistentDataContainer()`, which is the entity's container. A key written to the stack's `ItemMeta` is a different container and has no effect.

- **Use `PersistentDataType.BYTE` with the value `1`.** Any other value, type, or a missing key means unprotected.

- **Tag immediately after spawning.** `World#dropItem` returns the entity; tag it before returning control to the server so no despawn or cleanup pass can see it untagged.

- **Re-tag every new entity.** The tag lives on the entity, not the item. Once a player picks the item up the entity is gone, so a plugin that re-drops the item must tag the new entity too.

- **`ExperienceOrb` works the same way**, using the same key on the orb entity.

- **The tag persists.** It is stored in entity NBT, so it survives chunk unload and server restarts.

## Effect

A tagged entity is skipped by the periodic entity cleanup, by /clearentities, and by despawning: its `ItemDespawnEvent` is cancelled and it is never given despawn timing data. It lives until something removes it, so **the owning plugin is responsible for removing its own protected entities**. To give one back to NoDespawn-OG, remove the key (`container.remove(PROTECT)`) or set it to `(byte) 0`.

Death piles are unaffected by this key; they keep using their existing `disable-death-despawns` and death-pile-protection behavior.

# Permissions

Clear dropped entities with a command (/clearentities):

nodespawnog.clearentities

See how much time is left until entity cleanup (/cleanupin):

nodespawnog.cleanupin

# Building

`./gradlew build clean eclipse`

The resulting .jar file will be in build/libs/

Running the plugin on your server requires [Utilities-OG](https://git.trueog.net/trueog/Utilities-OG) to be installed alongside it.

# Changelog

*Version 4.1.1:*

- Items and experience orbs tagged `nodespawn-og:protect` by another plugin are now exempt from despawning, entity cleanup, and /clearentities.

*Version 4.1:*

- Death pile experience orbs now inherit the same pile ID, owner, and despawn timing as the items in their death pile.

- Death pile protection now counts protected experience orbs toward the same per-player-per-chunk anti-abuse limits as death pile items.

- Entity cleanup and /clearentities now preserve death pile experience orbs and remove non-death-pile experience orbs alongside dropped items.

*Version 4.0:*

- Migrated from NBT-API to a PersistentDataContainer implementation that works reliably on 1.19.4.

- Added per-player-per-chunk death pile limiting to prevent death piles from being used as a lag weapon.

- Added /clearentities to instantly clear non-death-pile dropped items in all loaded chunks.

- Added automatic "entity cleanup" for loaded chunks every 30 minutes with in-game countdown warnings.

- Improved how item despawn timing is tracked.

- Performance optimizations.

*Version 3.0:*

- Setting the age now applies to all items.

- Added more logic for persistence.

- Added support for human readable time format.

*Version 2.1:*

- Fixed NBT-API import in gradle.

- Normalized formatting and indentation.

- Fixed and enhanced despawn time settings.

*Version: 2.0:*

- Adopted GPLv3.

- General refactor.

- Migrated from maven to gradle.

- Updated NBT-API.

- Fixed class object passing.

- More efficient entity check.

**Licensed under the GPLv3.**
