package net.euromc.townyports.listener;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.*;
import com.palmergames.bukkit.towny.object.WorldCoord;

import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlotChangeType implements Listener {

    private static final int halfPlotSize = TownySettings.getTownBlockSize() / 2;

	@EventHandler
    public void onPlotChangeType(PlotPreChangeTypeEvent event) {
        if (!event.getNewType().getName().equals("port"))
            return;
        if (isInOceanBiome(event.getTownBlock().getWorldCoord()))
            return;
        event.setCancelled(true);
        event.setCancelMessage("§cYou cannot set plots to port type outside of ocean biomes.");
    }

	private boolean isInOceanBiome(WorldCoord wc) {
		return getBiome(wc).name().contains("OCEAN");
	}
    
    private Biome getBiome(WorldCoord wc) {
        return wc.getBukkitWorld().getBiome(wc.getLowerMostCornerLocation().add(halfPlotSize, 61, halfPlotSize));
    }
}


