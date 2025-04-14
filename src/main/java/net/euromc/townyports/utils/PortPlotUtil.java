package net.euromc.townyports.utils;

import java.util.Optional;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache.CacheType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;

public final class PortPlotUtil {

	public static boolean isPortPlot(TownBlock tb) {
		return isPortPlot(tb.getType());
	}

	public static boolean isPortPlot(TownBlockType type) {
		return type.getName().equalsIgnoreCase("port");
	}

	public static boolean hasPortPlot(Town town) {
		return town.getTownBlockTypeCache().getNumTownBlocks(TownBlockTypeHandler.getType("port"), CacheType.ALL) > 0;
	}

	public static TownBlock getPortPlot(Town town) {
		Optional<TownBlock> portPlot = town.getTownBlocks().stream().filter(tb -> PortPlotUtil.isPortPlot(tb)).findFirst();
		return portPlot.get();
	}
}
