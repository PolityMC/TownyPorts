// com.earthpol.townyports.cache.PortRegistry.java
package com.earthpol.townyports.cache;

import com.earthpol.townyports.PortsMain;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.*;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class PortRegistry {

    private final PortsMain plugin;
    // townUUID -> immutable list of port entries
    private volatile Map<UUID, List<PortEntry>> byTown = new ConcurrentHashMap<>();
    private volatile long lastBuildMs = 0L;

    public PortRegistry(PortsMain plugin) {
        this.plugin = plugin;
    }

    public long lastBuildEpochMs() {
        return lastBuildMs;
    }

    /** Call once onEnable and on /townyports reload; runs async + then swaps in snapshot on main thread */
    public void rebuildAsync() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            Map<UUID, List<PortEntry>> snapshot = buildSnapshot();
            // swap on main thread to keep readers safe from races on Bukkit objects in future usages
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                byTown = snapshot;
                lastBuildMs = System.currentTimeMillis();
                plugin.getLogger().info("[TownyPorts] PortRegistry rebuilt: " + totalPorts() + " active ports across " + byTown.size() + " towns.");
            });
        });
    }

    /** Rebuild only for one town (fast path when we know what changed). */
    public void rebuildTown(UUID townUUID) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            Map<UUID, List<PortEntry>> copy = new ConcurrentHashMap<>(byTown);
            copy.put(townUUID, scanTown(townUUID));
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                byTown = copy;
                lastBuildMs = System.currentTimeMillis();
            });
        });
    }

    /** Remove a town entirely (e.g., deleted/merged away). */
    public void removeTown(UUID townUUID) {
        Map<UUID, List<PortEntry>> copy = new ConcurrentHashMap<>(byTown);
        copy.remove(townUUID);
        byTown = copy;
        lastBuildMs = System.currentTimeMillis();
    }

    public int totalPorts() {
        return byTown.values().stream().mapToInt(List::size).sum();
    }

    public Set<String> activeTownNames() {
        return byTown.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> resolveTownName(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public List<PortEntry> portsForTownName(String townName) {
        Town town = TownyAPI.getInstance().getTown(townName);
        if (town == null) return List.of();
        return byTown.getOrDefault(town.getUUID(), List.of());
    }

    public List<PortEntry> allPortsFlattened() {
        return byTown.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    // ----------------- internals -----------------

    private Map<UUID, List<PortEntry>> buildSnapshot() {
        Map<UUID, List<PortEntry>> result = new ConcurrentHashMap<>();
        for (Town town : TownyAPI.getInstance().getTowns()) {
            result.put(town.getUUID(), scanTown(town.getUUID()));
        }
        return result;
    }

    private List<PortEntry> scanTown(UUID townUUID) {
        Town town = TownyAPI.getInstance().getTown(townUUID);
        if (town == null) return List.of();

        TownBlockType portType = TownBlockTypeHandler.getType("port");
        if (portType == null) return List.of();

        double price = plugin.getConfig().getDouble(townUUID.toString(), 0.0D);
        List<PortEntry> list = new ArrayList<>();

        for (TownBlock tb : town.getTownBlocks()) {
            if (tb.getType() != null && "port".equalsIgnoreCase(tb.getType().getName())) {
                WorldCoord wc = tb.getWorldCoord();
                list.add(new PortEntry(
                        townUUID,
                        town.getName(),
                        wc.getWorldName(),
                        wc.getX(),
                        wc.getZ(),
                        price
                ));
            }
        }
        // return immutable copy
        return List.copyOf(list);
    }

    private String resolveTownName(UUID uuid) {
        Town t = TownyAPI.getInstance().getTown(uuid);
        return t != null ? t.getName() : null;
    }
}
