package net.euromc.townyports.commands;

import net.euromc.townyports.PortsMain;
import net.euromc.townyports.utils.LocationUtil;
import net.euromc.townyports.utils.PortPlotUtil;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class PortCommand extends BaseCommand implements CommandExecutor {

	private final HashMap<UUID, Long> cooldown;
	public PortCommand() {
		this.cooldown = new HashMap<>();
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		try {
			checkTeleportEligibility(sender, args);
			parsePortCommand(sender, args);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}
		return true;
	}

	private void parsePortCommand(@NotNull CommandSender sender, @NotNull String[] args) throws TownyException {

		// Folia refactoring section below

		// Calculate port distance
		Player p = (Player) sender;
		Town destinationTown = getTownOrThrow(args[0]);
		WorldCoord wc = PortPlotUtil.getPortPlot(destinationTown).getWorldCoord();
		Location destinationLoc = getDestinationSpawnLocation(wc);

		if (!LocationUtil.isSafe(destinationLoc))
			throw new TownyException("§c The destination port's location is not safe.");

		final TownBlock loc = TownyAPI.getInstance().getTownBlock(p.getLocation());
		p.sendMessage("§6[TownyPorts]§a Travelling to this port...");

		// Give the player the option to accept the TP or not
		boolean costsMoney = PortsMain.getCustomConfig().getBoolean("uses-economy");
		if (costsMoney) {
			p.sendMessage( PortsMain.PREFIX + "§aThis will cost "
					+ PortsMain.instance.getConfig().getString(destinationTown.getUUID().toString())
					+ PortsMain.getCustomConfig().getString("currency-sign") + "...");
		}

		// If the player accepts the teleport, run the teleport task
		Confirmation.runOnAccept(() -> {

			// Retrieve the cooldown
			int cdSec = PortsMain.getCustomConfig().getInt("port-travel-cooldown-in-seconds");

			// Check if the player is under cooldown
			if (!cooldown.containsKey(p.getUniqueId()) || System.currentTimeMillis() - cooldown.get(p.getUniqueId()) > cdSec*1000) {
				cooldown.put(p.getUniqueId(), System.currentTimeMillis());
			} else {
				long calc = (System.currentTimeMillis() - cooldown.get(p.getUniqueId()))/1000;
				p.sendMessage(PortsMain.PREFIX + "§cYou need to wait another " + Math.round(cdSec - calc) + " seconds to use this command again.");
				return;
			}

			// Get the teleport warmup and notify player
			int warmup = PortsMain.getCustomConfig().getInt("port-travel-warmup-in-ticks");
			int secTime = Math.round(warmup/20);
			p.sendMessage("§6[TownyPorts]§a You accepted the costs of this trip. You will depart in §b" + secTime + " seconds§a.");

			// Teleport task via Bukkit Scheduler
			Bukkit.getScheduler().runTaskLater(PortsMain.instance, new Runnable() {


				@Override
				public void run() {

					// Verify the player trying to teleport is not offline.
					if (!p.isOnline()) {
						Bukkit.getLogger().info("§f[§4ALERT§f] §e" + p.getName() + " has tried to teleport to " + destinationTown.getName() + "'s port while being offline.");
						return;
					}

					//Verify the player can afford to complete the teleport
					double costDouble = Double.parseDouble(PortsMain.instance.getConfig().getString(destinationTown.getUUID().toString()));
					boolean usesEco = PortsMain.getCustomConfig().getBoolean("uses-economy");
					if (usesEco && costsMoney && !TownyAPI.getInstance().getResident(p.getName()).getAccount().payTo(costDouble, destinationTown.getAccount(), "Travelled to Port.")) {
						p.sendMessage("§6[TownyPorts]§c You cannot afford to travel to this port");
						return;
					}

					//Ensure the player is still standing in the port plot area before teleporting
					if (loc != TownyAPI.getInstance().getTownBlock(p.getLocation())) {
						p.sendMessage("§6[TownyPorts]§c You have moved away from the port while waiting, teleportation denied.");
						return;
					}

					//Complete the teleport
					p.teleport(destinationLoc);
					p.sendMessage("§6[TownyPorts]§a Arrived at the port.");
				}
			}, warmup); //Apply warmup period before running the task
		})
		.runOnCancel(() -> p.sendMessage("§6[TownyPorts]§c Your trip has been canceled.")) //If the task is canceled via return, send player message
		.sendTo(p.getPlayer());

	}

	// Needs to be run in a scheduler.
	private Location getDestinationSpawnLocation(WorldCoord wc) {
		World world = Bukkit.getWorld(wc.getWorldName());
		int X = wc.getX() * 16 + 8;
		int Z = wc.getZ() * 16 + 8;
		int safeY = world.getHighestBlockAt(X, Z).getY();
		return world.getBlockAt(X, safeY + 1, Z).getLocation(); // NEEDS TO BE RUN IN GlobalRegionScheduler
	}

	/* Checks all the pre-conditions required before a commandSender can be considered eligible
	   for teleporting.
	   Throws an exception if any of the prerequisite conditions are not met.
	   This method should only be run from within a try catch block.
	 */
	private static void checkTeleportEligibility(CommandSender sender, String[] args) throws TownyException{

		// Command sender isnt a player
		if (!(sender instanceof Player)) {
			PortsMain.instance.getLogger().info("You must run this command as a Player!");
			throw new TownyException("[TownyPorts] /port command must be run as a player.");
		}

		// Incorrect argument length
		if (args.length == 0 || args == null){
			throw new TownyException("§6[TownyPorts]§d Correct usage: `/port <destination-town>`.");
		}

		TownyAPI townyAPI = TownyAPI.getInstance();

		Player player = (Player) sender;
		Town playerTown = townyAPI.getResident(player.getName()).getTownOrNull();
		Nation playerNation = playerTown.getNationOrNull();

		Town destinationTown = getTownOrThrow(args[0]);
		Nation destinationNation = destinationTown.getNationOrNull();
		WorldCoord wc = PortPlotUtil.getPortPlot(destinationTown).getWorldCoord();

		// Player has no town
		if (!townyAPI.getResident(player.getName()).hasTown()){
			throw new TownyException("§c You do not belong to a town.");
		}

		// Player has no nation
		if (!playerTown.hasNation()){
			throw new TownyException("§c You do not belong to a nation.");
		}

		// Player is in the wilderness
		if(townyAPI.isWilderness(player.getLocation())){
			throw new TownyException("§c You cannot teleport to a port from the wilderness.");
		}

		// Destination town does not have a port plot
		if(!PortPlotUtil.hasPortPlot(destinationTown)){
			throw new TownyException("§c That town does not have a port.");
		}

		// Destination town does not belong to a nation
		if(!destinationTown.hasNation()){
			throw new TownyException("§c The destination town does not have a nation.");
		}

		// Destination town is part of an enemy nation
		if( destinationNation.hasEnemy(playerNation) && PortsMain.getCustomConfig().getBoolean("port-travel-denies-for-enemies")){
			throw new TownyException("§c You cannot teleport to an enemy nation's ports.");
		}

		// Destination town does not have a port plot
		if (!PortPlotUtil.hasPortPlot(destinationTown)){
			throw new TownyException("§c That town does not have a port.");
		}

		// The port is too far away to travel to.
		if (MathUtil.distance(TownyAPI.getInstance().getTownBlock(player.getLocation()).getWorldCoord(), wc) > 2750){
			throw new TownyException("§c The port is too far away.");
		}

	}
}