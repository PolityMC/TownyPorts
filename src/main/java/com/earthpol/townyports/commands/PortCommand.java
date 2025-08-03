package com.earthpol.townyports.commands;

import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.utils.LocationUtil;
import com.earthpol.townyports.utils.PortPlotUtil;

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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PortCommand extends BaseCommand implements CommandExecutor {
	private final HashMap<UUID, Long> cooldown;

	public PortCommand() {
		this.cooldown = new HashMap<>();
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
							 @NotNull String label, @NotNull String @NotNull [] args) {
		try {
			checkTeleportEligibility(sender, args);

			Player p = (Player) sender;
			Town destinationTown = getTownOrThrow(args[0]);
			WorldCoord wc = PortPlotUtil.getPortPlot(destinationTown).getWorldCoord();
			World world = Objects.requireNonNull(Bukkit.getWorld(wc.getWorldName()));

			// schedule all logic on the region thread for destination chunk
			Bukkit.getRegionScheduler().execute(
					PortsMain.instance,
					world,
					wc.getX(),
					wc.getZ(),
					() -> {
						try {
							parsePortCommand(sender, args);
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
						}
					}
			);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}
		return true;
	}

	private void parsePortCommand(@NotNull CommandSender sender, @NotNull String[] args) throws TownyException {
		Player p = (Player) sender;
		Town destinationTown = getTownOrThrow(args[0]);
		WorldCoord wc = PortPlotUtil.getPortPlot(destinationTown).getWorldCoord();
		Location destinationLoc = PortPlotUtil.getPortSpawnLocation(destinationTown);

		if (!LocationUtil.isSafe(destinationLoc))
			throw new TownyException("§c The destination port's location is not safe.");

		final TownBlock loc = TownyAPI.getInstance().getTownBlock(p.getLocation());
		p.sendMessage("§6[TownyPorts]§a Travelling to this port...");

		boolean costsMoney = PortsMain.getCustomConfig().getBoolean("uses-economy");
		double cost;
		if (costsMoney) {
			cost = Double.parseDouble(Objects.requireNonNull(PortsMain.instance.getConfig()
                    .getString(destinationTown.getUUID().toString())));
			p.sendMessage(PortsMain.PREFIX + "§aThis will cost " + cost
					+ PortsMain.getCustomConfig().getString("currency-sign") + "...");
			// pre-check balance
			double balance = Objects.requireNonNull(TownyAPI.getInstance()
                            .getResident(p.getName()))
					.getAccount()
					.getHoldingBalance();
			if (balance < cost) {
				p.sendMessage("§6[TownyPorts]§c You cannot afford to travel to this port");
				return;
			}
		} else {
            cost = 0;
        }

        Confirmation.runOnAccept(() -> {
					int cdSec = PortsMain.getCustomConfig().getInt("port-travel-cooldown-in-seconds");
					if (!cooldown.containsKey(p.getUniqueId())
							|| System.currentTimeMillis() - cooldown.get(p.getUniqueId()) > cdSec * 1000L
					) {
						cooldown.put(p.getUniqueId(), Long.valueOf(System.currentTimeMillis()));
					} else {
						long calc = (System.currentTimeMillis() - cooldown.get(p.getUniqueId())) / 1000;
						p.sendMessage(PortsMain.PREFIX + "§cYou need to wait another "
								+ Math.round(cdSec - calc) + " seconds to use this command again.");
						return;
					}

					int warmupTicks = PortsMain.getCustomConfig().getInt("port-travel-warmup-in-ticks");
					int secTime = Math.round(warmupTicks / 20f);
					p.sendMessage("§6[TownyPorts]§a You accepted the costs of this trip. You will depart in §b"
							+ secTime + " seconds§a.");
					// send title countdown
					p.sendTitle("Teleporting in " + secTime + "s", "", 10, secTime * 20, 10);

					Bukkit.getAsyncScheduler().runDelayed(
							PortsMain.instance,
							task -> Bukkit.getRegionScheduler().execute(
									PortsMain.instance,
									destinationLoc.getWorld(),
									wc.getX(),
									wc.getZ(),
									() -> {
										if (!p.isOnline()) {
											Bukkit.getLogger().info("§f[§4ALERT§f] §e" + p.getName()
													+ " tried to teleport while offline.");
											return;
										}

										// final balance check + withdrawal
										if (costsMoney) {
											boolean success = Objects.requireNonNull(TownyAPI.getInstance()
                                                            .getResident(p.getName()))
													.getAccount()
													.payTo(cost, destinationTown.getAccount(), "Travelled to Port.");
											if (!success) {
												p.sendMessage("§6[TownyPorts]§c You cannot afford to travel to this port");
												return;
											}
										}

										if (loc != TownyAPI.getInstance().getTownBlock(p.getLocation())) {
											p.sendMessage("§6[TownyPorts]§c You moved away; teleport cancelled.");
											return;
										}

										p.teleportAsync(destinationLoc);
										p.sendMessage("§6[TownyPorts]§a Arrived at the port.");
									}
							),
							secTime,
							TimeUnit.SECONDS
					);

				})
				.runOnCancel(() -> p.sendMessage("§6[TownyPorts]§c Your trip has been canceled."))
				.sendTo(p);
	}

	/* Checks all the pre-conditions required before a commandSender can be considered eligible
	   for teleporting.
	   Throws an exception if any of the prerequisite conditions are not met.
	   This method should only be run from within a try catch block.
	 */
	private static void checkTeleportEligibility(CommandSender sender, String[] args) throws TownyException{

		// Command sender isnt a player
		if (!(sender instanceof Player player)) {
			PortsMain.instance.getLogger().info("You must run this command as a Player!");
			throw new TownyException("[TownyPorts] /port command must be run as a player.");
		}

		// Incorrect argument length
		if (args.length == 0){
			throw new TownyException("§6[TownyPorts]§d Correct usage: `/port <destination-town>`.");
		}

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
		if(townyAPI.isWilderness(player.getLocation())){
			throw new TownyException("§c You cannot teleport to a port from the wilderness.");
		}

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
		if (MathUtil.distance(Objects.requireNonNull(TownyAPI.getInstance().getTownBlock(player.getLocation())).getWorldCoord(), wc) > portMaxDistance){
			throw new TownyException("§c The port is too far away.");
		}

	}

}