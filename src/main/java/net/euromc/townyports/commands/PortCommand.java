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
			parsePortCommand(sender, args);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}
		return true;
	}

	private void parsePortCommand(@NotNull CommandSender sender, @NotNull String[] args) throws TownyException {

		if (!(sender instanceof Player)) {
			PortsMain.instance.getLogger().info("You must run this command as a Player!");
			return;
		}

		Player p = (Player) sender;

		if (args.length == 0)
			throw new TownyException("§6[TownyPorts]§d Correct usage: `/port <destination-town>`.");

		if (!TownyAPI.getInstance().getResident(p.getName()).hasTown())
			throw new TownyException("§c You do not belong to a town.");

		Town t = TownyAPI.getInstance().getResident(p.getName()).getTownOrNull();
		if (!t.hasNation())
			throw new TownyException("§c You do not belong to a nation.");

		if (TownyAPI.getInstance().isWilderness(p.getLocation()))
			throw new TownyException("§c You cannot teleport to a port from the wilderness.");

		if (!PortPlotUtil.isPortPlot(TownyAPI.getInstance().getTownBlock(p)))
			throw new TownyException("§c You can only go to another port starting from a port plot.");

		Town destinationTown = getTownOrThrow(args[0]);
		if (!destinationTown.hasNation())
			throw new TownyException("§c The destination town does not have a nation.");

		if (destinationTown.getNationOrNull().hasEnemy(t.getNationOrNull())
				&& PortsMain.getCustomConfig().getBoolean("port-travel-denies-for-enemies"))
			throw new TownyException("§c You cannot teleport to an enemy nation's ports.");

		if (!PortPlotUtil.hasPortPlot(destinationTown))
			throw new TownyException("§c That town does not have a port.");

		TownBlock tb = PortPlotUtil.getPortPlot(destinationTown);
		WorldCoord wc = tb.getWorldCoord();
		if (MathUtil.distance(TownyAPI.getInstance().getTownBlock(p.getLocation()).getWorldCoord(), wc) > 2750)
			throw new TownyException("§c The port is too far away.");

		Location destinationLoc = getDestinationSpawnLocation(wc);

		if (!LocationUtil.isSafe(destinationLoc))
			throw new TownyException("§c The destination port's location is not safe.");

		final TownBlock loc = TownyAPI.getInstance().getTownBlock(p.getLocation());
		p.sendMessage("§6[TownyPorts]§a Travelling to this port...");

		boolean costsMoney = PortsMain.getCustomConfig().getBoolean("uses-economy");
		if (costsMoney) {
			p.sendMessage( PortsMain.PREFIX + "§aThis will cost "
					+ PortsMain.instance.getConfig().getString(destinationTown.getUUID().toString())
					+ PortsMain.getCustomConfig().getString("currency-sign") + "...");
		}
		Confirmation.runOnAccept(() -> {
			int cdSec = PortsMain.getCustomConfig().getInt("port-travel-cooldown-in-seconds");
			if (!cooldown.containsKey(p.getUniqueId()) || System.currentTimeMillis() - cooldown.get(p.getUniqueId()) > cdSec*1000) {
				cooldown.put(p.getUniqueId(), System.currentTimeMillis());
			} else {
				long calc = (System.currentTimeMillis() - cooldown.get(p.getUniqueId()))/1000;
				p.sendMessage(PortsMain.PREFIX + "§cYou need to wait another " + Math.round(cdSec - calc) + " seconds to use this command again.");
				return;
			}
			int warmup = PortsMain.getCustomConfig().getInt("port-travel-warmup-in-ticks");
			int secTime = Math.round(warmup/20);
			p.sendMessage("§6[TownyPorts]§a You accepted the costs of this trip. You will depart in §b" + secTime + " seconds§a.");

			Bukkit.getScheduler().runTaskLater(PortsMain.instance, new Runnable() {
				@Override
				public void run() {
					if (!p.isOnline()) {
						Bukkit.getLogger().info("§f[§4ALERT§f] §e" + p.getName() + " has tried to teleport to " + destinationTown.getName() + "'s port while being offline.");
						return;
					}

					double costDouble = Double.parseDouble(PortsMain.instance.getConfig().getString(destinationTown.getUUID().toString()));
					boolean usesEco = PortsMain.getCustomConfig().getBoolean("uses-economy");
					if (usesEco && costsMoney && !TownyAPI.getInstance().getResident(p.getName()).getAccount().payTo(costDouble, destinationTown.getAccount(), "Travelled to Port.")) {
						p.sendMessage("§6[TownyPorts]§c You cannot afford to travel to this port");
						return;
					}

					if (loc != TownyAPI.getInstance().getTownBlock(p.getLocation())) {
						p.sendMessage("§6[TownyPorts]§c You have moved away from the port while waiting, teleportation denied.");
						return;
					}
					p.teleport(destinationLoc);
					p.sendMessage("§6[TownyPorts]§a Arrived at the port.");
				}
			}, warmup);
		})
		.runOnCancel(() -> p.sendMessage("§6[TownyPorts]§c Your trip has been canceled."))
		.sendTo(p.getPlayer());

	}

	private Location getDestinationSpawnLocation(WorldCoord wc) {
		World world = Bukkit.getWorld(wc.getWorldName());
		int X = wc.getX() * 16 + 8;
		int Z = wc.getZ() * 16 + 8;
		int safeY = world.getHighestBlockAt(X, Z).getY();
		return world.getBlockAt(X, safeY + 1, Z).getLocation();
	}
}