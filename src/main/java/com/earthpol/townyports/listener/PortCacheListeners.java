package com.earthpol.townyports.listener;

import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.cache.PortRegistry;
import com.palmergames.bukkit.towny.event.*;
import com.palmergames.bukkit.towny.event.plot.changeowner.PlotClaimEvent;
import com.palmergames.bukkit.towny.event.plot.changeowner.PlotUnclaimEvent;
import com.palmergames.bukkit.towny.event.town.TownMergeEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class PortCacheListeners implements Listener {

    private final PortsMain plugin;
    private final PortRegistry registry;

    public PortCacheListeners(PortsMain plugin, PortRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlotChangeType(PlotChangeTypeEvent event) {
        Town town = event.getTownBlock().getTownOrNull();
        if (town == null)
            return;
        
        TownBlockType oldType = event.getOldType();
        TownBlockType newType = event.getNewType();
        boolean involvesPort =
                (oldType != null && "port".equalsIgnoreCase(oldType.getName())) ||
                        (newType != null && "port".equalsIgnoreCase(newType.getName()));

        if (involvesPort) {
            registry.rebuildTown(town.getUUID());
        }
    }

    @EventHandler
    public void onPlotClaim(PlotClaimEvent event) {
        Town town = event.getTownBlock().getTownOrNull();
        if (town != null)
            registry.rebuildTown(town.getUUID());
    }

    @EventHandler
    public void onPlotUnclaim(PlotUnclaimEvent event) {
        Town town = event.getTownBlock().getTownOrNull();
        if (town != null)
            registry.rebuildTown(town.getUUID());
    }

    @EventHandler
    public void onTownMerge(TownMergeEvent event) {
        // succumbing: likely loses special plots
        UUID succ = event.getSuccumbingTownUUID();
        UUID dom  = event.getRemainingTown().getUUID();
        registry.rebuildTown(dom);
        registry.removeTown(succ);
    }

    @EventHandler
    public void onTownRename(RenameTownEvent event) {
        registry.rebuildTown(event.getTown().getUUID());
    }

    @EventHandler
    public void onTownDelete(DeleteTownEvent event) {
        registry.removeTown(event.getTownUUID());
    }
}
