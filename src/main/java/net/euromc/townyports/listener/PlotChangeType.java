package net.euromc.townyports.listener;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.*;
import com.palmergames.bukkit.towny.object.WorldCoord;

import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;

public class PlotChangeType implements Listener {

    private static final int halfPlotSize = TownySettings.getTownBlockSize() / 2;

	@EventHandler
    public void onPlotChangeType(PlotPreChangeTypeEvent event) {
        if (!event.getNewType().getName().equals("port"))
            return;
        if (isEligibleBiome(event.getTownBlock().getWorldCoord()))
            return;
        event.setCancelled(true);
        event.setCancelMessage("§cYou cannot set plots to port type outside of ocean biomes.");
    }

	// Determines if a biome is eligible to have a port plot in it.
    private boolean isEligibleBiome(WorldCoord wc) {

        // Oceans, rivers, and beaches are eligible to be port plots.
        final Set<String> eligibleBiomes = Set.of(
                "ocean",
                "river",
                "beach"
        );

        /* name() is deprecated as of 1.21, will be removed in 1.22.
        * getBiome(wc) returns an OldEnum biome, which is also deprecated. This will need to be refactored
        * in the future.
        * */
        @Deprecated
        String biomeName = getBiome(wc).name().toLowerCase();

        //Compare each entry of eligibleBiomes to the provided biomeName
        for (String biome : eligibleBiomes) {
            if (biomeName.contains(biome))  {return true;}
        }
        return false;

	}

    private Biome getBiome(WorldCoord wc) {
        return wc.getBukkitWorld().getBiome(wc.getLowerMostCornerLocation().add(halfPlotSize, 61, halfPlotSize));
    }

}


