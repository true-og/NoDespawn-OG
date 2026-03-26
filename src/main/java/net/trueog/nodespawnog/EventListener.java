package net.trueog.nodespawnog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class EventListener implements Listener {

    private final FileConfiguration config = NoDespawnOG.getPlugin().getConfig();

    private final NamespacedKey keyDeath = new NamespacedKey(NoDespawnOG.getPlugin(), "nodespawn_death");
    private final NamespacedKey keyOwner = new NamespacedKey(NoDespawnOG.getPlugin(), "nodespawn_owner");
    private final NamespacedKey keyPile = new NamespacedKey(NoDespawnOG.getPlugin(), "nodespawn_pile");
    private final NamespacedKey keyCreated = new NamespacedKey(NoDespawnOG.getPlugin(), "nodespawn_created");
    private final NamespacedKey keyExpireAt = new NamespacedKey(NoDespawnOG.getPlugin(), "nodespawn_expire_at");

    private static final int VANILLA_DESPAWN_TICKS = 6000;
    private static final long TICK_MS = 50L;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event) {

        ensureExpireData(event.getEntity());

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(EntitySpawnEvent event) {

        if (!(event.getEntity() instanceof ExperienceOrb orb)) {

            return;

        }

        ensureExpireData(orb);

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDespawn(ItemDespawnEvent event) {

        final Item item = event.getEntity();
        final long expireAt = getExpireAt(item);
        if (expireAt == 0L) {

            return;

        }

        final long now = System.currentTimeMillis();

        if (expireAt == Long.MAX_VALUE) {

            event.setCancelled(true);

            item.setTicksLived(1);

            return;

        }

        // Allow despawning if its time.
        if (now >= expireAt) {

            return;

        }

        event.setCancelled(true);

        scheduleVanillaDespawnCheck(item, now, expireAt);

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityDeathEvent(EntityDeathEvent event) {

        if (!(event.getEntity() instanceof Player player)) {

            return;

        }

        final int droppedExp = event.getDroppedExp();
        final boolean hasDrops = !event.getDrops().isEmpty();
        final boolean hasExp = droppedExp > 0;
        if (!hasDrops && !hasExp) {

            return;

        }

        final int reservedEntities = event.getDrops().size() + (hasExp ? 1 : 0);
        enforcePlayerPerChunkLimits(player, player.getLocation().getChunk(), reservedEntities, 1, getMaxPiles(),
                getMaxItems());

        final boolean disableDeathDespawns = config.getBoolean("disable-death-despawns");
        final int deathLifetimeTicks = calculateDespawnTime("time-to-death-despawns");

        final String ownerStr = player.getUniqueId().toString();
        final String pileId = UUID.randomUUID().toString();
        final long created = System.currentTimeMillis();
        final long expireAt = disableDeathDespawns ? Long.MAX_VALUE : (created + ticksToMs(deathLifetimeTicks));

        event.getDrops().stream().map(itemStack -> player.getWorld().dropItem(player.getLocation(), itemStack))
                .forEach(droppedItem ->
                {

                    markDeathPileEntity(droppedItem, ownerStr, pileId, created, expireAt);

                    scheduleVanillaDespawnCheck(droppedItem, created, expireAt);

                });

        if (hasExp) {

            player.getWorld().spawn(player.getLocation(), ExperienceOrb.class, orb -> {

                orb.setExperience(droppedExp);
                markDeathPileEntity(orb, ownerStr, pileId, created, expireAt);
                scheduleVanillaDespawnCheck(orb, created, expireAt);

            });
            event.setDroppedExp(0);

        }

        event.getDrops().clear();

    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        handleEntitiesInChunk(event.getChunk());

    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {

        for (Chunk chunk : event.getWorld().getLoadedChunks()) {

            handleEntitiesInChunk(chunk);

        }

    }

    private static long ticksToMs(long ticks) {

        if (ticks <= 0) {

            return 0L;

        }

        final long ms = ticks * TICK_MS;
        return ms < 0 ? Long.MAX_VALUE : ms;

    }

    private boolean protectionEnabled() {

        return config.getBoolean("death-pile-protection.enabled", true);

    }

    private void markDeathPileEntity(Entity entity, String ownerStr, String pileId, long created, long expireAt) {

        final PersistentDataContainer pdc = entity.getPersistentDataContainer();

        pdc.set(keyDeath, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyOwner, PersistentDataType.STRING, ownerStr);
        pdc.set(keyPile, PersistentDataType.STRING, pileId);
        pdc.set(keyCreated, PersistentDataType.LONG, created);
        pdc.set(keyExpireAt, PersistentDataType.LONG, expireAt);

    }

    private boolean isDeathPileEntity(Entity entity) {

        final Byte flag = entity.getPersistentDataContainer().get(keyDeath, PersistentDataType.BYTE);

        return Byte.valueOf((byte) 1).equals(flag);

    }

    private boolean isDeathPileItem(Item item) {

        return isDeathPileEntity(item);

    }

    private boolean isDeathPileOrb(ExperienceOrb orb) {

        return isDeathPileEntity(orb);

    }

    private long getExpireAt(Entity entity) {

        final Long v = entity.getPersistentDataContainer().get(keyExpireAt, PersistentDataType.LONG);

        return v == null ? 0L : v.longValue();

    }

    private void setExpireAt(Entity entity, long expireAt) {

        entity.getPersistentDataContainer().set(keyExpireAt, PersistentDataType.LONG, expireAt);

    }

    private long ensureCreatedMillis(Entity entity, long now) {

        final PersistentDataContainer pdc = entity.getPersistentDataContainer();
        final Long created = pdc.get(keyCreated, PersistentDataType.LONG);
        if (created != null && created.longValue() > 0L) {

            return created.longValue();

        }

        // Infer created time from current ticks lived (always >= 0 on Bukkit side).
        final int lived = Math.max(0, entity.getTicksLived());
        final long inferred = now - (long) lived * TICK_MS;

        pdc.set(keyCreated, PersistentDataType.LONG, inferred);

        return inferred;

    }

    /**
     * Set ticksLived safely (>= 1) so vanilla despawn checks happen when we want.
     */
    private void scheduleVanillaDespawnCheck(Entity entity, long now, long expireAt) {

        if (!entity.isValid()) {

            return;

        }

        if (expireAt == Long.MAX_VALUE) {

            entity.setTicksLived(1);

            return;

        }

        final long remainingMs = expireAt - now;
        if (remainingMs <= 0L) {

            entity.remove();

            return;

        }

        final long remainingTicks = (remainingMs + (TICK_MS - 1)) / TICK_MS;

        if (remainingTicks >= VANILLA_DESPAWN_TICKS) {

            // Too far away to represent (would require negative); schedule another check
            // 6000 ticks later.
            entity.setTicksLived(1);

            return;

        }

        int age = (int) (VANILLA_DESPAWN_TICKS - remainingTicks);

        age = Math.max(1, age);

        entity.setTicksLived(age);

    }

    /**
     * Ensure expireAt exists for any item we manage.
     */
    private void ensureExpireData(Item item) {

        ensureExpireData((Entity) item);

    }

    private void ensureExpireData(ExperienceOrb orb) {

        ensureExpireData((Entity) orb);

    }

    private void ensureExpireData(Entity entity) {

        final long now = System.currentTimeMillis();
        long expireAt = getExpireAt(entity);
        if (expireAt != 0L) {

            scheduleVanillaDespawnCheck(entity, now, expireAt);

            return;

        }

        final long created = ensureCreatedMillis(entity, now);
        final boolean disableDeathDespawns = config.getBoolean("disable-death-despawns");
        if (isDeathPileEntity(entity)) {

            if (disableDeathDespawns) {

                expireAt = Long.MAX_VALUE;
                setExpireAt(entity, expireAt);

                scheduleVanillaDespawnCheck(entity, now, expireAt);

                return;

            }

            final int deathLifetimeTicks = calculateDespawnTime("time-to-death-despawns");

            expireAt = created + ticksToMs(deathLifetimeTicks);
            setExpireAt(entity, expireAt);

            scheduleVanillaDespawnCheck(entity, now, expireAt);

            return;

        }

        if (entity instanceof ExperienceOrb) {

            return;

        }

        final int defaultLifetimeTicks = calculateDespawnTime("time-to-despawn");

        expireAt = created + ticksToMs(defaultLifetimeTicks);
        setExpireAt(entity, expireAt);

        scheduleVanillaDespawnCheck(entity, now, expireAt);

    }

    // Convert machine time to human time.
    private int calculateDespawnTime(String configKey) {

        final String raw = config.getString(configKey);
        if (raw == null) {

            return VANILLA_DESPAWN_TICKS;

        }

        final String timeString = StringUtils.trim(raw);
        if (StringUtils.isEmpty(timeString)) {

            return VANILLA_DESPAWN_TICKS;

        }

        try {

            final long time;

            // Strips digits and whitespace to leave only unit characters.
            final String unit = timeString.replaceAll("[\\d\\s]", "");

            // Strips digits and whitespace to leave only digit characters.
            final String digits = timeString.replaceAll("[^0-9]", "");

            if (StringUtils.isEmpty(digits)) {

                return VANILLA_DESPAWN_TICKS;

            }

            final int value = Integer.parseInt(digits);

            time = switch (StringUtils.lowerCase(unit)) {

                case "s" -> value * 20L;
                case "m" -> value * 20L * 60L;
                case "h" -> value * 20L * 60L * 60L;
                default -> value;

            };

            if (time <= 0) {

                return VANILLA_DESPAWN_TICKS;

            }

            if (time > Integer.MAX_VALUE) {

                return Integer.MAX_VALUE;

            }

            return (int) time;

        } catch (NumberFormatException error) {

            return VANILLA_DESPAWN_TICKS;

        }

    }

    private boolean isProtectedDeathPileItem(Item item) {

        return isDeathPileItem(item) && getExpireAt(item) == Long.MAX_VALUE;

    }

    private boolean isProtectedDeathPileOrb(ExperienceOrb orb) {

        return isDeathPileOrb(orb) && getExpireAt(orb) == Long.MAX_VALUE;

    }

    private String getOwnerString(Entity entity) {

        return entity.getPersistentDataContainer().get(keyOwner, PersistentDataType.STRING);

    }

    private String getPileId(Entity entity) {

        final String pile = entity.getPersistentDataContainer().get(keyPile, PersistentDataType.STRING);

        return (pile == null) ? entity.getUniqueId().toString() : pile;

    }

    private long getCreated(Entity entity) {

        final Long created = entity.getPersistentDataContainer().get(keyCreated, PersistentDataType.LONG);

        return created == null ? 0L : created.longValue();

    }

    private void timeoutPile(PileGroup group, int overflowLifetimeTicks) {

        if (group == null) {

            return;

        }

        final long now = System.currentTimeMillis();
        final long expireAt = now + ticksToMs(overflowLifetimeTicks);
        for (Iterator<Entity> it = group.entities.iterator(); it.hasNext();) {

            final Entity entity = it.next();
            if (!entity.isValid()) {

                it.remove();

                continue;

            }

            setExpireAt(entity, expireAt);
            scheduleVanillaDespawnCheck(entity, now, expireAt);

        }

        group.protectedPile = false;
        group.protectedItemCount = 0;

    }

    private void enforcePlayerPerChunkLimits(Player player, Chunk chunk, int reserveItems, int reservePiles,
            int maxPiles, int maxItems)
    {

        if (!protectionEnabled()) {

            return;

        }

        if (!config.getBoolean("disable-death-despawns")) {

            return;

        }

        if (maxPiles == 0 && maxItems == 0) {

            return;

        }

        final int overflowLifetimeTicks = calculateDespawnTime("death-pile-protection.overflow-despawn");
        final String owner = player.getUniqueId().toString();

        final List<Entity> ownedDeathEntities = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {

            if (!(entity instanceof Item || entity instanceof ExperienceOrb)) {

                continue;

            }

            if (!isDeathPileEntity(entity)) {

                continue;

            }

            final String o = getOwnerString(entity);
            if (o != null && o.equals(owner)) {

                ownedDeathEntities.add(entity);

            }

        }

        enforceOnOwnerChunkItems(ownedDeathEntities, maxPiles, maxItems, reservePiles, reserveItems,
                overflowLifetimeTicks);

    }

    private void enforceOnOwnerChunkItems(List<Entity> items, int maxPiles, int maxItems, int reservePiles,
            int reserveItems, int overflowLifetimeTicks)
    {

        if (items.isEmpty()) {

            return;

        }

        final int targetPiles = maxPiles > 0 ? Math.max(0, maxPiles - reservePiles) : Integer.MAX_VALUE;
        final int targetItems = maxItems > 0 ? Math.max(0, maxItems - reserveItems) : Integer.MAX_VALUE;

        final Map<String, PileGroup> groups = new HashMap<>();

        int protectedPiles = 0;
        int protectedItems = 0;

        for (Entity entity : items) {

            if (!entity.isValid()) {

                continue;

            }

            // Never sets ticksLived < 1 or else bukkit freaks out.
            ensureExpireData(entity);

            final String pileId = getPileId(entity);
            PileGroup group = groups.get(pileId);
            if (group == null) {

                group = new PileGroup();
                group.created = getCreated(entity);
                groups.put(pileId, group);

            }

            group.entities.add(entity);
            group.created = Math.min(group.created, getCreated(entity));

            if ((entity instanceof Item item && isProtectedDeathPileItem(item))
                    || (entity instanceof ExperienceOrb orb && isProtectedDeathPileOrb(orb)))
            {

                group.protectedItemCount++;
                group.protectedPile = true;

            }

        }

        for (PileGroup group : groups.values()) {

            if (group.protectedPile) {

                protectedPiles++;
                protectedItems += group.protectedItemCount;

            }

        }

        if (protectedPiles <= targetPiles && protectedItems <= targetItems) {

            return;

        }

        final List<PileGroup> ordered = new ArrayList<>(groups.values());
        ordered.sort(Comparator.comparingLong(g -> g.created));

        int i = 0;
        while ((protectedPiles > targetPiles || protectedItems > targetItems) && i < ordered.size()) {

            final PileGroup g = ordered.get(i++);
            if (!g.protectedPile) {

                continue;

            }

            protectedPiles--;
            protectedItems -= g.protectedItemCount;

            timeoutPile(g, overflowLifetimeTicks);

        }

    }

    private static final class PileGroup {

        private long created;
        private boolean protectedPile;
        private int protectedItemCount;
        private final List<Entity> entities = new ArrayList<>();

        private PileGroup() {

            this.created = Long.MAX_VALUE;

        }

    }

    private void handleEntitiesInChunk(Chunk chunk) {

        final int maxPiles = getMaxPiles();
        final int maxItems = getMaxItems();
        final int overflowLifetimeTicks = calculateDespawnTime("death-pile-protection.overflow-despawn");

        final Map<String, List<Entity>> ownedDeathItems = new HashMap<>();

        for (Entity entity : chunk.getEntities()) {

            if (entity instanceof Item item) {

                // Never sets ticksLived < 1 or else bukkit freaks out.
                ensureExpireData(item);

            } else if (entity instanceof ExperienceOrb orb) {

                // Never sets ticksLived < 1 or else bukkit freaks out.
                ensureExpireData(orb);

            } else {

                continue;

            }

            final boolean condition = protectionEnabled() && config.getBoolean("disable-death-despawns")
                    && (maxPiles > 0 || maxItems > 0) && isDeathPileEntity(entity);

            if (condition) {

                final String owner = getOwnerString(entity);
                if (owner != null) {

                    ownedDeathItems.computeIfAbsent(owner, k -> new ArrayList<>()).add(entity);

                }

            }

        }

        if (protectionEnabled() && config.getBoolean("disable-death-despawns") && (maxPiles > 0 || maxItems > 0)) {

            ownedDeathItems.values()
                    .forEach(list -> enforceOnOwnerChunkItems(list, maxPiles, maxItems, 0, 0, overflowLifetimeTicks));

        }

    }

    final int getMaxPiles() {

        return config.getInt("death-pile-protection.max-piles-per-player-per-chunk", 12);

    }

    final int getMaxItems() {

        return config.getInt("death-pile-protection.max-items-per-player-per-chunk", 1000);

    }

    public void scanLoadedChunks() {

        Bukkit.getWorlds().forEach(world -> {

            for (Chunk chunk : world.getLoadedChunks()) {

                handleEntitiesInChunk(chunk);

            }

        });

    }

}
