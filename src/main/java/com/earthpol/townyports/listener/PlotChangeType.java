package com.earthpol.townyports.listener;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.*;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;

import org.bukkit.Chunk;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;

public class PlotChangeType implements Listener {

    // Oceans, rivers, and beaches are eligible to be port plots.
    private static final Set<Biome> eligibleBiomes = Set.of(
            Biome.OCEAN,
            Biome.WARM_OCEAN,
            Biome.LUKEWARM_OCEAN,
            Biome.COLD_OCEAN,
            Biome.FROZEN_OCEAN,
            Biome.DEEP_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN,
            Biome.DEEP_COLD_OCEAN,
            Biome.DEEP_FROZEN_OCEAN,
            Biome.RIVER,
            Biome.FROZEN_RIVER,
            Biome.BEACH,
            Biome.SNOWY_BEACH,
            Biome.STONY_SHORE
    );


	@EventHandler
    public void onPlotChangeType(PlotPreChangeTypeEvent event) {
        //Ignore non-port plot types
        if (!event.getNewType().getName().equals("port")) return;

        if(event.getResident().hasPermissionNode("townyports.bypass.biome") || event.getResident().getPlayer().isOp()) return;

        if (!isEligibleBiome(event.getTownBlock())){
            event.setCancelled(true);
            event.setCancelMessage("§cYou cannot set plots to port type outside of ocean biomes.");
        }
    }

	// Determines if a biome is eligible to have a port plot in it.
    private boolean isEligibleBiome(TownBlock townBlock) {
        Chunk potentialPortChunk = townBlock.getWorldCoord().getLowerMostCornerLocation().getChunk();

        for (Biome biome : eligibleBiomes){
            if(potentialPortChunk.contains(biome)){
                return true;
            }
        }
        return false;
	}
}


