package com.earthpol.townyports.utils;

import java.util.Optional;

import com.earthpol.townyports.PortsMain;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache.CacheType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

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

	public static void setPortSpawnLocation(Town town, Location loc) throws TownyException {
		String key = "portspawn." + town.getUUID().toString();
		FileConfiguration cfg = PortsMain.instance.getConfig();
		cfg.set(key + ".world", loc.getWorld().getName());
		cfg.set(key + ".x",     (Object) loc.getX());
		cfg.set(key + ".y",     (Object) loc.getY());
		cfg.set(key + ".z",     (Object) loc.getZ());
		cfg.set(key + ".yaw",   (Object) loc.getYaw());
		cfg.set(key + ".pitch", (Object) loc.getPitch());
		PortsMain.instance.saveConfig();
	}

	public static Location getPortSpawnLocation(Town town) throws TownyException {
		String key = "portspawn." + town.getUUID().toString();
		FileConfiguration cfg = PortsMain.instance.getConfig();
		if (!cfg.contains(key + ".world"))
			throw new TownyException("Port spawn not set for town " + town.getName());

		World world = Bukkit.getWorld(cfg.getString(key + ".world"));
		if (world == null)
			throw new TownyException("World '" + cfg.getString(key + ".world") + "' not found");

		double x     = cfg.getDouble(key + ".x");
		double y     = cfg.getDouble(key + ".y");
		double z     = cfg.getDouble(key + ".z");
		float  yaw   = (float) cfg.getDouble(key + ".yaw");
		float  pitch = (float) cfg.getDouble(key + ".pitch");
		return new Location(world, x, y, z, yaw, pitch);
	}
}
