package com.earthpol.townyports.commands;

import com.earthpol.earthPolLib.cooldown.PlayerCooldownManager;
import com.earthpol.earthPolLib.teleport.TeleportOutcomeHandler;
import com.earthpol.earthPolLib.teleport.Teleporter;
import com.earthpol.earthPolLib.teleport.VehicleTeleporter;
import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.data.Port;
import com.earthpol.townyports.data.PortDAO;
import com.earthpol.townyports.utils.PortPlotUtil;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.util.MathUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PortCommand extends BaseCommand implements CommandExecutor {
	private static final PlayerCooldownManager portsCooldownManager = new PlayerCooldownManager();

	public PortCommand() {}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
							 @NotNull String label, @NotNull String @NotNull [] args) {

		// Command parsing
		if(!(sender instanceof Player p)) { // Check if player
			PortsMain.instance.getLogger().info("You must run this command as a Player!");
			return true;
		}
		if (args.length == 0){ // Incorrect argument length
			p.sendMessage("§6[TownyPorts]§d Correct usage: `/port <destination-town>`.");
			return true;
		}

		try {checkTeleportEligibility(p, args);}
		catch (TownyException e) {
			p.sendMessage(e.getMessage());
			return true;
		}

		// Try to find the destination town
        Town destinationTown;
        try {destinationTown = getTownOrThrow(args[0]);}
		catch (TownyException e) {
			p.sendMessage(e.getMessage());
			return true;
		}

		// Check if a port is set up at the destination town.
        Port destinationPort = PortDAO.getPort(destinationTown);
		if (destinationPort == null) {
			p.sendMessage("This town doesn't have a port set up.");
			return true;
		}

		// ==== Teleporter configuration ======

		VehicleTeleporter portsVehicleTeleporter = new VehicleTeleporter(true,true);

		TeleportOutcomeHandler handler =  new TeleportOutcomeHandler();
		handler.setOnFailedUnaffordable(
				context -> {context.getPlayer().sendMessage("§6[TownyPorts]§c You cannot afford to travel to this port");}
		);

		Teleporter portsTeleporter = Teleporter.builder(PortsMain.instance)
				.setVehicleTeleporter(portsVehicleTeleporter)

				.preTeleportMessage(Component.text("§6[TownyPorts]§a Travelling to this port..."))
				.postTeleportMessage(Component.text("§6[TownyPorts]§a Arrived at the port."))

				.enableWarmup(PortsMain.getCustomConfig().getLong("port-travel-warmup-in-ticks"))
				.enableDestinationSafety()
				.disablePreTeleportMovement()
				.setOutcomeHandler(handler)

				.build();

		// Player cooldown verification
		if (portsCooldownManager.hasCooldown(p)){
			p.sendMessage("TownyPorts cooldown: " + portsCooldownManager.getCooldown(p) + " seconds");
			return true;
		}

		portsTeleporter.teleport(
				p,
				destinationPort.location(),
				destinationPort.portPrice()
		);
		portsCooldownManager.setCooldown(p, PortsMain.getCustomConfig().getLong("port-travel-cooldown-in-seconds"));

		return true;
	}

	/* Checks all the pre-conditions required before a commandSender can be considered eligible
	   for teleporting.
	   Throws an exception if any of the prerequisite conditions are not met.
	   This method should only be run from within a try catch block.
	 */
	private static void checkTeleportEligibility(Player player, String[] args) throws TownyException{

		TownyAPI townyAPI = TownyAPI.getInstance();
		Resident playerResident = townyAPI.getResident(player.getName());

		//Null check playerResident
		if(playerResident == null){
			throw new TownyException("§c Failed to get player Resident object. Please report this issue to a Developer.");
		}

        Town playerTown = playerResident.getTownOrNull();
		// Player has no town
		if (playerTown == null) {
			throw new TownyException("§c You do not belong to a town.");
		}

        Nation playerNation = playerTown.getNationOrNull();
		Town destinationTown = getTownOrThrow(args[0]);
		Nation destinationNation = destinationTown.getNationOrNull();

		// Player has no nation
		if (!playerTown.hasNation()){
			throw new TownyException("§c You do not belong to a nation.");
		}

		// Player is in the wilderness
		if(!player.hasPermission("townyports.bypass.wilderness")){
			if(townyAPI.isWilderness(player.getLocation())){
				throw new TownyException("§c You cannot teleport to a port from the wilderness.");
			}
		}

		// Check if player is standing in a port plot

		// Destination town does not belong to a nation
		if(!destinationTown.hasNation()){
			throw new TownyException("§c The destination town does not have a nation.");
		}

		// Destination town is part of an enemy nation
        assert destinationNation != null;
        if( destinationNation.hasEnemy(playerNation) && PortsMain.getCustomConfig().getBoolean("port-travel-denies-for-enemies")){
			throw new TownyException("§c You cannot teleport to an enemy nation's ports.");
		}

		// Destination town does not have a port plot
		if (!PortPlotUtil.hasPortPlot(destinationTown)){
			throw new TownyException("§c That town does not have a port.");
		}

		// The port is too far away to travel to.
		int portMaxDistance = PortsMain.getCustomConfig().getInt("maximum-port-distance-in-chunks");
		WorldCoord wc = PortPlotUtil.getPortPlot(destinationTown).getWorldCoord();

		Location loc = player.getLocation();
		int originChunkX = loc.getBlockX() >> 4;
		int originChunkZ = loc.getBlockZ() >> 4;
		WorldCoord originWc = new WorldCoord(loc.getWorld().getName(), originChunkX, originChunkZ);

		if (MathUtil.distance(originWc, wc) > portMaxDistance && !player.hasPermission("townyports.bypass.distance")){
			throw new TownyException("§c The port is too far away.");
		}

	}

}