package com.earthpol.townyports.commands;

import com.earthpol.earthPolLib.cooldown.PlayerCooldownManager;
import com.earthpol.earthPolLib.entity.vehicle.VehicleUtil;
import com.earthpol.earthPolLib.teleport.TeleportOutcomeHandler;
import com.earthpol.earthPolLib.teleport.Teleporter;
import com.earthpol.earthPolLib.teleport.VehicleTeleporter;
import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.config.Config;
import com.earthpol.townyports.data.Port;
import com.earthpol.townyports.data.PortDAO;
import com.earthpol.townyports.registry.VehicleRegistry;
import com.earthpol.townyports.util.Msg;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.MathUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.jetbrains.annotations.NotNull;

public class PortCommand extends BaseCommand implements CommandExecutor {

	private static final PlayerCooldownManager portsCooldownManager = new PlayerCooldownManager();

	public PortCommand() {}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
							 @NotNull String label, @NotNull String @NotNull [] args) {

		if(!(sender instanceof Player p)) {
			Msg.error(sender, "This command can only be used in-game.");
			return true;
		}

		if (args.length == 0) {
			Msg.usage(p, Component.text("/t port <destination-town>", Msg.ACCENT));
			return true;
		}

		// Destination town
		final Town destinationTown;
		try {
			destinationTown = getTownOrThrow(args[0]);
		} catch (TownyException e) {
			Msg.error(p, e.getMessage());
			return true;
		}

		final Port destinationPort = PortDAO.getPort(destinationTown);
		if (destinationPort == null) {
			Msg.error(p, "That town does not have a port.");
			return true;
		}

		try {
			checkTeleportEligibility(p, destinationTown, destinationPort);
		} catch (TownyException e) {

			String msg = e.getMessage();
			Msg.error(p, msg != null ? msg : "You cannot travel right now.");

			// If this is the "must be standing in port chunk" error, add guidance + copyable coords.
			if (msg != null && msg.startsWith("You must be standing in the same chunk as a port spawn")) {

				// 1) quick helper command
				Msg.send(p, Component.text()
						.append(Msg.PREFIX)
						.append(Component.text("Tip: ", Msg.MUTED))
						.append(Msg.runCmd("Click to run /t port here", "/t port here",
								"Check whether your current chunk is a port chunk"))
						.build()
				);

				// 2) If their town has a port, show it as click-to-copy coords
				Resident res = TownyAPI.getInstance().getResident(p);
				Town town = (res != null) ? res.getTownOrNull() : null;

				if (town != null) {
					Port originPort = PortDAO.getPort(town);
					if (originPort != null) {
						var l = originPort.location();
						String coordText = l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();

						Component coords = Component.text(coordText, NamedTextColor.WHITE)
								.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
										Component.text("Click to copy (suggest) coords", NamedTextColor.WHITE)))
								.clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(coordText));

						Msg.send(p, Component.text()
								.append(Msg.PREFIX)
								.append(Component.text("Your town port spawn: ", Msg.MUTED))
								.append(coords)
								.build()
						);
					}
				}
			}

			return true;
		}

		// Vehicle restrictions
		if (VehicleUtil.isOnboardVehicle(p)) {
			if (!(p.getVehicle() instanceof Vehicle v) || !VehicleRegistry.isAllowedVehicle(v)) {
				Msg.error(p, "You can't teleport while riding this vehicle.");
				return true;
			}
		}

		// Teleporter configuration
		VehicleTeleporter portsVehicleTeleporter = new VehicleTeleporter(true, true);

		TeleportOutcomeHandler handler = new TeleportOutcomeHandler();
		handler.setOnFailedUnaffordable(context ->
				Msg.error(context.getPlayer(), "You cannot afford to travel to this port.")
		);

		long warmupTicks = Config.PORT_TRAVEL_WARMUP_IN_TICKS.getLong();
		long warmupSeconds = warmupTicks / 20;
		String sign = Config.CURRENCY_SIGN.getString();

		Teleporter portsTeleporter = Teleporter.builder(PortsMain.instance)
				.setVehicleTeleporter(portsVehicleTeleporter)

				.preTeleportMessage(
						Component.text()
								.append(Msg.PREFIX)
								.append(Component.text("You will pay ", NamedTextColor.WHITE))
								.append(Component.text(destinationPort.portPrice() + " " + sign, Msg.GOOD))
								.append(Component.text(" to travel to ", NamedTextColor.WHITE))
								.append(Component.text(destinationPort.getName(), Msg.ACCENT))
								.append(Component.text(" in " + warmupSeconds + "s. ", NamedTextColor.WHITE))
								.append(Component.text("Don't move.", Msg.MUTED))
								.build()
				)
				.postTeleportMessage(
						Component.text()
								.append(Msg.PREFIX)
								.append(Component.text("Arrived at the port.", Msg.GOOD))
								.build()
				)

				.enableWarmup(warmupTicks)
				.enableDestinationSafety()
				.disablePreTeleportMovement()
				.setOutcomeHandler(handler)

				.build();

		// Cooldown
		if (portsCooldownManager.hasCooldown(p)) {
			Msg.warn(p, "Port cooldown: " + portsCooldownManager.getCooldown(p) + " seconds.");
			return true;
		}

		portsTeleporter.teleport(
				p,
				destinationPort.location(),
				destinationPort.portPrice(),
				destinationTown.getAccount(),
				"TownyPorts teleport"
		);

		portsCooldownManager.setCooldown(p, Config.PORT_TRAVEL_COOLDOWN_IN_SECONDS.getLong());
		return true;
	}

	/**
	 * Checks all prerequisite conditions required before a player may port-travel.
	 * Throws TownyException with a player-facing message if any condition is not met.
	 */
	private static void checkTeleportEligibility(Player player, @NotNull Town destinationTown, @NotNull Port destinationPort) throws TownyException {

		TownyAPI townyAPI = TownyAPI.getInstance();

		Resident playerResident = townyAPI.getResident(player);
		if (playerResident == null) {
			throw new TownyException("Failed to load your Towny Resident record. Please relog or contact staff.");
		}

		Town playerTown = playerResident.getTownOrNull();
		if (playerTown == null) {
			throw new TownyException("You do not belong to a town.");
		}

		if (!playerTown.hasNation()) {
			throw new TownyException("You do not belong to a nation.");
		}

		Nation playerNation = playerTown.getNationOrNull();

		// Wilderness restriction
		if (!player.hasPermission("townyports.bypass.wilderness") && townyAPI.isWilderness(player.getLocation())) {
			throw new TownyException("You cannot port-travel from the wilderness.");
		}

		// Enemy restriction
		Nation destinationNation = destinationTown.getNationOrNull();
		if (destinationNation != null &&
				playerNation != null &&
				destinationNation.hasEnemy(playerNation) &&
				Config.PORT_TRAVEL_DENIES_FOR_ENEMIES.getBool()) {
			throw new TownyException("You cannot port-travel to an enemy nation's ports.");
		}

		// Distance restriction
		int portMaxDistance = Config.MAXIMUM_PORT_DISTANCE_IN_CHUNKS.getInt();
		WorldCoord destWc = townyAPI.getTownBlock(destinationPort.location()).getWorldCoord();

		Location loc = player.getLocation();
		int originChunkX = loc.getBlockX() >> 4;
		int originChunkZ = loc.getBlockZ() >> 4;
		WorldCoord originWc = new WorldCoord(loc.getWorld().getName(), originChunkX, originChunkZ);

		if (MathUtil.distance(originWc, destWc) > portMaxDistance && !player.hasPermission("townyports.bypass.distance")) {
			throw new TownyException("That port is too far away to travel to.");
		}

		// Must start in a port chunk
		if (!isPlayerStandingInPortChunk(player)) {

			// Try to provide a helpful pointer: their own town's port spawn (if it exists).
			Port originPort = PortDAO.getPort(playerTown);
			if (originPort != null) {
				var l = originPort.location();
				String coords = l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();
				throw new TownyException("You must be standing in the same chunk as a port spawn to travel. " +
						"Your town's port spawn is at: " + coords);
			}

			throw new TownyException("You must be standing in the same chunk as a port spawn to travel. " +
					"Your town does not have a port set.");
		}
	}

	public static boolean isPlayerStandingInPortChunk(Player p) {
		TownyAPI townyAPI = TownyAPI.getInstance();
		WorldCoord playerWorldCoord = WorldCoord.parseWorldCoord(p.getLocation());

		Town townPlayerIsStandingIn = townyAPI.getTownOrNull(townyAPI.getTownBlock(playerWorldCoord));
		if (townPlayerIsStandingIn == null) return false;

		Port townPort = PortDAO.getPort(townPlayerIsStandingIn);
		if (townPort == null) return false;

		WorldCoord portWorldCoord = WorldCoord.parseWorldCoord(townPort.location());
		return playerWorldCoord.equals(portWorldCoord);
	}
}
