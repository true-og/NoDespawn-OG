# NoDespawn-OG

![Icon](https://github.com/NotAlexNoyle/NoDespawn-OG/blob/main/assets/nodespawn-og-logo.png?raw=true)

A more sophisticated fork of NoDespawn made for the [TrueOG](https://true-og.net/) Network designed to replace the proprietary ClearLag plugin and its extension SaveDeathDrops.

Current Target: Purpur 1.18.2

Authors: BadPylot, NotAlexNoyle.

To build:

`./gradlew build`

The resulting .jar file will be in build/libs/

*Version 2.1 Changelog:*

- Fixed NBT-API import in gradle.

- Normalized formatting and indentation.

- Fixed and enhanced despawn time settings.

*Version: 2.0 Changelog:*

- Adopted GPLv3.

- General refactor.

- Documented the code line-by-line.

- Migrated from maven to gradle.

- Updated NBT-API.

- Fixed class object passing.

- More efficient entity check.

*Features:*

- Prevent player death piles from despawning.

- Change the amount of time it takes for items to despawn.

- Set in-game warnings for when items are about to despawn. (Coming in 3.0)

- Clear items with an in-game command. (Coming in 3.0)

Uses [NBTAPI](https://github.com/tr7zw/Item-NBT-API).

**Licensed under the GPLv3.**
