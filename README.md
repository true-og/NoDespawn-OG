# NoDespawn-OG

![Icon](https://raw.githubusercontent.com/true-og/NoDespawn-OG/master/assets/nodespawn-og-logo-small.png)

A sophisticated fork of [NoDespawn](https://github.com/rydklein/NoDespawn) made for [TrueOG Network](https://true-og.net/) designed to replace closed-source ClearLag plugins and SaveDeathDrops.

# Target

Purpur 1.19.4

# Authors

BadPylot, NotAlexNoyle.

# Features

- Change the amount of time it takes for dropped items to despawn.

- Chat warnings for when dropped items are soon to despawn.

- Stop player death piles from despawning, or change the amount of time it takes for them to despawn.

- Anti-abuse measures to stop players from using death piles to create lag machines.

- Clear dropped items with an in-game command (/clearentities).

- See how much time is left until entity cleanup with an in-game command (/cleanupin).

# Permissions

Clear dropped entities with a command (/clearentities):

nodespawnog.clearentities

See how much time is left until entity cleanup (/cleanupin):

nodespawnog.cleanupin

# Building

`./gradlew build clean eclipse`

The resulting .jar file will be in build/libs/

Running the plugin on your server requires [Utilities-OG](https://github.com/true-og/Utilities-OG) to be installed alongside it.

# Changelog

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
